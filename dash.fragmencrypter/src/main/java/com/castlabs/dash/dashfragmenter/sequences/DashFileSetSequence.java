package com.castlabs.dash.dashfragmenter.sequences;

import com.castlabs.dash.dashfragmenter.BetterTrackGroupFragmenter;
import com.castlabs.dash.dashfragmenter.FileAndTrackSelector;
import com.castlabs.dash.dashfragmenter.formats.csf.DashBuilder;
import com.castlabs.dash.dashfragmenter.formats.csf.SegmentBaseSingleSidxManifestWriterImpl;
import com.castlabs.dash.dashfragmenter.formats.multiplefilessegmenttemplate.ExplodedSegmentListManifestWriterImpl;
import com.castlabs.dash.dashfragmenter.formats.multiplefilessegmenttemplate.SingleSidxExplode;

import com.castlabs.dash.dashfragmenter.representation.ManifestOptimizer;
import com.castlabs.dash.dashfragmenter.representation.Mp4RepresentationBuilder;
import com.castlabs.dash.dashfragmenter.representation.RepresentationBuilderImpl;
import com.castlabs.dash.dashfragmenter.tracks.NegativeCtsInsteadOfEdit;
import com.castlabs.dash.helpers.BoxHelper;
import com.castlabs.dash.helpers.DashHelper;
import com.castlabs.dash.helpers.DashHelper2;
import com.castlabs.dash.helpers.RepresentationBuilderToFile;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.*;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.googlecode.mp4parser.FileDataSourceViaHeapImpl;
import com.googlecode.mp4parser.authoring.*;
import com.googlecode.mp4parser.authoring.builder.BetterFragmenter;
import com.googlecode.mp4parser.authoring.builder.DefaultFragmenterImpl;
import com.googlecode.mp4parser.authoring.builder.Fragmenter;
import com.googlecode.mp4parser.authoring.builder.StaticFragmentIntersectionFinderImpl;
import com.googlecode.mp4parser.authoring.tracks.*;
import com.googlecode.mp4parser.authoring.tracks.h264.H264TrackImpl;
import com.googlecode.mp4parser.authoring.tracks.ttml.TtmlHelpers;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.CencSampleEncryptionInformationGroupEntry;
import com.googlecode.mp4parser.util.Mp4Arrays;
import com.googlecode.mp4parser.util.Path;
import com.googlecode.mp4parser.util.UUIDConverter;
import com.mp4parser.iso23001.part7.ProtectionSystemSpecificHeaderBox;
import mpegCenc2013.DefaultKIDAttribute;
import mpegDashSchemaMpd2011.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.castlabs.dash.helpers.DashHelper.*;
import static com.castlabs.dash.helpers.FileHelpers.isMp4;
import static com.castlabs.dash.helpers.LanguageHelper.getFilesLanguage;
import static com.castlabs.dash.helpers.ManifestHelper.getApproxTrackSize;
import static com.castlabs.dash.helpers.ManifestHelper.getXmlOptions;


/**
 *
 */
public class DashFileSetSequence {
    private static Logger LOG = Logger.getLogger(DashFileSetSequence.class.getName());

    static Set<String> supportedTypes = new HashSet<>(Arrays.asList("ac-3", "ec-3", "dtsl", "dtsh", "dtse", "avc1", "avc3", "mp4a", "h264", "hev1", "hvc1"));
    protected UUID audioKeyid;
    protected SecretKey audioKey;
    protected UUID videoKeyid;
    protected SecretKey videoKey;

    protected Map<UUID, List<ProtectionSystemSpecificHeaderBox>> psshBoxes = new HashMap<>();
    protected Map<UUID, List<org.mp4parser.boxes.iso23001.part7.ProtectionSystemSpecificHeaderBox>> psshBoxes2 = new HashMap<>();

    protected List<FileAndTrackSelector> inputFilesOrig;
    protected List<FileAndTrackSelector> inputFiles;
    protected File outputDirectory = new File(System.getProperty("user.dir"));
    protected int sparse = 0;
    protected int clearlead = 0;
    protected String encryptionAlgo = "cenc";
    protected boolean explode = false;
    protected String mediaPattern = "$RepresentationID$/media-$Time$.mp4";
    protected String initPattern = "$RepresentationID$/init.mp4";

    protected String mainLang = "eng";
    protected boolean avc1ToAvc3 = false;


    // This is purely for debugging purposes and WILL weaken security when set to true
    protected boolean dummyIvs = false;
    protected boolean encryptButClear = false;

    protected int minAudioSegmentDuration = 15;
    protected int minVideoSegmentDuration = 4;

    protected List<File> subtitles;
    protected List<File> closedCaptions;
    protected List<File> trickModeFiles;

    protected Map<String, String> languageMap = new HashMap<>();


    DocumentBuilder documentBuilder;

    public DashFileSetSequence() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        //dbf.setNamespaceAware(true);
        try {
            documentBuilder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

    }

    public void setPsshBoxes(Map<UUID, List<ProtectionSystemSpecificHeaderBox>> psshBoxes) {
        this.psshBoxes = psshBoxes;
        this.psshBoxes2 = new HashMap<>();
        for (UUID uuid : psshBoxes.keySet()) {
            List<org.mp4parser.boxes.iso23001.part7.ProtectionSystemSpecificHeaderBox> l = new ArrayList<>();
            for (ProtectionSystemSpecificHeaderBox pssh : psshBoxes.get(uuid)) {
                org.mp4parser.boxes.iso23001.part7.ProtectionSystemSpecificHeaderBox pssh2
                        = new org.mp4parser.boxes.iso23001.part7.ProtectionSystemSpecificHeaderBox();
                pssh2.setSystemId(pssh.getSystemId());
                pssh2.setKeyIds(pssh.getKeyIds());
                pssh2.setContent(pssh.getContent());
                l.add(pssh2);
            }
        }
    }

    public void setSubtitles(List<File> subtitles) {
        this.subtitles = subtitles;
    }

    public void setClosedCaptions(List<File> closedCaptions) {
        this.closedCaptions = closedCaptions;
    }


    public void setLanguageMap(Map<String, String> languageMap) {
        this.languageMap = languageMap;
    }

    /**
     * Sets the minimum audio segment duration.
     *
     * @param minAudioSegmentDuration shortest allowable audio segment duration
     */
    public void setMinAudioSegmentDuration(int minAudioSegmentDuration) {
        this.minAudioSegmentDuration = minAudioSegmentDuration;
    }

    /**
     * Sets the minimum video segment duration.
     *
     * @param minVideoSegmentDuration shortest allowable video segment duration
     */
    public void setMinVideoSegmentDuration(int minVideoSegmentDuration) {
        this.minVideoSegmentDuration = minVideoSegmentDuration;
    }

    /**
     * Turns off the random number generator for IVs and therefore they start at 0x0000000000000000.
     *
     * @param dummyIvs <code>true</code> turns off the RNG
     */
    public void setDummyIvs(boolean dummyIvs) {
        this.dummyIvs = dummyIvs;
    }

    /**
     * Sets the mediaPattern for 'exploded' mode and defines under which name to store the segments.
     * <p>
     * This option has no effect when <code>explode==false</code>
     *
     * @param mediaPattern under which name to store the segments
     * @see #setExplode(boolean)
     */
    public void setMediaPattern(String mediaPattern) {
        this.mediaPattern = mediaPattern;
    }

