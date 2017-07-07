package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.Main;
import com.castlabs.dash.dashfragmenter.cmdlines.trackoptions.InputOutputSelector;
import com.castlabs.dash.dashfragmenter.cmdlines.trackoptions.InputOutputSelectorOptionHandler;
import com.castlabs.dash.dashfragmenter.representation.*;
import com.castlabs.dash.helpers.DashHelper;
import com.castlabs.dash.helpers.DashHelper2;
import com.castlabs.dash.helpers.RepresentationBuilderToFile;
import mpegDashSchemaMpd2011.*;
import org.apache.xmlbeans.GDuration;
import org.apache.xmlbeans.XmlOptions;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.mp4parser.Version;
import org.mp4parser.boxes.iso23001.part7.ProtectionSystemSpecificHeaderBox;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.tracks.encryption.CencEncryptingTrackImpl;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.castlabs.dash.helpers.ManifestHelper.getXmlOptions;

public class Encrypt2Command extends AbstractEncryptOrNotCommand {
    private static final Logger LOG = Logger.getLogger(Encrypt2Command.class.getName());

    @Argument(required = true, multiValued = true, handler = InputOutputSelectorOptionHandler.class, usage = "MP4 input files", metaVar = "vid1.mp4, aud1.mp4 ...")
    protected List<InputOutputSelector> inputs;

    @Option(name = "--outputdir", aliases = "-o",
            usage = "output directory - if no output directory is given the " +
                    "current working directory is used.",
            metaVar = "PATH")
    protected File outputDirectory = new File(System.getProperty("user.dir"));


    @Option(name = "-ma", usage = "Minimum Audio Segment Duration.")
    protected double minAudioSegmentDuration = 15;

    @Option(name = "-mv", usage = "Minimum Video Segment Duration.")
    protected double minVideoSegmentDuration = 4;


    DocumentBuilder documentBuilder;


    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        //dbf.setNamespaceAware(true);
        try {
            documentBuilder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

    }

    MPDDocument getManifest(Map<Integer,List<AdaptationSetType>> periods2AdaptationSets, Map<Integer, Duration> durations) throws IOException {

        List<Integer> ids = new ArrayList<>(periods2AdaptationSets.keySet());
        Collections.sort(ids);
        MPDDocument mdd = MPDDocument.Factory.newInstance();
        MPDtype mpd = mdd.addNewMPD();
        ProgramInformationType programInformationType = mpd.addNewProgramInformation();
        programInformationType.setMoreInformationURL("www.castLabs.com");
        Duration total = Duration.ofSeconds(0);
        for (Integer periodId : ids) {
            PeriodType periodType = mpd.addNewPeriod();
            periodType.setId("period-" + periodId.toString());
            periodType.setStart(new GDuration(1, 0, 0, 0, (int)total.getSeconds()/3600, (int)(total.getSeconds()%3600) / 60, (int)total.getSeconds() % 60, BigDecimal.ZERO));

            List<AdaptationSetType> adaptationSets = periods2AdaptationSets.get(periodId);
            periodType.setAdaptationSetArray(adaptationSets.toArray(new AdaptationSetType[adaptationSets.size()]));
            Duration d =  durations.get(periodId);
            total = total.plus(d);
            periodType.setDuration(new GDuration(1, 0, 0, 0, (int)d.getSeconds()/3600, (int)(d.getSeconds()%3600) / 60, (int)d.getSeconds() % 60, BigDecimal.ZERO));
        }
        mpd.setMediaPresentationDuration(new GDuration(1, 0, 0, 0, (int)total.getSeconds()/3600, (int)(total.getSeconds()%3600) / 60, (int)total.getSeconds() % 60, BigDecimal.ZERO));

        mpd.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");
        mpd.setType(PresentationType.STATIC);

        mpd.setMinBufferTime(new GDuration(1, 0, 0, 0, 0, 0, 4, BigDecimal.ZERO));



        return mdd;
    }


