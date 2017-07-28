package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.Main;
import com.castlabs.dash.dashfragmenter.cmdlines.trackoptions.InputOutputSelector;
import com.castlabs.dash.dashfragmenter.cmdlines.trackoptions.InputOutputSelectorOptionHandler;
import com.castlabs.dash.dashfragmenter.encrypt2.ManifestCreation;
import com.castlabs.dash.dashfragmenter.representation.ManifestOptimizer;
import com.castlabs.dash.dashfragmenter.representation.RepresentationBuilderImpl;
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
import org.mp4parser.muxer.builder.DefaultFragmenterImpl;
import org.mp4parser.muxer.tracks.encryption.CencEncryptingTrackImpl;
import org.mp4parser.tools.RangeStartMap;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.castlabs.dash.helpers.BoxHelper.boxToBytes;
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

                PeriodType periodType = ManifestCreation.getPeriod(mpd, periodId);

                if (tracks == InputOutputSelector.TEXTTRACK) {
                    List<File> files = inputSource.getFiles();
                    ManifestCreation.addTextTrack(periodType, files, outputDirectory, outOptions);
                } else if (tracks == InputOutputSelector.THUMBTRACK) {
                    List<File> files = inputSource.getFiles();
                    ManifestCreation.addThumbnailTrack(periodType, files, outputDirectory, outOptions);
                } else {
                    int tracksWritten = 0;

                    if (hasTrack(tracks, "hvc1", "hev1", "avc1", "avc3")) {
                        AdaptationSetType adaptationSetType =
                                Arrays.stream(periodType.getAdaptationSetArray()).filter(
                                        a -> a.getMimeType().equals("video/mp4") &&
                                                Arrays.stream(a.getRoleArray()).allMatch(
                                                        r -> outOptions.containsKey("role") &&
                                                                outOptions.get("role").equals(r.getValue()))).findFirst().orElseGet(() -> {
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
                            String representationId = ManifestCreation.findRepresentationId(track, mpd);
                            long[] fragStartSamples = videoFragmenter.sampleNumbers(track);
                            Track t = encryptIfNeeded(track, fragStartSamples[(int) (clearLead / minVideoSegmentDuration)]);

                            RepresentationType representationType = ManifestCreation.addVideoRepresentation(adaptationSetType, representationId, t);

                            RepresentationBuilderImpl rb = new RepresentationBuilderImpl(
                                    t,
                                    getPsshs().get(videoKeyId),
                                    "",
                                    fragStartSamples,
                                    fragStartSamples);

                            ManifestCreation.addContentProtection(representationType, t, getPsshs());
                            ManifestCreation.updateDuration( periodType, (double) t.getDuration() / t.getTrackMetaData().getTimescale());
                            ManifestCreation.addSegmentBase(rb, representationType);
                            RepresentationBuilderToFile.writeOnDemand(
                                    rb,
                                    representationType,
                                    outputDirectory);
                            tracksWritten++;
                        }
                    }

                    for (Track track : getTrack(tracks, "dtsl", "dtse", "ec-3", "ac-3", "mlpa", "mp4a")) {
                        AdaptationSetType adaptationSetType = periodType.addNewAdaptationSet();
                        String representationId = ManifestCreation.findRepresentationId(track, mpd);
                        long[] fragStartSamples = audioFragmenter.sampleNumbers(track);
                        Track t = encryptIfNeeded(track, fragStartSamples[(int) (clearLead / minAudioSegmentDuration)]);

                        RepresentationBuilderImpl rb = new RepresentationBuilderImpl(
                                t,
                                getPsshs().get(audioKeyId),
                                "audio",
                                fragStartSamples,
                                fragStartSamples);

                        Locale locale;
                        if (outOptions.containsKey("lang")) {
                            locale = Locale.forLanguageTag(outOptions.remove("lang"));
                            t.getTrackMetaData().setLanguage(locale.getISO3Language());
                        } else {
                            locale = Locale.forLanguageTag(t.getTrackMetaData().getLanguage());
                        }
                        adaptationSetType.setLang(locale.toLanguageTag());

                        RepresentationType representationType = ManifestCreation.addAudioRepresentation(adaptationSetType, t, representationId, outOptions);

                        ManifestCreation.addContentProtection(representationType, t, getPsshs());
                        ManifestCreation.updateDuration(periodType, (double) t.getDuration() / t.getTrackMetaData().getTimescale());
                        ManifestCreation.addSegmentBase(rb, representationType);
                        RepresentationBuilderToFile.writeOnDemand(
                                rb,
                                representationType,
                                outputDirectory);
                        tracksWritten++;
                    }


                    if (tracksWritten < tracks.size()) {
                        throw new RuntimeException("Not all input tracks have been used in the Manifest");
                    }

                }
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

    public Map<UUID, List<ProtectionSystemSpecificHeaderBox>> getPsshs() {
        return Collections.emptyMap();
    }

}
