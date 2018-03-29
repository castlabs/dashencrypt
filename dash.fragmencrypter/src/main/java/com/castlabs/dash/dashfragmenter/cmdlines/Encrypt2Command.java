package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.cmdlines.trackoptions.InputOutputSelector;
import com.castlabs.dash.dashfragmenter.cmdlines.trackoptions.InputOutputSelectorOptionHandler;
import com.castlabs.dash.dashfragmenter.encrypt2.ManifestCreation;
import com.castlabs.dash.dashfragmenter.representation.ManifestOptimizer;
import com.castlabs.dash.dashfragmenter.representation.RepresentationBuilderImpl;
import com.castlabs.dash.helpers.DashHelper2;
import com.castlabs.dash.helpers.RepresentationBuilderToFile;
import mpeg.dash.schema.mpd._2011.*;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.mp4parser.boxes.iso23001.part7.ProtectionSystemSpecificHeaderBox;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.DefaultFragmenterImpl;
import org.mp4parser.muxer.tracks.encryption.CencEncryptingTrackImpl;
import org.mp4parser.tools.RangeStartMap;

import javax.crypto.SecretKey;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


import static org.mp4parser.tools.CastUtils.l2i;

public class Encrypt2Command extends AbstractEncryptOrNotCommand {
    static DatatypeFactory datatypeFactory = null;

    static {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }



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


            MPDtype mpd = new MPDtype();

/*           @todo
  mpd.newCursor().insertComment(Main.TOOL);
            mpd.newCursor().insertComment(Version.VERSION);*/

            ProgramInformationType programInformationType = new ProgramInformationType();
            mpd.getProgramInformation().add(programInformationType);
            programInformationType.setMoreInformationURL("www.castLabs.com");

            mpd.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");
            mpd.setType(PresentationType.STATIC);

            mpd.setMinBufferTime(datatypeFactory.newDuration(true, 0, 0, 0, 0, 0, 4));

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
                        AdaptationSetType adaptationSetType = periodType.getAdaptationSet().stream().filter(
                                        a -> a.getMimeType().equals("video/mp4") &&
                                                a.getRole().stream().allMatch(
                                                        r -> outOptions.containsKey("role") &&
                                                                outOptions.get("role").equals(r.getValue()))).findFirst().orElseGet(() -> {
                                    AdaptationSetType as = new AdaptationSetType();
                                    periodType.getAdaptationSet().add(as);
                                    as.setMimeType("video/mp4");
                                    if (outOptions.containsKey("role")) {
                                        DescriptorType role = new DescriptorType();
                                        as.getRole().add(role);
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
                            ManifestCreation.updateDuration(periodType, (double) t.getDuration() / t.getTrackMetaData().getTimescale());
                            ManifestCreation.addSegmentBase(rb, representationType);
                            totalSize += RepresentationBuilderToFile.writeOnDemand(
                                    rb,
                                    representationType,
                                    outputDirectory);
                            tracksWritten++;
                        }
                    }

                    for (Track track : getTrack(tracks, "dtsl", "dtse", "ec-3", "ac-3", "mlpa", "mp4a")) {
                        AdaptationSetType adaptationSetType = new AdaptationSetType();
                        periodType.getAdaptationSet().add(adaptationSetType);
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
                        totalSize += RepresentationBuilderToFile.writeOnDemand(
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

            ManifestOptimizer.optimize(mpd);
            File manifest1 = new File(outputDirectory, "Manifest.mpd");

            FileWriter writer = new FileWriter(manifest1);

            JAXBContext context = JAXBContext.newInstance(MPDtype.class);

            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            LOG.info("Writing " + manifest1.getAbsolutePath());
            JAXBElement<MPDtype> jaxbMPD = new ObjectFactory().createMPD(mpd);
            m.marshal(jaxbMPD, writer);
           // System.out.println(writer.toString());


            LOG.info(String.format("Finished fragmenting of %dMB in %.1fs", totalSize / 1024 / 1024, (double) (System.currentTimeMillis() - startTime) / 1000));

        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return 365;
        } catch (JAXBException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return 366;
        }

        return 0;
    }

    public Map<UUID, List<ProtectionSystemSpecificHeaderBox>> getPsshs() {
        return Collections.emptyMap();
    }

}
