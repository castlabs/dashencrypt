package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.Main;
import com.castlabs.dash.dashfragmenter.cmdlines.trackoptions.InputOutputSelector;
import com.castlabs.dash.dashfragmenter.cmdlines.trackoptions.InputOutputSelectorOptionHandler;
import com.castlabs.dash.dashfragmenter.representation.ManifestOptimizer;
import com.castlabs.dash.dashfragmenter.representation.RepresentationBuilderImpl;
import com.castlabs.dash.helpers.DashHelper2;
import com.castlabs.dash.helpers.RepresentationBuilderToFile;
import ietfParamsXmlNsKeyprovPskc.impl.StringDataTypeImpl;
import mpegCenc2013.DefaultKIDAttribute;
import mpegDashSchemaMpd2011.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.GDuration;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.mp4parser.Box;
import org.mp4parser.Container;
import org.mp4parser.Version;
import org.mp4parser.boxes.iso23001.part7.ProtectionSystemSpecificHeaderBox;
import org.mp4parser.boxes.iso23001.part7.TrackEncryptionBox;
import org.mp4parser.boxes.sampleentry.AudioSampleEntry;
import org.mp4parser.boxes.sampleentry.SampleEntry;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.DefaultFragmenterImpl;
import org.mp4parser.muxer.tracks.encryption.CencEncryptingTrackImpl;
import org.mp4parser.tools.Path;
import org.mp4parser.tools.RangeStartMap;
import org.mp4parser.tools.UUIDConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.castlabs.dash.helpers.BoxHelper.boxToBytes;
import static com.castlabs.dash.helpers.ManifestHelper.convertFramerate;
import static com.castlabs.dash.helpers.ManifestHelper.getXmlOptions;
import static org.mp4parser.tools.CastUtils.l2i;

public class Encrypt2Command extends AbstractEncryptOrNotCommand {
    private static final Logger LOG = Logger.getLogger(Encrypt2Command.class.getName());

    @Argument(required = true, multiValued = true, handler = InputOutputSelectorOptionHandler.class, usage = "MP4 input files", metaVar = "vid1.mp4, aud1.mp4 ...")
    private List<InputOutputSelector> inputs;

    @Option(name = "--outputdir", aliases = "-o",
            usage = "output directory - if no output directory is given the " +
                    "current working directory is used.",
            metaVar = "PATH")
    private File outputDirectory = new File(System.getProperty("user.dir"));


    @Option(name = "-ma", usage = "Minimum Audio Segment Duration.")
    private double minAudioSegmentDuration = 15;

    @Option(name = "-mv", usage = "Minimum Video Segment Duration.")
    private double minVideoSegmentDuration = 4;

    @Option(name = "-clearlead", usage = "Sets the number of clear seconds in the representation in the beginning. Set to at least the minimum segment duration as only full segment can be clear")
    private int clearLead = 0;