    /**
     * Sets the initPattern for 'exploded' mode and defines under which name the init segment is stored.
     * <p>
     * This option has no effect when <code>explode==false</code>
     *
     * @param initPattern under which name the init segment is stored
     * @see #setExplode(boolean)
     */
    public void setInitPattern(String initPattern) {
        this.initPattern = initPattern;
    }

    /**
     * The dash.encrypter will convert AVC1 tracks to AVC3 tracks when flag is set.
     *
     * @param avc1ToAvc3 triggers avc1 to avc3 conversion
     */
    public void setAvc1ToAvc3(boolean avc1ToAvc3) {
        this.avc1ToAvc3 = avc1ToAvc3;
    }

    public void setEncryptionAlgo(String encryptionAlgo) {
        this.encryptionAlgo = encryptionAlgo;
    }

    public void setAudioKey(SecretKey key) {
        this.audioKey = key;
    }

    public void setVideoKey(SecretKey key) {
        this.videoKey = key;
    }

    public void setAudioKeyid(UUID keyid) {
        this.audioKeyid = keyid;
    }

    public void setVideoKeyid(UUID keyid) {
        this.videoKeyid = keyid;
    }

    public void setInputFiles(List<FileAndTrackSelector> inputFiles) {
        this.inputFiles = inputFiles;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void setExplode(boolean explode) {
        this.explode = explode;
    }

    public void setSparse(int sparse) {
        this.sparse = sparse;
    }

    public void setClearlead(int clearlead) {
        this.clearlead = clearlead;
    }

    public int run() {
        try {
            if (outputDirectory.getAbsoluteFile().exists() == outputDirectory.getAbsoluteFile().mkdirs()) {
                LOG.severe("Output directory does not exist and cannot be created.");
                return 9745;
            }

            long start = System.currentTimeMillis();

            long totalSize = 0;
            for (FileAndTrackSelector inputFile : inputFiles) {
                totalSize += inputFile.file.length();
            }
            for (File inputFile : safe(closedCaptions)) {
                totalSize += inputFile.length();
            }
            for (File inputFile : safe(subtitles)) {
                totalSize += inputFile.length();
            }
            for (File inputFile : safe(trickModeFiles)) {
                totalSize += inputFile.length();
            }

            Map<TrackProxy, String> track2File = createTracks();

            checkUnhandledFile();

            alignEditsToZero(track2File);
            fixAppleOddity(track2File);
            useNegativeCtsToPreventEdits(track2File);

            Map<TrackProxy, UUID> track2KeyId = assignKeyIds(track2File);
            Map<UUID, SecretKey> keyId2Key = createKeyMap();

            encryptTracks(track2File, track2KeyId, keyId2Key);

            // sort by language and codec
            Map<String, List<TrackProxy>> adaptationSets = findAdaptationSets(track2File.keySet());

            Map<String, String> adaptationSet2Role = setAdaptionSetRoles(adaptationSets);

            // Track sizes are expensive to calculate -> save them for later
            Map<TrackProxy, Long> trackSizes = calculateTrackSizes(adaptationSets);

            // sort within the track families by size to get stable output
            sortTrackFamilies(adaptationSets, trackSizes);

            // calculate the fragment start samples once & save them for later
            Map<TrackProxy, long[]> track2SegmentStartSamples = findFragmentStartSamples(adaptationSets);

            // calculate bitrates
            Map<TrackProxy, Long> trackBitrate = calculateBitrate(adaptationSets, trackSizes);

            // generate filenames for later reference
            Map<TrackProxy, String> trackFilename = generateFilenames(track2File);

            // export the dashed single track MP4s
            Map<TrackProxy, Container> track2CsfStructure = createSingleTrackDashedMp4s(track2SegmentStartSamples, trackFilename);

            Map<TrackProxy, List<File>> trackToFileRepresentation = writeFiles(trackFilename, track2CsfStructure, trackBitrate);

            generateManifest(
                    adaptationSets, trackBitrate, trackFilename, track2CsfStructure, trackToFileRepresentation, adaptationSet2Role);


            LOG.info(String.format("Finished fragmenting of %dMB in %.1fs", totalSize / 1024 / 1024, (double) (System.currentTimeMillis() - start) / 1000));
            for (TrackProxy trackProxy : trackToFileRepresentation.keySet()) {
                trackProxy.close();
            }
            return 0;
        } catch (ExitCodeException e) {
            LOG.severe(e.getMessage());
            return e.getExitCode();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return 9015;
        }
    }

    protected void generateManifest(Map<String, List<TrackProxy>> adaptationSets, Map<TrackProxy, Long> trackBitrate, Map<TrackProxy, String> trackFilename, Map<TrackProxy, Container> track2CsfStructure, Map<TrackProxy, List<File>> trackToFileRepresentation, Map<String, String> adaptationSet2Role) throws IOException, ExitCodeException {
        MPDDocument manifest = createManifest(
                adaptationSets, trackBitrate, trackFilename, track2CsfStructure, trackToFileRepresentation, adaptationSet2Role);

        writeManifest(manifest);

    }

    protected Map<String, String> setAdaptionSetRoles(Map<String, List<TrackProxy>> adaptationSets) {
        Map<String, String> roles = new HashMap<String, String>();
        for (Map.Entry<String, List<TrackProxy>> stringListEntry : adaptationSets.entrySet()) {
            if (stringListEntry.getValue().get(0).getHandler().contains("vide")) {
                roles.put(stringListEntry.getKey(), "urn:mpeg:dash:role:2011|main");
            } else if (stringListEntry.getValue().get(0).getHandler().contains("soun")) {
                String lang = stringListEntry.getValue().get(0).getTrackMetaData().getLanguage();
                if (lang.equals(mainLang) || lang.equals("und")) {
                    roles.put(stringListEntry.getKey(), "urn:mpeg:dash:role:2011|main");
                } else {
                    roles.put(stringListEntry.getKey(), "urn:mpeg:dash:role:2011|dub");
                }
            }
        }
        return roles;
    }


    private void useNegativeCtsToPreventEdits(Map<TrackProxy, String> track2File) {
        for (Map.Entry<TrackProxy, String> entry : track2File.entrySet()) {
            TrackProxy track = entry.getKey();
            if (NegativeCtsInsteadOfEdit.benefitsFromChange(track.getTarget())) {
                track.setTarget(new NegativeCtsInsteadOfEdit(track.getTarget()));
            }
        }
    }

    public Map<UUID, SecretKey> createKeyMap() {
        Map<UUID, SecretKey> keyIds = new HashMap<UUID, SecretKey>();
        keyIds.put(audioKeyid, audioKey);
        keyIds.put(videoKeyid, videoKey);
        return keyIds;
    }

    public Map<TrackProxy, UUID> assignKeyIds(Map<TrackProxy, String> track2File) {
        Map<TrackProxy, UUID> keyIds = new HashMap<TrackProxy, UUID>();
        for (TrackProxy track : track2File.keySet()) {
            if (track.getHandler().equals("soun") && audioKeyid != null) {
                keyIds.put(track, audioKeyid);
            } else if (track.getHandler().equals("vide") && videoKeyid != null) {
                keyIds.put(track, videoKeyid);
            }
        }
        return keyIds;
    }

    public Map<TrackProxy, List<File>> writeFiles(
            Map<TrackProxy, String> trackFilename,
            Map<TrackProxy, Container> dashedFiles,
            Map<TrackProxy, Long> trackBitrate) throws IOException {
        if (!explode) {
            return writeFilesSingleSidx(trackFilename, dashedFiles);
        } else {
            return writeFilesExploded(trackFilename, dashedFiles, trackBitrate, outputDirectory, initPattern, mediaPattern);
        }
    }


    public MPDDocument createManifest(Map<String, List<TrackProxy>> trackFamilies, Map<TrackProxy, Long> trackBitrate,
                                      Map<TrackProxy, String> representationIds,
                                      Map<TrackProxy, Container> dashedFiles, Map<TrackProxy, List<File>> trackToFile, Map<String, String> adaptationSet2Role) throws IOException, ExitCodeException {
        MPDDocument mpdDocument;
        if (!explode) {
            mpdDocument = getManifestSingleSidx(trackFamilies, trackBitrate, representationIds, dashedFiles, adaptationSet2Role);
        } else {
            mpdDocument = getManifestSegmentList(trackFamilies, trackBitrate, representationIds, dashedFiles, trackToFile, adaptationSet2Role);
        }

        DescriptorType subtitleRole = DescriptorType.Factory.newInstance();
        subtitleRole.setSchemeIdUri("urn:mpeg:dash:role");
        subtitleRole.setValue("subtitle");
        addTextTracks(mpdDocument, subtitles, new DescriptorType[]{subtitleRole}, new DescriptorType[0]);

        DescriptorType captionRole = DescriptorType.Factory.newInstance();
        captionRole.setSchemeIdUri("urn:mpeg:dash:role");
        captionRole.setValue("caption");
        addTextTracks(mpdDocument, closedCaptions, new DescriptorType[]{captionRole}, new DescriptorType[0]);

        addTrickModeTracks(mpdDocument);

        ManifestOptimizer.optimize(mpdDocument);
        return mpdDocument;
    }


    private void addTrickModeTracks(MPDDocument mpdDocument) throws IOException {
        List<Mp4RepresentationBuilder> trickModeRepresentations = new ArrayList<Mp4RepresentationBuilder>();
        for (File trickModeFile : safe(trickModeFiles)) {
            if (isMp4(trickModeFile)) {
                org.mp4parser.muxer.Movie movie = org.mp4parser.muxer.container.mp4.MovieCreator.build(trickModeFile.getAbsolutePath());
                for (org.mp4parser.muxer.Track track : movie.getTracks()) {
                    if ("vide".equals(track.getHandler())) {
                        if (videoKeyid != null) {
                            track = new org.mp4parser.muxer.tracks.encryption.CencEncryptingTrackImpl(track, videoKeyid, videoKey, false);
                        }
                        org.mp4parser.muxer.builder.DefaultFragmenterImpl fragmenter = new org.mp4parser.muxer.builder.DefaultFragmenterImpl(minVideoSegmentDuration);
                        trickModeRepresentations.add(
                                new RepresentationBuilderImpl(
                                        track, psshBoxes2.get(videoKeyid), trickModeFile.getName(), fragmenter.sampleNumbers(track), fragmenter.sampleNumbers(track)));
                    } else {
                        LOG.warning("Excluding " + trickModeFile + " track " + track.getTrackMetaData().getTrackId() + " as it's not a video track");

                    }
                }
            } else if (trickModeFile.getName().endsWith(".h264") || trickModeFile.getName().endsWith(".264")) {
                org.mp4parser.muxer.Track track = new org.mp4parser.muxer.tracks.h264.H264TrackImpl(new org.mp4parser.muxer.FileDataSourceViaHeapImpl(trickModeFile));
                if (videoKeyid != null) {
                    track = new org.mp4parser.muxer.tracks.encryption.CencEncryptingTrackImpl(track, videoKeyid, videoKey, false);
                }
                org.mp4parser.muxer.builder.DefaultFragmenterImpl fragmenter = new org.mp4parser.muxer.builder.DefaultFragmenterImpl(minVideoSegmentDuration);
                trickModeRepresentations.add(new RepresentationBuilderImpl(track, psshBoxes2.get(videoKeyid), trickModeFile.getName(), fragmenter.sampleNumbers(track), fragmenter.sampleNumbers(track)));
            } else {
                throw new IOException("Trick Mode file " + trickModeFile + " is not a valid trick mode file. Expecting *.[h]264 or *.mp4");
            }
        }

        if (!trickModeRepresentations.isEmpty()) {
            LOG.info("Creating Trick Mode AdaptationSet");
            AdaptationSetType adaptationSet = mpdDocument.getMPD().getPeriodArray(0).addNewAdaptationSet();
            DescriptorType essentialProperty = adaptationSet.addNewEssentialProperty();
            essentialProperty.setSchemeIdUri("http://dashif.org/guide-lines/trickmode");
            essentialProperty.setValue("1"); // an AdaptationSet built with ExplodedSegmentListManifestWriter or SegmentBaseSingleSidxManifestWriterImpl always has id "1" if it's a video.

            ArrayList<RepresentationType> representations = new ArrayList<RepresentationType>();
            for (Mp4RepresentationBuilder mp4RepresentationBuilder : trickModeRepresentations) {
                LOG.fine("Creating Trick Mode Representation for " + mp4RepresentationBuilder.getSource());
                double seconds = (double) mp4RepresentationBuilder.getTrack().getDuration() / mp4RepresentationBuilder.getTrack().getTrackMetaData().getTimescale();
                // todo find actual main video FPS - will happen when
                long maxPlayoutRate = Math.round((double) 25 / ((double) mp4RepresentationBuilder.getTrack().getSamples().size() / seconds));
                RepresentationType representation = writeDataAndCreateRepresentation(mp4RepresentationBuilder, Locale.forLanguageTag(mp4RepresentationBuilder.getTrack().getTrackMetaData().getLanguage()));
                representation.setMaxPlayoutRate(maxPlayoutRate);
                representations.add(representation);
            }
            adaptationSet.setRepresentationArray(representations.toArray(new RepresentationType[representations.size()]));
            LOG.info("Trick Mode AdaptationSet: Done.");
        }
    }

    RepresentationType writeDataAndCreateRepresentation(Mp4RepresentationBuilder mp4RepresentationBuilder, Locale locale) throws IOException {
        RepresentationType representation;
        String id = filename2UrlPath((mp4RepresentationBuilder.getSource()));
        if (explode) {
            representation = mp4RepresentationBuilder.getLiveRepresentation();

            representation.getSegmentTemplate().setInitialization2(initPattern.replace("%lang%", locale.toLanguageTag()));
            representation.getSegmentTemplate().setMedia(mediaPattern.replace("%lang%", locale.toLanguageTag()));
            representation.setId(id);
            RepresentationBuilderToFile.writeLive(mp4RepresentationBuilder, representation, outputDirectory);
        } else {
            representation = mp4RepresentationBuilder.getOnDemandRepresentation();
            representation.addNewBaseURL().setStringValue(id + ".mp4");
            representation.setId(id);
            RepresentationBuilderToFile.writeOnDemand(mp4RepresentationBuilder, representation, outputDirectory);
        }
        return representation;
    }


    public <T> Map<Track, T> t(Map<TrackProxy, T> mapIn) {
        Map<Track, T> mapOut = new HashMap<Track, T>();
        for (Map.Entry<TrackProxy, T> trackProxyTEntry : mapIn.entrySet()) {
            mapOut.put(trackProxyTEntry.getKey().getTarget(), trackProxyTEntry.getValue());
        }
        return mapOut;
    }

    public List<Track> t(List<TrackProxy> listIn) {
        List<Track> listOut = new ArrayList<Track>();
        for (TrackProxy trackProxy : listIn) {
            listOut.add(trackProxy.getTarget());
        }
        return listOut;
    }

    public <T> Map<T, List<Track>> tt(Map<T, List<TrackProxy>> mapIn) {
        Map<T, List<Track>> mapOut;
        if (mapIn instanceof LinkedHashMap) {
            mapOut = new LinkedHashMap<T, List<Track>>();
        } else {
            mapOut = new HashMap<T, List<Track>>();
        }

        for (Map.Entry<T, List<TrackProxy>> tListEntry : mapIn.entrySet()) {
            mapOut.put(tListEntry.getKey(), t(tListEntry.getValue()));
        }
        return mapOut;
    }

    protected MPDDocument getManifestSegmentList(
            Map<String, List<TrackProxy>> trackFamilies,
            Map<TrackProxy, Long> trackBitrate,
            Map<TrackProxy, String> representationIds,
            Map<TrackProxy, Container> dashedFiles,
            Map<TrackProxy, List<File>> trackToFile,
            Map<String, String> adaptationSet2Role) throws IOException {
        return new ExplodedSegmentListManifestWriterImpl(this,
                tt(trackFamilies), t(dashedFiles), t(trackBitrate), t(representationIds),
                t(trackToFile), initPattern, mediaPattern, true, adaptationSet2Role).getManifest();
    }

    protected MPDDocument getManifestSingleSidx(
            Map<String, List<TrackProxy>> trackFamilies,
            Map<TrackProxy, Long> trackBitrate,
            Map<TrackProxy, String> representationIds,
            Map<TrackProxy, Container> dashedFiles,
            Map<String, String> adaptationSet2Role) throws IOException {
        return new SegmentBaseSingleSidxManifestWriterImpl(this,
                tt(trackFamilies), t(dashedFiles),
                t(trackBitrate), t(representationIds), true, adaptationSet2Role).getManifest();
    }

    public <E> Collection<E> safe(Collection<E> c) {
        if (c == null) {
            return Collections.emptySet();
        } else {
            return c;
        }
    }

    public void addTextTracks(MPDDocument mpdDocument, List<File> textTracks, DescriptorType[] roles, DescriptorType[] essentialProperties) throws IOException {
        for (File textTrack : safe(textTracks)) {

            addRawTextTrack(mpdDocument, textTrack, roles, essentialProperties);
            addMuxedTextTrack(mpdDocument, textTrack, roles, essentialProperties);
        }
    }

    private void addMuxedTextTrack(MPDDocument mpdDocument, File textTrackFile, DescriptorType[] roles, DescriptorType[] essentialProperties) throws IOException {
        LOG.info("Creating Muxed Text Track AdaptationSet for " + textTrackFile.getName());
        PeriodType period = mpdDocument.getMPD().getPeriodArray()[0];

        org.mp4parser.muxer.Track textTrack;
        if (textTrackFile.getName().endsWith(".xml") || textTrackFile.getName().endsWith(".dfxp") || textTrackFile.getName().endsWith(".ttml")) {
            try {
                textTrack = new org.mp4parser.muxer.tracks.ttml.TtmlTrackImpl(textTrackFile.getName() + "-mp4",
                        Collections.singletonList(documentBuilder.parse(textTrackFile)));
            } catch (SAXException | ParserConfigurationException | URISyntaxException e) {
                throw new IOException(e);
            } catch (XPathExpressionException e) {
                throw new IOException(e);
            }

        } else if (textTrackFile.getName().endsWith(".vtt")) {
            textTrack = new org.mp4parser.muxer.tracks.webvtt.WebVttTrack(new FileInputStream(textTrackFile), textTrackFile.getName(), getTextTrackLocale(textTrackFile));
        } else {
            throw new RuntimeException("Not sure what kind of textTrack " + textTrackFile.getName() + " is.");
        }
        Locale locale = getTextTrackLocale(textTrackFile);
        org.mp4parser.muxer.builder.DefaultFragmenterImpl fragmenter = new org.mp4parser.muxer.builder.DefaultFragmenterImpl(30);
        Mp4RepresentationBuilder mp4RepresentationBuilder =
                new RepresentationBuilderImpl(textTrack, Collections.emptyList(), textTrackFile.getName(), fragmenter.sampleNumbers(textTrack), fragmenter.sampleNumbers(textTrack));
        RepresentationType representation = writeDataAndCreateRepresentation(mp4RepresentationBuilder, locale);
        AdaptationSetType adaptationSet = period.addNewAdaptationSet();

        adaptationSet.setLang(locale.getLanguage() + ("".equals(locale.getScript()) ? "" : "-" + locale.getScript()));
        adaptationSet.setMimeType("application/mp4");
        adaptationSet.setRepresentationArray(new RepresentationType[]{representation});
        adaptationSet.setRoleArray(roles);
        adaptationSet.setEssentialPropertyArray(essentialProperties);

        LOG.info("Muxed Text Track AdaptationSet: Done.");
    }


    public void addRawTextTrack(MPDDocument mpdDocument, File textTrack, DescriptorType[] roles, DescriptorType[] essentialProperties) throws IOException {

        LOG.info("Creating Raw Text Track AdaptationSet for " + textTrack.getName());
        PeriodType period = mpdDocument.getMPD().getPeriodArray()[0];
        AdaptationSetType adaptationSet = period.addNewAdaptationSet();
        File tracksOutputDir = new File(outputDirectory, FilenameUtils.getBaseName(textTrack.getName()));
        if (tracksOutputDir.getAbsoluteFile().exists() == tracksOutputDir.getAbsoluteFile().mkdirs()) {
            LOG.severe("Track's output directory does not exist and cannot be created (" + tracksOutputDir.getAbsolutePath() + ")");
        }
        if (textTrack.getName().endsWith(".xml") || textTrack.getName().endsWith(".dfxp") || textTrack.getName().endsWith(".ttml")) {
            if (textTrack.getName().endsWith(".dfxp")) {
                adaptationSet.setMimeType("application/ttaf+xml");
            } else {
                adaptationSet.setMimeType("application/ttml+xml");
            }

            try {
                TtmlHelpers.deepCopyDocument(documentBuilder.parse(textTrack), new File(tracksOutputDir, textTrack.getName()));
            } catch (SAXException e) {
                throw new IOException(e);
            }

        } else if (textTrack.getName().endsWith(".vtt")) {
            adaptationSet.setMimeType("text/vtt");

            FileUtils.copyFileToDirectory(textTrack, tracksOutputDir);
        } else {
            throw new RuntimeException("Not sure what kind of textTrack " + textTrack.getName() + " is.");
        }

        Locale locale = getTextTrackLocale(textTrack);
        adaptationSet.setLang(locale.getLanguage() + ("".equals(locale.getScript()) ? "" : "-" + locale.getScript()));
        adaptationSet.setRoleArray(roles);
        adaptationSet.setEssentialPropertyArray(essentialProperties);
        RepresentationType representation = adaptationSet.addNewRepresentation();
        representation.setId(filename2UrlPath(FilenameUtils.getBaseName(textTrack.getName())+ "-" +FilenameUtils.getExtension(textTrack.getName())));
        representation.setBandwidth(0); // pointless - just invent a small number
        BaseURLType baseURL = representation.addNewBaseURL();
        baseURL.setStringValue(FilenameUtils.getBaseName(textTrack.getName()) + "/" + textTrack.getName());

        LOG.info("Raw Text Track AdaptationSet: Done.");
    }


    public void writeManifest(MPDDocument mpdDocument) throws IOException, ExitCodeException {
        File manifest1 = new File(outputDirectory, "Manifest.mpd");
        LOG.info("Writing " + manifest1);
        mpdDocument.save(manifest1, getXmlOptions());
        //LOG.info("Done.");

    }

    private void checkUnhandledFile() throws ExitCodeException {
        for (FileAndTrackSelector inputFile : inputFiles) {
            LOG.severe("Cannot identify type of " + inputFile.file);
        }
        if (inputFiles.size() > 0) {
            throw new ExitCodeException("Only extensions mp4, ismv, mov, m4v, aac, ac3, ec3, dtshd are known.", 1);
        }
    }

    public Map<TrackProxy, List<File>> writeFilesExploded(
            Map<TrackProxy, String> trackFilename,
            Map<TrackProxy, Container> dashedFiles,
            Map<TrackProxy, Long> trackBitrate,
            File outputDirectory,
            String initPattern,
            String mediaPattern) throws IOException {
        Map<TrackProxy, List<File>> trackToSegments = new HashMap<TrackProxy, List<File>>();
        for (TrackProxy t : trackFilename.keySet()) {
            SingleSidxExplode singleSidxExplode = new SingleSidxExplode(LOG);
            singleSidxExplode.setGenerateStypSdix(false);
            List<File> segments = singleSidxExplode.doIt(
                    dashedFiles.get(t), trackFilename.get(t),
                    trackBitrate.get(t), outputDirectory, initPattern, mediaPattern);

            //LOG.info("Done.");
            trackToSegments.put(t, segments);
        }
        return trackToSegments;
    }

    public Map<TrackProxy, List<File>> writeFilesSingleSidx(Map<TrackProxy, String> trackFilename, Map<TrackProxy, Container> dashedFiles) throws IOException {
        Map<TrackProxy, List<File>> track2Files = new HashMap<TrackProxy, List<File>>();
        for (Map.Entry<TrackProxy, Container> trackContainerEntry : dashedFiles.entrySet()) {
            File f = new File(outputDirectory, trackFilename.get(trackContainerEntry.getKey()));
            if (f.exists()) {
                for (FileAndTrackSelector file : inputFilesOrig) {
                    if (file.file.getAbsolutePath().equals(f.getAbsolutePath())) {
                        throw new IOException("Please choose another output directory as current setting causes input files to be overwritten");
                    }
                }
            }
        }
        for (Map.Entry<TrackProxy, Container> trackContainerEntry : dashedFiles.entrySet()) {

            TrackProxy t = trackContainerEntry.getKey();
            File f = new File(outputDirectory, trackFilename.get(t));
            LOG.info("Writing " + f.getAbsolutePath());
            WritableByteChannel wbc = new FileOutputStream(f).getChannel();
            try {
                List<Box> boxes = trackContainerEntry.getValue().getBoxes();
                for (int i = 0; i < boxes.size(); i++) {
                    LOG.finest("Writing... " + boxes.get(i).getType() + " [" + i + " of " + boxes.size() + "]");
                    boxes.get(i).getBox(wbc);
                }

            } finally {
                wbc.close();
            }
            //LOG.info("Done.");
            track2Files.put(t, Collections.singletonList(f));
        }
        return track2Files;
    }

    public DashBuilder getFileBuilder(Fragmenter fragmenter, Movie m) {
        DashBuilder dashBuilder = new DashBuilder();
        dashBuilder.setFragmenter(fragmenter);
        return dashBuilder;
    }

    public Map<TrackProxy, Container> createSingleTrackDashedMp4s(
            Map<TrackProxy, long[]> fragmentStartSamples,
            Map<TrackProxy, String> filenames) throws IOException {

        HashMap<TrackProxy, Container> containers = new HashMap<TrackProxy, Container>();
        Map<Track, long[]> sampleNumbers = new HashMap<Track, long[]>();
        for (Map.Entry<TrackProxy, long[]> entry : fragmentStartSamples.entrySet()) {
            sampleNumbers.put(entry.getKey().getTarget(), entry.getValue());
        }
        for (final Map.Entry<TrackProxy, long[]> trackEntry : fragmentStartSamples.entrySet()) {
            String filename = filenames.get(trackEntry.getKey());
            Movie movie = new Movie();
            movie.addTrack(trackEntry.getKey().getTarget());

            LOG.info("Creating model for " + filename + "... ");


            DashBuilder mp4Builder = getFileBuilder(
                    new StaticFragmentIntersectionFinderImpl(sampleNumbers),
                    movie);
            Container isoFile = mp4Builder.build(movie);
            containers.put(trackEntry.getKey(), isoFile);

        }
        return containers;
    }

    public void sortTrackFamilies(Map<String, List<TrackProxy>> trackFamilies, final Map<TrackProxy, Long> sizes) {
        for (List<TrackProxy> tracks : trackFamilies.values()) {
            Collections.sort(tracks, new Comparator<TrackProxy>() {
                public int compare(TrackProxy o1, TrackProxy o2) {
                    return (int) (sizes.get(o1) - sizes.get(o2));
                }
            });
        }
    }

    /**
     * Calculates approximate track size suitable for sorting & calculating bitrate but not suitable
     * for precise calculations.
     *
     * @param trackFamilies all tracks grouped by their type.
     * @return map from track to track's size
     */
    public Map<TrackProxy, Long> calculateTrackSizes(Map<String, List<TrackProxy>> trackFamilies) {
        HashMap<TrackProxy, Long> sizes = new HashMap<TrackProxy, Long>();
        for (List<TrackProxy> tracks : trackFamilies.values()) {
            for (TrackProxy track : tracks) {
                sizes.put(track, getApproxTrackSize(track.getTarget()));
            }
        }
        return sizes;
    }

    /**
     * Calculates bitrate from sizes.
     *
     * @param trackFamilies all tracks grouped by their type.
     * @param trackSize     size per track
     * @return bitrate per track
     */
    public Map<TrackProxy, Long> calculateBitrate(Map<String, List<TrackProxy>> trackFamilies, Map<TrackProxy, Long> trackSize) {
        HashMap<TrackProxy, Long> bitrates = new HashMap<TrackProxy, Long>();
        for (List<TrackProxy> tracks : trackFamilies.values()) {
            for (TrackProxy track : tracks) {

                double duration = (double) track.getDuration() / track.getTrackMetaData().getTimescale();
                long size = trackSize.get(track);
                long bitrate = (long) ((size * 8 / duration / 100)) * 100;


                bitrates.put(track, bitrate);
            }

        }
        return bitrates;
    }

    /**
     * Generates filenames from type, language and bitrate.
     *
     * @return a descriptive filename <code>type[-lang]-bitrate.mp4</code>
     */
    public Map<TrackProxy, String> generateFilenames(Map<TrackProxy, String> trackOriginalFilename) {
        HashMap<TrackProxy, String> filenames = new HashMap<TrackProxy, String>();
        for (TrackProxy track : trackOriginalFilename.keySet()) {
            String originalFilename = trackOriginalFilename.get(track);
            originalFilename = originalFilename.replaceAll(".mov$", "");
            originalFilename = originalFilename.replaceAll(".aac$", "");
            originalFilename = originalFilename.replaceAll(".ec3$", "");
            originalFilename = originalFilename.replaceAll(".ac3$", "");
            originalFilename = originalFilename.replaceAll(".dtshd$", "");
            originalFilename = originalFilename.replaceAll(".mp4$", "");
            for (TrackProxy track1 : filenames.keySet()) {
                if (track1 != track &&
                        trackOriginalFilename.get(track1).equals(trackOriginalFilename.get(track))) {
                    // ouch multiple tracks point to same file
                    originalFilename += "_" + track.getTrackMetaData().getTrackId();
                }
            }
            if (!explode) {
                filenames.put(track, String.format("%s.mp4", originalFilename));
            } else {
                filenames.put(track, originalFilename);
            }

        }
        return filenames;
    }

    public Map<TrackProxy, long[]> findFragmentStartSamples(Map<String, List<TrackProxy>> trackFamilies) {
        Map<TrackProxy, long[]> fragmentStartSamples = new HashMap<>();


        Map<String, List<TrackProxy>> trackFamiliesForSegments = new HashMap<>();

        for (Map.Entry<String, List<TrackProxy>> stringListEntry : trackFamilies.entrySet()) {
            String shortFamily = stringListEntry.getKey().substring(0, 4);
            List<TrackProxy> tps = trackFamiliesForSegments.get(shortFamily);
            if (tps == null) {
                tps = new ArrayList<>();
                trackFamiliesForSegments.put(shortFamily, tps);
            }
            tps.addAll(stringListEntry.getValue());
        }

        for (String key : trackFamiliesForSegments.keySet()) {
            List<TrackProxy> trackProxies = trackFamiliesForSegments.get(key);



            if (trackProxies.get(0).getHandler().startsWith("vide")) {
                List<Track> tracks = new ArrayList<>();
                for (TrackProxy trackProxy : trackProxies) {
                    tracks.add(trackProxy.getTarget());
                }
                Fragmenter fragmenter = new BetterTrackGroupFragmenter(minVideoSegmentDuration, tracks);
                for (TrackProxy track : trackProxies) {
                    fragmentStartSamples.put(track, fragmenter.sampleNumbers(track.getTarget()));
                }
            } else if (trackProxies.get(0).getHandler().startsWith("soun")) {

                long[] commonSamples = null;
                for (TrackProxy track : trackProxies) {
                        Fragmenter soundIntersectionFinder = new BetterFragmenter(minAudioSegmentDuration);
                        long[] nuSamples = soundIntersectionFinder.sampleNumbers(track.getTarget());
                    if (commonSamples == null) {
                        commonSamples = nuSamples;
                    } else {
                        commonSamples = getCommonIndices(commonSamples, nuSamples);
                    }
                }
                for (TrackProxy track : trackProxies) {
                    fragmentStartSamples.put(track, commonSamples);
                }
            } else {
                throw new RuntimeException("An engineer needs to tell me if " + trackProxies.get(0).getHandler() + " is audio or video!");
            }

        }
        return fragmentStartSamples;
    }


    public static long[] getCommonIndices(long[] samples1, long[] samples2) {
        if (Arrays.equals(samples1, samples2)) {
            return samples1;
        } else {
            int i1 = 0, i2 = 0;
            long[] result = new long[0];
            while (i1 < samples1.length && i2 < samples2.length) {
                if (samples1[i1] == samples2[i2]) {
                    result = Mp4Arrays.copyOfAndAppend(result, samples1[i1]);
                    i1++;
                    i2++;
                } else if (samples1[i1] < samples2[i2]) {
                    i1++;
                } else {
                    i2++;
                }
            }
            return result;
        }
    }


    /**
     * Creates a Map with Track as key and originating filename as value.
     *
     * @return Track too originating file map
     * @throws IOException
     */
    public Map<TrackProxy, String> createTracks() throws IOException, ExitCodeException {
        List<FileAndTrackSelector> unhandled = new ArrayList<>();
        inputFilesOrig = new ArrayList<>(inputFiles);

        Map<TrackProxy, String> track2File = new LinkedHashMap<>();
        for (FileAndTrackSelector fileAndTrack : inputFiles) {
            if (fileAndTrack.file.isFile()) {
                if (isMp4(fileAndTrack.file)) {
                    IsoFile isoFile = new IsoFile(new FileDataSourceViaHeapImpl(fileAndTrack.file));
                    for (TrackBox trackBox : isoFile.getMovieBox().getBoxes(TrackBox.class)) {
                        if (!fileAndTrack.isSelected(trackBox)) {
                            LOG.info("Excluding " + fileAndTrack.file + " track " + trackBox.getTrackHeaderBox().getTrackId() + " as it is not selected by the track selectors.");
                            continue;
                        }
                        SchemeTypeBox schm = Path.getPath(trackBox, "mdia[0]/minf[0]/stbl[0]/stsd[0]/enc.[0]/sinf[0]/schm[0]");
                        if (schm != null && (schm.getSchemeType().equals("cenc") || schm.getSchemeType().equals("cbc1"))) {
                            LOG.warning("Excluding " + fileAndTrack.file + " track " + trackBox.getTrackHeaderBox().getTrackId() + " as it is encrypted. Encrypted source tracks are not yet supported");
                            continue;
                        }
                        Track track = new Mp4TrackImpl(fileAndTrack.file + "[" + trackBox.getTrackHeaderBox().getTrackId() + "]", trackBox);
                        String codec = DashHelper.getFormat(track);
                        if (!supportedTypes.contains(codec)) {
                            LOG.warning("Excluding " + fileAndTrack.file + " track " + track.getTrackMetaData().getTrackId() + " as its codec " + codec + " is not yet supported");
                            continue;
                        }


                        track2File.put(new TrackProxy(track), fileAndTrack.file.getName());
                    }
                } else if (fileAndTrack.file.getName().endsWith(".aac")) {
                    Track track = new AACTrackImpl(new FileDataSourceViaHeapImpl(fileAndTrack.file));
                    track.getTrackMetaData().setLanguage(getFilesLanguage(fileAndTrack.file).getISO3Language());
                    track2File.put(new TrackProxy(track), fileAndTrack.file.getName());
                    assertOptionEmpty(fileAndTrack);
                    LOG.fine("Created AAC Track from " + fileAndTrack.file.getName());
                } else if (fileAndTrack.file.getName().endsWith(".h264")) {
                    Track track = new H264TrackImpl(new FileDataSourceViaHeapImpl(fileAndTrack.file));
                    track2File.put(new TrackProxy(track), fileAndTrack.file.getName());
                    assertOptionEmpty(fileAndTrack);
                    LOG.fine("Created H264 Track from " + fileAndTrack.file.getName());
                } else if (fileAndTrack.file.getName().endsWith(".ac3")) {
                    Track track = new AC3TrackImpl(new FileDataSourceViaHeapImpl(fileAndTrack.file));
                    track.getTrackMetaData().setLanguage(getFilesLanguage(fileAndTrack.file).getISO3Language());
                    track2File.put(new TrackProxy(track), fileAndTrack.file.getName());
                    assertOptionEmpty(fileAndTrack);
                    LOG.fine("Created AC3 Track from " + fileAndTrack.file.getName());
                } else if (fileAndTrack.file.getName().endsWith(".ec3")) {
                    Track track = new EC3TrackImpl(new FileDataSourceViaHeapImpl(fileAndTrack.file));
                    track.getTrackMetaData().setLanguage(getFilesLanguage(fileAndTrack.file).getISO3Language());
                    track2File.put(new TrackProxy(track), fileAndTrack.file.getName());
                    assertOptionEmpty(fileAndTrack);
                    LOG.fine("Created EC3 Track from " + fileAndTrack.file.getName());
                } else if (fileAndTrack.file.getName().endsWith(".dtshd")) {
                    Track track = new DTSTrackImpl(new FileDataSourceViaHeapImpl(fileAndTrack.file));
                    track.getTrackMetaData().setLanguage(getFilesLanguage(fileAndTrack.file).getISO3Language());
                    track2File.put(new TrackProxy(track), fileAndTrack.file.getName());
                    assertOptionEmpty(fileAndTrack);
                    LOG.fine("Created DTS HD Track from " + fileAndTrack.file.getName());
                } else {
                    unhandled.add(fileAndTrack);
                }
            }
        }
        for (TrackProxy trackProxy : track2File.keySet()) {
            String newLang = languageMap.get(trackProxy.getTarget().getTrackMetaData().getLanguage());
            if (newLang != null) {
                LOG.info("Replacing input language " + trackProxy.getTarget().getTrackMetaData().getLanguage() + " in " + trackProxy + " with " + newLang);
                trackProxy.getTarget().getTrackMetaData().setLanguage(newLang);
            }
        }
        inputFiles.retainAll(unhandled);
        if (avc1ToAvc3) {
            for (Map.Entry<TrackProxy, String> trackStringEntry : track2File.entrySet()) {
                if ("avc1".equals(trackStringEntry.getKey().getSampleDescriptionBox().getSampleEntry().getType())) {
                    trackStringEntry.getKey().setTarget(new Avc1ToAvc3TrackImpl(trackStringEntry.getKey().getTarget()));
                }
            }
        }
        if (track2File.isEmpty()) {
            throw new ExitCodeException("No tracks found for creating DASH stream.", 9283);
        }
        return track2File;
    }

    private void assertOptionEmpty(FileAndTrackSelector fileAndTrack) throws ExitCodeException {
        if (fileAndTrack.language != null || fileAndTrack.type != null || fileAndTrack.trackId >= 0) {
            throw new ExitCodeException(fileAndTrack + " references a bitstream file but contains track selectors (" + fileAndTrack + ")", 237126);
        }
    }

    long[] longSet2Array(Set<Long> longSet) {
        long[] r = new long[longSet.size()];
        List<Long> longList = new ArrayList<Long>(longSet);
        for (int i = 0; i < r.length; i++) {
            r[i] = longList.get(i);
        }
        Arrays.sort(r);
        return r;
    }

    public void encryptTracks(Map<TrackProxy, String> track2File, Map<TrackProxy, UUID> track2KeyId, Map<UUID, SecretKey> keyId2Key) {

        for (Map.Entry<TrackProxy, String> trackStringEntry : track2File.entrySet()) {
            if (track2KeyId.containsKey(trackStringEntry.getKey())) {
                TrackProxy t = trackStringEntry.getKey();
                UUID keyid = track2KeyId.get(t);
                SecretKey key = keyId2Key.get(keyid);


                int clearTillSample = 0;
                int numSamples = t.getSamples().size();
                if (clearlead > 0) {
                    clearTillSample = (int) (clearlead * numSamples / (t.getDuration() / t.getTrackMetaData().getTimescale()));
                }

                if (sparse == 0) {
                    CencEncryptingTrackImpl cencTrack;
                    if (clearTillSample > 0) {
                        CencSampleEncryptionInformationGroupEntry e = new CencSampleEncryptionInformationGroupEntry();
                        e.setEncrypted(false);
                        long[] excludes = new long[clearTillSample];
                        for (int i = 0; i < excludes.length; i++) {
                            excludes[i] = i;
                        }
                        cencTrack = new CencEncryptingTrackImpl(
                                t.getTarget(), keyid,
                                Collections.singletonMap(keyid, key),
                                Collections.singletonMap(e, excludes),
                                encryptionAlgo, dummyIvs, encryptButClear);

                    } else {
                        cencTrack = new CencEncryptingTrackImpl(
                                t.getTarget(), keyid,
                                Collections.singletonMap(keyid, key),
                                null, encryptionAlgo, dummyIvs, encryptButClear);
                    }
                    t.setTarget(cencTrack);
                } else if (sparse == 1) {
                    CencSampleEncryptionInformationGroupEntry e = new CencSampleEncryptionInformationGroupEntry();
                    e.setEncrypted(false);

                    Set<Long> plainSamples = new HashSet<Long>();
                    if (t.getSyncSamples() != null && t.getSyncSamples().length > 0) {
                        for (long i = 1; i <= t.getSamples().size(); i++) {
                            if (Arrays.binarySearch(t.getSyncSamples(), i) < 0 || i < clearTillSample) {
                                plainSamples.add(i);
                            }
                        }
                    } else {
                        for (int i = 0; i < clearTillSample; i++) {
                            plainSamples.add((long) i);
                        }
                        for (int i = clearTillSample; i < t.getSamples().size(); i++) {
                            if (i % 3 == 0) {
                                plainSamples.add((long) i);
                            }
                        }
                    }

                    CencEncryptingTrackImpl cencTrack = new CencEncryptingTrackImpl(
                            t.getTarget(), keyid,
                            Collections.singletonMap(keyid, key),
                            Collections.singletonMap(e, longSet2Array(plainSamples)),
                            "cenc", dummyIvs, encryptButClear);
                    t.setTarget(cencTrack);

                } else if (sparse == 2) {
                    CencSampleEncryptionInformationGroupEntry e = new CencSampleEncryptionInformationGroupEntry();
                    e.setEncrypted(true);
                    e.setKid(keyid);
                    e.setIvSize(8);


                    Set<Long> encryptedSamples = new HashSet<Long>();
                    if (t.getSyncSamples() != null && t.getSyncSamples().length > 0) {
                        for (int i = 0; i < t.getSyncSamples().length; i++) {
                            if (t.getSyncSamples()[i] >= clearTillSample) {
                                encryptedSamples.add(t.getSyncSamples()[i]);
                            }
                        }

                    } else {
                        SecureRandom r = new SecureRandom();
                        int encSamples = numSamples / 10;
                        while (--encSamples >= 0) {
                            int s = r.nextInt(numSamples - clearTillSample) + clearTillSample;
                            encryptedSamples.add((long) s);
                        }
                    }
                    t.setTarget(new CencEncryptingTrackImpl(
                            t.getTarget(), null,
                            Collections.singletonMap(keyid, key),
                            Collections.singletonMap(e, longSet2Array(encryptedSamples)),
                            "cenc", dummyIvs, encryptButClear));
                }
            }
        }

    }

    public void fixAppleOddity(Map<TrackProxy, String> track2File) {


        for (Map.Entry<TrackProxy, String> entry : track2File.entrySet()) {
            TrackProxy track = entry.getKey();
            if (Path.getPath(track.getSampleDescriptionBox(), "...a/wave/esds") != null) { // mp4a or enca
                final SampleDescriptionBox stsd = track.getSampleDescriptionBox();
                AudioSampleEntry ase = (AudioSampleEntry) stsd.getSampleEntry();
                List<Box> aseBoxes = new ArrayList<Box>();
                aseBoxes.add(Path.getPath(stsd, "...a/wave/esds"));
                for (Box box : ase.getBoxes()) {
                    if (!box.getType().equals("wave")) {
                        aseBoxes.add(box);
                    }
                }
                ase.setBoxes(Collections.<Box>emptyList());
                for (Box aseBox : aseBoxes) {
                    ase.addBox(aseBox);
                }
                track.setTarget(new StsdCorrectingTrack(track.getTarget(), stsd));
            }
        }
    }

    /**
     * In DASH Some tracks might have an earliest presentation timestamp < 0
     *
     * @param track2File map from track object to originating file
     */
    public void alignEditsToZero(Map<TrackProxy, String> track2File) {

        double earliestMoviePresentationTime = 0;
        Map<TrackProxy, Double> startTimes = new HashMap<TrackProxy, Double>();
        Map<TrackProxy, Double> ctsOffset = new HashMap<TrackProxy, Double>();

        for (TrackProxy track : track2File.keySet()) {

            List<Edit> edits = track.getEdits();

            double earliestTrackPresentationTime = getEarliestTrackPresentationTime(edits);

            if (track.getCompositionTimeEntries() != null && track.getCompositionTimeEntries().size() > 0) {
                long currentTime = 0;
                int[] ptss = Arrays.copyOfRange(CompositionTimeToSample.blowupCompositionTimes(track.getCompositionTimeEntries()), 0, 50);
                for (int j = 0; j < ptss.length; j++) {
                    ptss[j] += currentTime;
                    currentTime += track.getSampleDurations()[j];
                }
                Arrays.sort(ptss);
                earliestTrackPresentationTime += (double) ptss[0] / track.getTrackMetaData().getTimescale();
                ctsOffset.put(track, (double) ptss[0] / track.getTrackMetaData().getTimescale());
            } else {
                ctsOffset.put(track, 0.0);
            }
            startTimes.put(track, earliestTrackPresentationTime);
            earliestMoviePresentationTime = Math.min(earliestMoviePresentationTime, earliestTrackPresentationTime);
        }
        for (TrackProxy track : track2File.keySet()) {
            double adjustedStartTime = startTimes.get(track) - earliestMoviePresentationTime - ctsOffset.get(track);
            if (earliestMoviePresentationTime != 0) {
                LOG.info("Adjusted earliest presentation of " + track.getName() + " from " + startTimes.get(track) + " to " + (startTimes.get(track) - earliestMoviePresentationTime));
            }

            final List<Edit> edits = BoxHelper.getEdits(track.getTarget(), adjustedStartTime);
            track.setTarget(new WrappingTrack(track.getTarget()) {
                @Override
                public List<Edit> getEdits() {
                    return edits;
                }
            });
        }
    }


    public Map<String, List<TrackProxy>> findAdaptationSets(Set<TrackProxy> allTracks) throws IOException, ExitCodeException {
        HashMap<String, List<TrackProxy>> trackFamilies = new LinkedHashMap<String, List<TrackProxy>>();
        for (TrackProxy track : allTracks) {
            String family;

            if (track.getTarget().getHandler().equals("soun")) {
                int channels = ((AudioSampleEntry) track.getSampleDescriptionBox().getSampleEntry()).getChannelCount();
                family = DashHelper.getRfc6381Codec(track.getSampleDescriptionBox().getSampleEntry()) +
                        "-" + track.getTrackMetaData().getLanguage() + "-" + channels + "ch";
            } else {
                family = DashHelper.getFormat(track.getTarget());
            }

            List<TrackProxy> tracks = trackFamilies.get(family);
            if (tracks == null) {
                tracks = new ArrayList<TrackProxy>();
                trackFamilies.put(family, tracks);
            }
            tracks.add(track);
        }

        for (String fam : trackFamilies.keySet()) {
            List<TrackProxy> tracks = trackFamilies.get(fam);
            long timeScale = -1;
            for (TrackProxy track : tracks) {
                if (timeScale > 0) {
                    if (timeScale != track.getTrackMetaData().getTimescale()) {
                        throw new ExitCodeException("The tracks " + tracks.get(0) + " and " + track + " have been assigned the same adaptation set but their timescale differs: " + timeScale + " vs. " + track.getTrackMetaData().getTimescale(), 38743);
                    }
                } else {
                    timeScale = track.getTrackMetaData().getTimescale();
                }
            }
        }


        return trackFamilies;
    }

    public void setTrickModeFiles(List<File> trickModeFiles) {
        this.trickModeFiles = trickModeFiles;
    }


    private class StsdCorrectingTrack extends WrappingTrack {
        SampleDescriptionBox stsd;

        public StsdCorrectingTrack(Track track, SampleDescriptionBox stsd) {
            super(track);
            this.stsd = stsd;
        }

        public SampleDescriptionBox getSampleDescriptionBox() {
            return stsd;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    public void addContentProtection(AdaptationSetType adaptationSet, UUID keyId) {
        DescriptorType contentProtection = adaptationSet.addNewContentProtection();
        final DefaultKIDAttribute defaultKIDAttribute = DefaultKIDAttribute.Factory.newInstance();
        defaultKIDAttribute.setDefaultKID(Collections.singletonList(keyId.toString()));
        contentProtection.set(defaultKIDAttribute);
        contentProtection.setSchemeIdUri("urn:mpeg:dash:mp4protection:2011");
        contentProtection.setValue("cenc");

        List<ProtectionSystemSpecificHeaderBox> psshs = psshBoxes.get(keyId);

        for (ProtectionSystemSpecificHeaderBox pssh : safe(psshs)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                pssh.getBox(Channels.newChannel(baos));
            } catch (IOException e) {
                throw new RuntimeException(e); // unexpected
            }
            byte[] completePssh = baos.toByteArray();

            Node cpn = createContentProctionNode(adaptationSet, "urn:uuid:" + UUIDConverter.convert(pssh.getSystemId()).toString(), null);
            Document d = cpn.getOwnerDocument();
            Element psshElement = d.createElementNS("urn:mpeg:cenc:2013", "pssh");
            psshElement.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(completePssh)));
            cpn.appendChild(psshElement);

            if (Arrays.equals(ProtectionSystemSpecificHeaderBox.PLAYREADY_SYSTEM_ID, pssh.getSystemId())) {
                cpn.setNodeValue("MSPR 2.0");
                Element pro = d.createElementNS("urn:microsoft:playready", "pro");
                pro.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(pssh.getContent())));
                cpn.appendChild(pro);
            }
        }


    }

    protected Node createContentProctionNode(AdaptationSetType adaptationSet, String schemeIdUri, String value) {
        DescriptorType cpNode = adaptationSet.addNewContentProtection();
        if (schemeIdUri != null) {
            cpNode.setSchemeIdUri(schemeIdUri);
        }
        if (value != null) {
            cpNode.setValue(value);
        }

        return cpNode.getDomNode();
    }

    /**
     * Exception to force exit from tool in functions - kind of goto.
     */
    protected static class ExitCodeException extends Exception {
        int exitCode;

        public ExitCodeException(String message, int exitCode) {
            super(message);
            this.exitCode = exitCode;
        }

        public int getExitCode() {
            return exitCode;
        }
    }
}
