package com.castlabs.dash.dashfragmenter.sequences;

import com.castlabs.dash.dashfragmenter.ExitCodeException;
import com.castlabs.dash.dashfragmenter.formats.csf.DashBuilder;
import com.castlabs.dash.dashfragmenter.formats.csf.SegmentBaseSingleSidxManifestWriterImpl;
import com.castlabs.dash.dashfragmenter.formats.multiplefilessegmenttemplate.ExplodedSegmentListManifestWriterImpl;
import com.castlabs.dash.dashfragmenter.formats.multiplefilessegmenttemplate.SingleSidxExplode;
import com.castlabs.dash.dashfragmenter.representation.ManifestOptimizer;
import com.castlabs.dash.dashfragmenter.representation.RepresentationBuilder;
import com.castlabs.dash.dashfragmenter.representation.SyncSampleAssistedRepresentationBuilder;
import com.castlabs.dash.dashfragmenter.tracks.NegativeCtsInsteadOfEdit;
import com.castlabs.dash.helpers.BoxHelper;
import com.castlabs.dash.helpers.DashHelper;
import com.castlabs.dash.helpers.RepresentationBuilderToFile;
import com.castlabs.dash.helpers.SoundIntersectionFinderImpl;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.CompositionTimeToSample;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Edit;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.WrappingTrack;
import com.googlecode.mp4parser.authoring.builder.Fragmenter;
import com.googlecode.mp4parser.authoring.builder.StaticFragmentIntersectionFinderImpl;
import com.googlecode.mp4parser.authoring.builder.SyncSampleIntersectFinderImpl;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.*;
import com.googlecode.mp4parser.authoring.tracks.h264.H264TrackImpl;
import com.googlecode.mp4parser.authoring.tracks.ttml.TtmlHelpers;
import com.googlecode.mp4parser.authoring.tracks.ttml.TtmlTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.webvtt.WebVttTrack;
import com.googlecode.mp4parser.boxes.mp4.ESDescriptorBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.AudioSpecificConfig;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.CencSampleEncryptionInformationGroupEntry;
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

    static Set<String> supportedTypes = new HashSet<String>(Arrays.asList("ac-3", "ec-3", "dtsl", "dtsh", "dtse", "avc1", "avc3", "mp4a", "h264", "hev1", "hvc1"));
    protected UUID audioKeyid;
    protected SecretKey audioKey;
    protected UUID videoKeyid;
    protected SecretKey videoKey;

    protected Map<UUID, List<ProtectionSystemSpecificHeaderBox>> psshBoxes = new HashMap<UUID, List<ProtectionSystemSpecificHeaderBox>>();

    protected List<File> inputFilesOrig;
    protected List<File> inputFiles;
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
    }

    public void setSubtitles(List<File> subtitles) {
        this.subtitles = subtitles;
    }

    public void setClosedCaptions(List<File> closedCaptions) {
        this.closedCaptions = closedCaptions;
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
     * <p/>
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
     * <p/>
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

    public void setInputFiles(List<File> inputFiles) {
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

    public int run() throws IOException, ExitCodeException {

        if (!(outputDirectory.getAbsoluteFile().exists() ^ outputDirectory.getAbsoluteFile().mkdirs())) {
            LOG.severe("Output directory does not exist and cannot be created.");
        }

        long start = System.currentTimeMillis();

        long totalSize = 0;
        for (File inputFile : inputFiles) {
            totalSize += inputFile.length();
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
        Map<UUID, SecretKey> keyId2Key = createKeyMap(track2KeyId);

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

        MPDDocument manifest = createManifest(
                adaptationSets, trackBitrate, trackFilename, track2CsfStructure, trackToFileRepresentation, adaptationSet2Role);

        writeManifest(manifest);

        LOG.info(String.format("Finished fragmenting of %dMB in %.1fs", totalSize / 1024 / 1024, (double) (System.currentTimeMillis() - start) / 1000));
        return 0;
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

    public Map<UUID, SecretKey> createKeyMap(Map<TrackProxy, UUID> track2KeyId) {
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
                                      Map<TrackProxy, Container> dashedFiles, Map<TrackProxy, List<File>> trackToFile, Map<String, String> adaptationSet2Role) throws IOException {
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
        List<RepresentationBuilder> trickModeRepresentations = new ArrayList<RepresentationBuilder>();
        for (File trickModeFile : safe(trickModeFiles)) {
            if (isMp4(trickModeFile)) {
                Movie movie = MovieCreator.build(new FileDataSourceImpl(trickModeFile));
                for (Track track : movie.getTracks()) {
                    if ("vide".equals(track.getHandler())) {
                        if (videoKeyid != null) {
                            track = new CencEncryptingTrackImpl(track, videoKeyid, videoKey, false);
                        }
                        trickModeRepresentations.add(new SyncSampleAssistedRepresentationBuilder(track, trickModeFile.getName(), time2Frames(track, 3), psshBoxes.get(videoKeyid)));
                    } else {
                        LOG.warning("Excluding " + trickModeFile + " track " + track.getTrackMetaData().getTrackId() + " as it's not a video track");

                    }
                }
            } else if (trickModeFile.getName().endsWith(".h264") || trickModeFile.getName().endsWith(".264")) {
                Track track = new H264TrackImpl(new FileDataSourceImpl(trickModeFile));
                if (videoKeyid != null) {
                    track = new CencEncryptingTrackImpl(track, videoKeyid, videoKey, false);
                }
                trickModeRepresentations.add(new SyncSampleAssistedRepresentationBuilder(track, trickModeFile.getName(), time2Frames(track, 3), psshBoxes.get(videoKeyid)));
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
            for (RepresentationBuilder representationBuilder : trickModeRepresentations) {
                LOG.fine("Creating Trick Mode Representation for " + representationBuilder.getSource());
                double seconds = (double) representationBuilder.getTrack().getDuration() / representationBuilder.getTrack().getTrackMetaData().getTimescale();
                // todo find actual main video FPS - will happen when
                long maxPlayoutRate = Math.round((double) 25 / ((double) representationBuilder.getTrack().getSamples().size() / seconds));
                RepresentationType representation = writeDataAndCreateRepresentation(representationBuilder);
                representation.setMaxPlayoutRate(maxPlayoutRate);
                representations.add(representation);
            }
            adaptationSet.setRepresentationArray(representations.toArray(new RepresentationType[representations.size()]));
            LOG.info("Trick Mode AdaptationSet: Done.");
        }
    }

    private RepresentationType writeDataAndCreateRepresentation(RepresentationBuilder representationBuilder) throws IOException {
        RepresentationType representation;
        String id = FilenameUtils.getBaseName(representationBuilder.getSource());
        if (explode) {
            representation = representationBuilder.getLiveRepresentation();
            representation.getSegmentTemplate().setInitialization2(initPattern);
            representation.getSegmentTemplate().setMedia(mediaPattern);
            representation.setId(id);
            RepresentationBuilderToFile.writeLive(representationBuilder, representation, outputDirectory);
        } else {
            representation = representationBuilder.getOnDemandRepresentation();
            representation.addNewBaseURL().setStringValue(id + ".mp4");
            representation.setId(id);
            RepresentationBuilderToFile.writeOnDemand(representationBuilder, representation, outputDirectory);
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
        if (mapIn instanceof  LinkedHashMap) {
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
                t(trackToFile), initPattern, mediaPattern, false, adaptationSet2Role).getManifest();
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

        Track textTrack;
        if (textTrackFile.getName().endsWith(".xml") || textTrackFile.getName().endsWith(".dfxp") || textTrackFile.getName().endsWith(".ttml")) {
            try {
                textTrack = new TtmlTrackImpl(textTrackFile.getName(),
                        Collections.singletonList(documentBuilder.parse(textTrackFile)));
            } catch (SAXException e) {
                throw new IOException(e);
            } catch (ParserConfigurationException e) {
                throw new IOException(e);
            } catch (XPathExpressionException e) {
                throw new IOException(e);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }

        } else if (textTrackFile.getName().endsWith(".vtt")) {
            textTrack = new WebVttTrack(new FileInputStream(textTrackFile), textTrackFile.getName(), getTextTrackLocale(textTrackFile));
        } else {
            throw new RuntimeException("Not sure what kind of textTrack " + textTrackFile.getName() + " is.");
        }
        RepresentationBuilder representationBuilder =
                new SyncSampleAssistedRepresentationBuilder(textTrack, textTrackFile.getName(), 10, Collections.<ProtectionSystemSpecificHeaderBox>emptyList());
        RepresentationType representation = writeDataAndCreateRepresentation(representationBuilder);
        AdaptationSetType adaptationSet = period.addNewAdaptationSet();
        Locale locale = getTextTrackLocale(textTrackFile);
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
        tracksOutputDir.mkdirs();
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
        representation.setId(FilenameUtils.getBaseName(textTrack.getName()));
        representation.setBandwidth(128); // pointless - just invent a small number
        BaseURLType baseURL = representation.addNewBaseURL();
        baseURL.setStringValue(FilenameUtils.getBaseName(textTrack.getName()) + "/" + textTrack.getName());

        LOG.info("Raw Text Track AdaptationSet: Done.");
    }


    public void writeManifest(MPDDocument mpdDocument) throws IOException {
        File manifest1 = new File(outputDirectory, "Manifest.mpd");
        LOG.info("Writing " + manifest1);
        mpdDocument.save(manifest1, getXmlOptions());
        //LOG.info("Done.");

    }

    private void checkUnhandledFile() throws ExitCodeException {
        for (File inputFile : inputFiles) {
            LOG.severe("Cannot identify type of " + inputFile);
        }
        if (inputFiles.size() > 0) {
            throw new ExitCodeException("Only extensions mp4, ismv, mov, m4v, aac, ac3, ec3, dtshd and xml/vtt are known.", 1);
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
                for (File file : inputFilesOrig) {
                    if (file.getAbsolutePath().equals(f.getAbsolutePath())) {
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

    public DashBuilder getFileBuilder(Fragmenter fragmentIntersectionFinder, Movie m) {
        DashBuilder dashBuilder = new DashBuilder();
        dashBuilder.setIntersectionFinder(fragmentIntersectionFinder);
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
        Map<TrackProxy, long[]> fragmentStartSamples = new HashMap<TrackProxy, long[]>();

        Map<String, List<TrackProxy>> trackFamiliesForSegements = new HashMap<String, List<TrackProxy>>();

        for (Map.Entry<String, List<TrackProxy>> stringListEntry : trackFamilies.entrySet()) {
            String shortFamily = stringListEntry.getKey().substring(0, 4);
            List<TrackProxy> tps = trackFamiliesForSegements.get(shortFamily);
            if (tps == null) {
                tps = new ArrayList<TrackProxy>();
                trackFamiliesForSegements.put(shortFamily, tps);
            }
            tps.addAll(stringListEntry.getValue());
        }

        for (String key : trackFamiliesForSegements.keySet()) {
            List<TrackProxy> trackProxies = trackFamiliesForSegements.get(key);
            List<Track> tracks = new ArrayList<Track>();
            for (TrackProxy proxy : trackProxies) {
                tracks.add(proxy.getTarget());
            }

            Movie movie = new Movie();
            movie.setTracks(tracks);
            for (TrackProxy track : trackProxies) {
                if (track.getHandler().startsWith("vide")) {
                    Fragmenter videoIntersectionFinder = new SyncSampleIntersectFinderImpl(movie, null, minVideoSegmentDuration);
                    fragmentStartSamples.put(track, videoIntersectionFinder.sampleNumbers(track.getTarget()));
                    //fragmentStartSamples.put(track, checkMaxFragmentDuration(track, videoIntersectionFinder.sampleNumbers(track)));
                } else if (track.getHandler().startsWith("soun")) {
                    Fragmenter soundIntersectionFinder = new SoundIntersectionFinderImpl(tracks, minAudioSegmentDuration);
                    fragmentStartSamples.put(track, soundIntersectionFinder.sampleNumbers(track.getTarget()));
                } else {
                    throw new RuntimeException("An engineer needs to tell me if " + key + " is audio or video!");
                }
            }
        }
        return fragmentStartSamples;
    }

    /**
     * Creates a Map with Track as key and originating filename as value.
     *
     * @return Track too originating file map
     * @throws IOException
     */
    public Map<TrackProxy, String> createTracks() throws IOException, ExitCodeException {
        List<File> unhandled = new ArrayList<File>();
        inputFilesOrig = new ArrayList<File>(inputFiles);

        Map<TrackProxy, String> track2File = new LinkedHashMap<TrackProxy, String>();
        for (File inputFile : inputFiles) {
            if (inputFile.isFile()) {
                if (isMp4(inputFile)) {
                    Movie movie = MovieCreator.build(new FileDataSourceImpl(inputFile));
                    for (Track track : movie.getTracks()) {
                        String codec = DashHelper.getFormat(track);
                        if (!supportedTypes.contains(codec)) {
                            LOG.warning("Excluding " + inputFile + " track " + track.getTrackMetaData().getTrackId() + " as its codec " + codec + " is not yet supported");
                            break;
                        }
                        if (track instanceof CencEncryptedTrack) {
                            LOG.warning("Excluding " + inputFile + " track " + track.getTrackMetaData().getTrackId() + " as it is encrypted. Encrypted source tracks are not yet supported");
                            break;
                        }
                        track2File.put(new TrackProxy(track), inputFile.getName());
                    }
                } else if (inputFile.getName().endsWith(".aac")) {
                    Track track = new AACTrackImpl(new FileDataSourceImpl(inputFile));
                    track.getTrackMetaData().setLanguage(getFilesLanguage(inputFile).getISO3Language());
                    track2File.put(new TrackProxy(track), inputFile.getName());
                    LOG.fine("Created AAC Track from " + inputFile.getName());
                } else if (inputFile.getName().endsWith(".h264")) {
                    Track track = new H264TrackImpl(new FileDataSourceImpl(inputFile));
                    track2File.put(new TrackProxy(track), inputFile.getName());
                    LOG.fine("Created H264 Track from " + inputFile.getName());
                } else if (inputFile.getName().endsWith(".ac3")) {
                    Track track = new AC3TrackImpl(new FileDataSourceImpl(inputFile));
                    track.getTrackMetaData().setLanguage(getFilesLanguage(inputFile).getISO3Language());
                    track2File.put(new TrackProxy(track), inputFile.getName());
                    LOG.fine("Created AC3 Track from " + inputFile.getName());
                } else if (inputFile.getName().endsWith(".ec3")) {
                    Track track = new EC3TrackImpl(new FileDataSourceImpl(inputFile));
                    track.getTrackMetaData().setLanguage(getFilesLanguage(inputFile).getISO3Language());
                    track2File.put(new TrackProxy(track), inputFile.getName());
                    LOG.fine("Created EC3 Track from " + inputFile.getName());
                } else if (inputFile.getName().endsWith(".dtshd")) {
                    Track track = new DTSTrackImpl(new FileDataSourceImpl(inputFile));
                    track.getTrackMetaData().setLanguage(getFilesLanguage(inputFile).getISO3Language());
                    track2File.put(new TrackProxy(track), inputFile.getName());
                    LOG.fine("Created DTS HD Track from " + inputFile.getName());
                } else {
                    unhandled.add(inputFile);
                }
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
        return track2File;
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
     * @return a copy of the input map with zero-aligned tracks
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

            if ("mp4a".equals(DashHelper.getFormat(track.getTarget()))) {
                // we need to look at actual channel configuration
                ESDescriptorBox esds = track.getSampleDescriptionBox().getSampleEntry().getBoxes(ESDescriptorBox.class).get(0);
                AudioSpecificConfig audioSpecificConfig = esds.getEsDescriptor().getDecoderConfigDescriptor().getAudioSpecificInfo();
                family = DashHelper.getRfc6381Codec(track.getSampleDescriptionBox().getSampleEntry()) + "-" + track.getTrackMetaData().getLanguage() + "-" + audioSpecificConfig.getChannelConfiguration();
            } else if (track.getTarget().getHandler().equals("soun")) {
                int channels = ((AudioSampleEntry) track.getSampleDescriptionBox().getSampleEntry()).getChannelCount();
                family = DashHelper.getRfc6381Codec(track.getSampleDescriptionBox().getSampleEntry()) +
                        "-" + track.getTrackMetaData().getLanguage() + "-" + channels + "ch";
            } else {
                family = DashHelper.getFormat(track.getTarget()) + "-" + track.getTrackMetaData().getLanguage();
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

            if (Arrays.equals(ProtectionSystemSpecificHeaderBox.PLAYREADY_SYSTEM_ID, pssh.getSystemId())) {

                Node playReadyCPN = createContentProctionNode(adaptationSet, "urn:uuid:" + UUIDConverter.convert(pssh.getSystemId()).toString(), "MSPR 2.0");
                Document d = playReadyCPN.getOwnerDocument();
                Element pro = d.createElementNS("urn:microsoft:playready", "pro");
                Element prPssh = d.createElementNS("urn:mpeg:cenc:2013", "pssh");

                pro.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(pssh.getContent())));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    pssh.getBox(Channels.newChannel(baos));
                } catch (IOException e) {
                    throw new RuntimeException(e); // unexpected
                }
                prPssh.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(baos.toByteArray())));

                playReadyCPN.appendChild(pro);
                playReadyCPN.appendChild(prPssh);
            }
            if (Arrays.equals(ProtectionSystemSpecificHeaderBox.WIDEVINE, pssh.getSystemId())) {
                // Widevvine
                Node widevineCPN = createContentProctionNode(adaptationSet, "urn:uuid:" + UUIDConverter.convert(pssh.getSystemId()).toString(), null);
                Document d = widevineCPN.getOwnerDocument();
                Element wvPssh = d.createElementNS("urn:mpeg:cenc:2013", "pssh");
                wvPssh.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(pssh.getContent())));

                widevineCPN.appendChild(wvPssh);
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
}