    private boolean hasTrack(List<Track> tracks, String... codecs) {
        for (Track track : tracks) {
            String codec = DashHelper2.getFormat(track);
            if (codec == null) {
                return false;
            } else {
                for (String oneOf : codecs) {
                    if (codec.equals(oneOf)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<Track> getTrack(List<Track> tracks, String... codecs) {
        List<Track> theTracks = new ArrayList<>();

        for (Track track : tracks) {
            String codec = DashHelper2.getFormat(track);
            if (codec != null) {
                for (String oneOf : codecs) {
                    if (codec.equals(oneOf)) {
                        theTracks.add(track);
                    }
                }
            }
        }
        return theTracks;
    }

    private Track encryptIfNeeded(Track in, long encStartSample) {
        if (in.getHandler().equals("soun")) {
            if (audioKeyId != null) {
                RangeStartMap<Integer, UUID> indexToKeyId = new RangeStartMap<>(0, null);
                indexToKeyId.put(l2i(encStartSample - 1), audioKeyId);
                Map<UUID, SecretKey> keys = Collections.singletonMap(audioKeyId, audioKey);
                return new CencEncryptingTrackImpl(in, indexToKeyId, keys, "cenc", false, false);
            } else {
                return in;
            }
        } else if (in.getHandler().equals("vide")) {
            if (videoKeyId != null) {
                RangeStartMap<Integer, UUID> indexToKeyId = new RangeStartMap<>(0, null);
                indexToKeyId.put(l2i(encStartSample - 1), videoKeyId);
                Map<UUID, SecretKey> keys = Collections.singletonMap(videoKeyId, videoKey);
                return new CencEncryptingTrackImpl(in, indexToKeyId, keys, "cenc", false, false);
            } else {
                return in;
            }

        } else {
            return in;
        }

    }

    public String getName(Set<String> representationNames, String baseName) {
        if (representationNames.contains(baseName)) {
            int i = 1;
            while (representationNames.contains(baseName + "_" + i)) {
                i++;
            }
            representationNames.add(baseName + "_" + i);
            return baseName + "_" + i;
        } else {
            representationNames.add(baseName);
            return baseName;
        }
    }

    public int run() {
        try {
            if (outputDirectory.getAbsoluteFile().exists() == outputDirectory.getAbsoluteFile().mkdirs()) {
                LOG.severe("Output directory does not exist and cannot be created.");
                return 982;
            }

            long totalSize = 0;
            long startTime = System.currentTimeMillis();
            DefaultFragmenterImpl videoFragmenter = new DefaultFragmenterImpl(minVideoSegmentDuration);
            DefaultFragmenterImpl audioFragmenter = new DefaultFragmenterImpl(minAudioSegmentDuration);

            MPDDocument mdd = MPDDocument.Factory.newInstance();
            MPDtype mpd = mdd.addNewMPD();
            mpd.newCursor().insertComment(Main.TOOL);
            mpd.newCursor().insertComment(Version.VERSION);

            ProgramInformationType programInformationType = mpd.addNewProgramInformation();
            programInformationType.setMoreInformationURL("www.castLabs.com");

            mpd.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");
            mpd.setType(PresentationType.STATIC);

            mpd.setMinBufferTime(new GDuration(1, 0, 0, 0, 0, 0, 4, BigDecimal.ZERO));

            for (InputOutputSelector inputSource : inputs) {
                List<Track> tracks = inputSource.getSelectedTracks();

                Map<String, String> outOptions = new HashMap<>(inputSource.getOutputOptions());
                String periodId = "period-" + (outOptions.containsKey("period") ? outOptions.remove("period") : "0");

                PeriodType periodType = Arrays
                        .stream(mpd.getPeriodArray())
                        .filter(x -> x.getId().equals(periodId))
                        .findFirst()
                        .orElseGet(() -> {
                            PeriodType p = mpd.addNewPeriod();
                            p.setId(periodId);
                            if (mpd.getPeriodArray().length == 1) {
                                p.setStart(new GDuration(1, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO));
                            }
                            return p;
                        });

                if (hasTrack(tracks, "hvc1", "hev1", "avc1", "avc3")) {
                    AdaptationSetType adaptationSetType =
                            Arrays.stream(periodType.getAdaptationSetArray()).filter(a -> a.getMimeType().equals("video/mp4")).findFirst().orElseGet(() -> {
                                AdaptationSetType as = periodType.addNewAdaptationSet();
                                as.setMimeType("video/mp4");
                                if (outOptions.containsKey("role")) {
                                    DescriptorType role = as.addNewRole();
                                    role.setSchemeIdUri("urn:mpeg:dash:role");
                                    role.setValue(outOptions.get("role"));
                                }
                                return as;
                            });


                    for (Track track : getTrack(tracks, "hvc1", "hev1", "avc1", "avc3")) {

                        long[] fragStartSamples = videoFragmenter.sampleNumbers(track);
                        Track t = encryptIfNeeded(track, fragStartSamples[(int) (clearLead / minVideoSegmentDuration)]);
                        RepresentationBuilderImpl rb = new RepresentationBuilderImpl(
                                t,
                                getPsshs().get(videoKeyId),
                                "",
                                fragStartSamples,
                                fragStartSamples);
                        RepresentationType representationType = adaptationSetType.addNewRepresentation();
                        representationType.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");
                        addContentProtection(representationType, t);
                        long videoHeight = (long) track.getTrackMetaData().getHeight();
                        long videoWidth = (long) track.getTrackMetaData().getWidth();
                        double framesPerSecond = (double) (track.getSamples().size() * track.getTrackMetaData().getTimescale()) / track.getDuration();

                        LinkedHashSet<String> codecs = new LinkedHashSet<>();
                        for (SampleEntry sampleEntry : track.getSampleEntries()) {
                            codecs.add(DashHelper2.getRfc6381Codec(sampleEntry));
                        }
                        representationType.setCodecs(StringUtils.join(codecs.toArray(), ","));
                        representationType.setWidth(videoWidth);
                        representationType.setHeight(videoHeight);
                        representationType.setFrameRate(convertFramerate(framesPerSecond));
                        representationType.setSar("1:1");
                        representationType.setBandwidth(rb.getBandwidth());
                        representationType.setId(findRepresentationId(track, mpd));
                        BaseURLType baseURLType = representationType.addNewBaseURL();
                        baseURLType.set(representationType.getId() + ".mp4");
                        RepresentationBuilderToFile.writeOnDemand(
                                rb,
                                representationType,
                                outputDirectory);

                        updateDuration(periodType, (double) track.getDuration() / track.getTrackMetaData().getTimescale());
                        addSegmentBase(rb, representationType);
                    }
                }

                for (Track track : getTrack(tracks, "dtsl", "dtse", "ec-3", "ac-3", "mlpa", "mp4a")) {
                    long[] fragStartSamples = audioFragmenter.sampleNumbers(track);
                    AdaptationSetType as = periodType.addNewAdaptationSet();
                    Locale locale;
                    if (outOptions.containsKey("lang")) {
                        locale = Locale.forLanguageTag(outOptions.remove("lang"));
                        track.getTrackMetaData().setLanguage(locale.getISO3Language());
                    } else {
                        locale = Locale.forLanguageTag(track.getTrackMetaData().getLanguage());
                    }
                    as.setLang(locale.toLanguageTag());


                    Track t = encryptIfNeeded(track, fragStartSamples[(int) (clearLead / minAudioSegmentDuration)]);
                    RepresentationBuilderImpl rb = new RepresentationBuilderImpl(
                            t,
                            getPsshs().get(audioKeyId),
                            "audio",
                            fragStartSamples,
                            fragStartSamples);
                    RepresentationType representationType = as.addNewRepresentation();
                    representationType.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");
                    as.setMimeType("audio/mp4");
                    if (outOptions.containsKey("role")) {
                        DescriptorType role = as.addNewRole();
                        role.setSchemeIdUri("urn:mpeg:dash:role");
                        role.setValue(outOptions.get("role"));
                    }
                    addContentProtection(representationType, t);
                    LinkedHashSet<String> codecs = new LinkedHashSet<>();
                    AudioSampleEntry ase = null;
                    for (SampleEntry sampleEntry : track.getSampleEntries()) {
                        if (sampleEntry instanceof AudioSampleEntry) {
                            ase = (AudioSampleEntry) sampleEntry;
                        }
                        codecs.add(DashHelper2.getRfc6381Codec(sampleEntry));
                    }
                    assert ase != null;
                    representationType.setCodecs(String.join(",", codecs));
                    representationType.setAudioSamplingRate("" + DashHelper2.getAudioSamplingRate(ase));
                    representationType.setBandwidth(rb.getBandwidth());
                    representationType.setId(findRepresentationId(track, mpd));
                    BaseURLType baseURLType = representationType.addNewBaseURL();
                    baseURLType.set(representationType.getId() + ".mp4");
                    RepresentationBuilderToFile.writeOnDemand(
                            rb,
                            representationType,
                            outputDirectory);

                    DashHelper2.ChannelConfiguration cc = DashHelper2.getChannelConfiguration(ase);
                    if (cc != null) {
                        DescriptorType audio_channel_conf = representationType.addNewAudioChannelConfiguration();
                        audio_channel_conf.setSchemeIdUri(cc.schemeIdUri);
                        audio_channel_conf.setValue(cc.value);
                    }

                    updateDuration(periodType, (double) track.getDuration() / track.getTrackMetaData().getTimescale());
                    addSegmentBase(rb, representationType);
                }

                if (tracks == InputOutputSelector.TEXTTRACK) {

                    AdaptationSetType adaptationSet = periodType.addNewAdaptationSet();
                    adaptationSet.setLang(outOptions.get("lang"));
                    RepresentationType representation = adaptationSet.addNewRepresentation();
                    DescriptorType textTrackRole = adaptationSet.addNewRole();
                    textTrackRole.setSchemeIdUri("urn:mpeg:dash:role");
                    textTrackRole.setValue(outOptions.getOrDefault("role", "subtitle"));
                    representation.setId(outOptions.getOrDefault("role", "subtitle") + "-" + outOptions.get("lang"));
                    representation.setBandwidth(0); // pointless - just invent a small number
                    if (inputSource.getFiles().get(0).getName().endsWith(".dfxp")) {
                        representation.setMimeType("application/ttaf+xml");
                    } else if (inputSource.getFiles().get(0).getName().endsWith(".xml") || inputSource.getFiles().get(0).getName().endsWith(".ttml")) {
                        representation.setMimeType("application/ttml+xml");
                    } else if (inputSource.getFiles().get(0).getName().endsWith(".vtt")) {
                        representation.setMimeType("text/vtt");

                    } else {
                        throw new RuntimeException("Not sure what kind of textTrack " + inputSource.getFiles().get(0).getName() + " is.");
                    }
                    BaseURLType baseURL = representation.addNewBaseURL();
                    baseURL.setStringValue(inputSource.getFiles().get(0).getName());

                }

           /*     if (tracks == InputOutputSelector.THUMBTRACK) {
                   representationBuilders.put(new RawTextTrackRepresentationBuilder(inputSource.getName(), inputSource.getFiles()), inputSource);
                }*/


            }

            ManifestOptimizer.optimize(mdd);
            File manifest1 = new File(outputDirectory, "Manifest.mpd");
            XmlOptions xmlOptions = getXmlOptions();
            Map<String, String> ns = (Map<String, String>) xmlOptions.get(XmlOptions.SAVE_SUGGESTED_PREFIXES);
            ns.put("urn:microsoft:playready", "mspro");
            LOG.info("Writing " + manifest1.getAbsolutePath());
            mdd.save(manifest1, xmlOptions);

            LOG.info(String.format("Finished fragmenting of %dMB in %.1fs", totalSize / 1024 / 1024, (double) (System.currentTimeMillis() - startTime) / 1000));

        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return 365;
        }

        return 0;
    }

    private void addSegmentBase(RepresentationBuilderImpl rb, RepresentationType representationType) {
        SegmentBaseType segBaseType = representationType.addNewSegmentBase();
        segBaseType.setTimescale(((org.mp4parser.boxes.iso14496.part12.MovieHeaderBox) Path.getPath(rb.getInitSegment(), "moov[0]/mvhd[0]")).getTimescale());
        segBaseType.setIndexRangeExact(true);
        long initSize = 0;
        for (Box b : rb.getInitSegment().getBoxes()) {
            initSize += b.getSize();
        }
        long indexSize = 0;
        for (Box b : rb.getIndexSegment().getBoxes()) {
            indexSize += b.getSize();
        }
        segBaseType.setIndexRange(String.format("%s-%s", initSize, initSize + indexSize - 1));
        URLType initialization = segBaseType.addNewInitialization();
        initialization.setRange(String.format("0-%s", initSize - 1));
    }

    private String findRepresentationId(Track track, MPDtype mpd) {
        HashSet<String> ids = new HashSet<>();

        for (PeriodType periodType : mpd.getPeriodArray()) {
            for (AdaptationSetType adaptationSetType : periodType.getAdaptationSetArray()) {
                for (RepresentationType representationType : adaptationSetType.getRepresentationArray()) {
                    ids.add(representationType.getId());
                }
            }
        }
        if (ids.contains(FilenameUtils.getBaseName(track.getName()))) {
            return FilenameUtils.getBaseName(track.getName()) + "-" + track.getTrackMetaData().getTrackId();
        } else {
            return FilenameUtils.getBaseName(track.getName());
        }

    }

    private void updateDuration(PeriodType periodType, double duration) {
        GDuration gd = new GDuration(
                1, 0, 0, 0, (int) (duration / 3600),
                (int) ((duration % 3600) / 60),
                (int) (duration % 60), BigDecimal.ZERO);
        GDuration gdOld = periodType.getDuration();
        if (gdOld == null || gdOld.compareToGDuration(gd) >  0) {
            periodType.setDuration(gd);
        }
    }

    public Map<UUID, List<ProtectionSystemSpecificHeaderBox>> getPsshs() {
        return Collections.emptyMap();
    }

    private void addContentProtection(RepresentationType representation, Track theTrack) {
        List<String> keyIds = new ArrayList<>();
        List<ProtectionSystemSpecificHeaderBox> psshs = new ArrayList<>();
        for (SampleEntry sampleEntry : theTrack.getSampleEntries()) {
            TrackEncryptionBox tenc = Path.getPath((Container) sampleEntry, "sinf[0]/schi[0]/tenc[0]");
            if (tenc != null) {
                keyIds.add(tenc.getDefault_KID().toString());
                psshs.addAll(getPsshs().getOrDefault(tenc.getDefault_KID(), Collections.emptyList()));

            }
        }

        if (!keyIds.isEmpty()) {
            DescriptorType contentProtection = representation.addNewContentProtection();
            final DefaultKIDAttribute defaultKIDAttribute = DefaultKIDAttribute.Factory.newInstance();

            defaultKIDAttribute.setDefaultKID(keyIds);
            contentProtection.set(defaultKIDAttribute);
            contentProtection.setSchemeIdUri("urn:mpeg:dash:mp4protection:2011");
            contentProtection.setValue("cenc");
        }


        for (ProtectionSystemSpecificHeaderBox pssh : psshs) {
            DescriptorType dt = representation.addNewContentProtection();
            byte[] psshContent = pssh.getContent();
            dt.setSchemeIdUri("urn:uuid:" + UUIDConverter.convert(pssh.getSystemId()).toString());
            if (Arrays.equals(ProtectionSystemSpecificHeaderBox.PLAYREADY_SYSTEM_ID, pssh.getSystemId())) {
                dt.setValue("MSPR 2.0");
                Node playReadyCPN = dt.getDomNode();
                Document d = playReadyCPN.getOwnerDocument();
                Element pro = d.createElementNS("urn:microsoft:playready", "pro");
                Element prPssh = d.createElementNS("urn:mpeg:cenc:2013", "pssh");

                pro.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(psshContent)));
                prPssh.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(boxToBytes(pssh))));

                playReadyCPN.appendChild(pro);
                playReadyCPN.appendChild(prPssh);
            }
            if (Arrays.equals(ProtectionSystemSpecificHeaderBox.WIDEVINE, pssh.getSystemId())) {
                // Widevvine
                Node widevineCPN = dt.getDomNode();
                Document d = widevineCPN.getOwnerDocument();
                Element wvPssh = d.createElementNS("urn:mpeg:cenc:2013", "pssh");
                wvPssh.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(boxToBytes(pssh))));

                widevineCPN.appendChild(wvPssh);
            }
        }

    }
}