    boolean hasTrack(List<Track> tracks, String... codecs) {
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

    List<Track> getTrack(List<Track> tracks, String... codecs) {
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

    public Track encryptIfNeeded(Track in) {
        if (in.getHandler().equals("soun")) {
            if (audioKeyId != null) {
                return new CencEncryptingTrackImpl(in, audioKeyId, audioKey, false);
            } else {
                return in;
            }
        } else if (in.getHandler().equals("vide")) {
            if (videoKeyId != null) {
                return new CencEncryptingTrackImpl(in, videoKeyId, videoKey, false);
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


            Map<UUID, List<ProtectionSystemSpecificHeaderBox>> psshs = getPsshs();
            Map<RepresentationBuilder, InputOutputSelector> representationBuilders = new LinkedHashMap<>();
            Set<String> representationNames = new HashSet<>();

            long totalSize = 0;
            long startTime = System.currentTimeMillis();


            for (InputOutputSelector inputSource : inputs) {
                List<Track> tracks = inputSource.getSelectedTracks();
                int numRepresentationsBefore = representationBuilders.size();

                if (hasTrack(tracks, "hvc1", "hev1", "avc1", "avc3")) {

                    for (Track track : getTrack(tracks, "hvc1", "hev1", "avc1", "avc3")) {
                        String name = getName(representationNames, inputSource.getName());
                        representationBuilders.put(new SyncSampleAssistedRepresentationBuilder(
                                encryptIfNeeded(track),
                                name,
                                minVideoSegmentDuration,
                                psshs.get(videoKeyId)
                        ), inputSource);
                        LOG.fine("Added " + name);
                    }
                }

                for (Track track : getTrack(tracks, "dtsl", "dtse", "ec-3", "ac-3", "mlpa", "mp4a")) {
                    String name = getName(representationNames, inputSource.getName());
                    representationBuilders.put(new SyncSampleAssistedRepresentationBuilder(
                            encryptIfNeeded(track),
                            name,
                            minAudioSegmentDuration,
                            psshs.get(audioKeyId)), inputSource);
                    LOG.fine("Added " + name);
                }

                if (tracks == InputOutputSelector.TEXTTRACK) {
                    representationBuilders.put(new RawTextTrackRepresentationBuilder(inputSource.getName(), inputSource.getFile()), inputSource);
                }


                if (numRepresentationsBefore == representationBuilders.size()) {
                    LOG.severe("No Representation has been created from " + inputSource);
                    return 9873;
                }


            }


            Map<Integer, Map<String, AdaptationSetType>> period2AdaptationSets = new LinkedHashMap<>();
            Map<Integer, Duration> period2Duration = new LinkedHashMap<>();

            // Assigns all representations to a Period and then to an AdaptationSet within the Period
            for (Map.Entry<RepresentationBuilder, InputOutputSelector> e : representationBuilders.entrySet()) {
                RepresentationBuilder rb = e.getKey();
                InputOutputSelector inputOutputSelector = e.getValue();
                Map<String, String> outOptions = new HashMap<>(inputOutputSelector.getOutputOptions());
                Integer periodId = outOptions.containsKey("period") ? Integer.parseInt(outOptions.remove("period")) : 0;
                if (rb instanceof Mp4RepresentationBuilder) {
                    Mp4RepresentationBuilder representationBuilder = (Mp4RepresentationBuilder) rb;

                    Locale locale;
                    if (outOptions.containsKey("lang")) {
                        locale = Locale.forLanguageTag(outOptions.remove("lang"));
                        representationBuilder.getTrack().getTrackMetaData().setLanguage(locale.getISO3Language());
                    } else {
                        locale = Locale.forLanguageTag(representationBuilder.getTrack().getTrackMetaData().getLanguage());
                    }

                    String id = DashHelper.filename2UrlPath(representationBuilder.getSource());

                    LOG.info("Representation generated from " + inputOutputSelector.getName() + " was assigned to Period " + periodId);

                    RepresentationType representation = representationBuilder.getOnDemandRepresentation();


                    representation.addNewBaseURL().setStringValue(id + ".mp4");
                    representation.setId(id);
                    RepresentationBuilderToFile.writeOnDemand(
                            representationBuilder,
                            representation, outputDirectory);

                    String type = DashHelper2.getFormat(representationBuilder.getTrack());
                    type += representationBuilder.getTrack().getTrackMetaData().getLanguage();

                    Map<String, AdaptationSetType> adaptationSets = period2AdaptationSets.computeIfAbsent(periodId, k -> new LinkedHashMap<>());

                    AdaptationSetType adaptationSet = adaptationSets.computeIfAbsent(type, k -> AdaptationSetType.Factory.newInstance());

                    if (outOptions.containsKey("role")) {
                        DescriptorType role = adaptationSet.addNewRole();
                        role.setSchemeIdUri("urn:mpeg:dash:role");
                        role.setValue(outOptions.get("role"));
                    }

                    adaptationSet.setLang(locale.toLanguageTag());

                    Duration duration = period2Duration.get(periodId);
                    if (duration == null || duration.compareTo(getDuration(representationBuilder)) > 0) {
                        duration = getDuration(representationBuilder);
                    }
                    period2Duration.put(periodId, duration);


                    RepresentationType[] representationsInThisSet = adaptationSet.getRepresentationArray();
                    representationsInThisSet = Arrays.copyOf(representationsInThisSet, representationsInThisSet.length + 1);
                    representationsInThisSet[representationsInThisSet.length - 1] = representation;
                    adaptationSet.setRepresentationArray(representationsInThisSet);

                } else if (rb instanceof RawTextTrackRepresentationBuilder) {
                    RawTextTrackRepresentationBuilder representationBuilder = (RawTextTrackRepresentationBuilder) rb;
                    Map<String, AdaptationSetType> adaptationSets = period2AdaptationSets.get(periodId);
                    if (adaptationSets == null) {
                        adaptationSets = new LinkedHashMap<>();
                        period2AdaptationSets.put(periodId, adaptationSets);
                    }
                    AdaptationSetType adaptationSet = AdaptationSetType.Factory.newInstance();
                    adaptationSet.setLang(outOptions.get("lang"));
                    DescriptorType textTrackRole = adaptationSet.addNewRole();
                    textTrackRole.setSchemeIdUri("urn:mpeg:dash:role");
                    if (outOptions.containsKey("role")) {
                        textTrackRole.setValue(outOptions.get("role"));
                    } else {
                        textTrackRole.setValue("subtitle");
                    }
                    RepresentationType representationType = representationBuilder.getOnDemandRepresentation();

                    adaptationSet.setRepresentationArray(new RepresentationType[]{representationType});
                    adaptationSets.put(rb.getSource(), adaptationSet);
                    RepresentationBuilderToFile.writeOnDemand(
                            representationBuilder,
                            representationType, outputDirectory);
                }
            }


            Map<Integer, List<AdaptationSetType>> adaptationSets = new HashMap<>();
            for (Map.Entry<Integer, Map<String, AdaptationSetType>> stringMapEntry : period2AdaptationSets.entrySet()) {
                adaptationSets.put(stringMapEntry.getKey(), new ArrayList<>(stringMapEntry.getValue().values()));
            }

            MPDDocument mpd = getManifest(adaptationSets, period2Duration);
            mpd.getMPD().newCursor().insertComment(Main.TOOL);
            mpd.getMPD().newCursor().insertComment(Version.VERSION);

            ManifestOptimizer.optimize(mpd);
            File manifest1 = new File(outputDirectory, "Manifest.mpd");
            XmlOptions xmlOptions = getXmlOptions();
            Map<String, String> ns = (Map<String, String>) xmlOptions.get(XmlOptions.SAVE_SUGGESTED_PREFIXES);
            ns.put("urn:microsoft:playready", "mspro");
            LOG.info("Writing " + manifest1.getAbsolutePath());
            mpd.save(manifest1, xmlOptions);

            LOG.info(String.format("Finished fragmenting of %dMB in %.1fs", totalSize / 1024 / 1024, (double) (System.currentTimeMillis() - startTime) / 1000));

        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return 365;
        }

        return 0;
    }

    private Duration getDuration(Mp4RepresentationBuilder representationBuilder) {
        double duration = representationBuilder.getTrack().getDuration() / (double)representationBuilder.getTrack().getTrackMetaData().getTimescale();
        long ms = (long) (duration * 1000);
        return Duration.ofMillis(ms);

    }


    public Map<UUID,List<ProtectionSystemSpecificHeaderBox>> getPsshs() {
        return Collections.emptyMap();
    }
}
