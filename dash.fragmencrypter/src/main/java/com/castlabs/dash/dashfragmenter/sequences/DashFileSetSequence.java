package com.castlabs.dash.dashfragmenter.sequences;

import com.castlabs.dash.dashfragmenter.ExitCodeException;
import com.castlabs.dash.dashfragmenter.formats.csf.DashBuilder;
import com.castlabs.dash.dashfragmenter.formats.csf.SegmentBaseSingleSidxManifestWriterImpl;
import com.castlabs.dash.dashfragmenter.formats.multiplefilessegmenttemplate.ExplodedSegmentListManifestWriterImpl;
import com.castlabs.dash.dashfragmenter.formats.multiplefilessegmenttemplate.SingleSidxExplode;
import com.castlabs.dash.dashfragmenter.tracks.NegativeCtsInsteadOfEdit;
import com.castlabs.dash.helpers.DashHelper;
import com.castlabs.dash.helpers.SoundIntersectionFinderImpl;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.CompositionTimeToSample;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.*;
import com.googlecode.mp4parser.authoring.builder.FragmentIntersectionFinder;
import com.googlecode.mp4parser.authoring.builder.StaticFragmentIntersectionFinderImpl;
import com.googlecode.mp4parser.authoring.builder.SyncSampleIntersectFinderImpl;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.*;
import com.googlecode.mp4parser.authoring.tracks.h264.H264TrackImpl;
import com.googlecode.mp4parser.boxes.mp4.ESDescriptorBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.AudioSpecificConfig;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.CencSampleEncryptionInformationGroupEntry;
import com.googlecode.mp4parser.util.Path;
import mpegCenc2013.DefaultKIDAttribute;
import mpegDashSchemaMpd2011.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 */
public class DashFileSetSequence {
    static Set<String> supportedTypes = new HashSet<String>(Arrays.asList("ac-3", "ec-3", "dtsl", "dtsh", "dtse", "avc1", "avc3", "mp4a", "h264", "hev1", "hvc1", "stpp"));
    protected UUID audioKeyid;
    protected SecretKey audioKey;
    protected UUID videoKeyid;
    protected SecretKey videoKey;

    protected List<File> inputFiles;
    protected File outputDirectory = new File(System.getProperty("user.dir"));
    protected int sparse = 0;
    protected int clearlead = 0;
    protected String encryptionAlgo = "cenc";
    protected boolean explode = false;
    protected String mediaPattern = "$RepresentationID$/media-$Time$.mp4";
    protected String initPattern = "$RepresentationID$/init.mp4";
    protected boolean generateStypSdix = true;

    protected String mainLang = "eng";
    protected boolean avc1ToAvc3 = false;

    protected Logger l;

    // This is purely for debugging purposes and WILL weaken security weh set to true
    protected boolean dummyIvs = false;
    protected boolean encryptButClear = false;

    /**
     * Turns off the random number generator for IVs and therefore they start at 0x0000000000000000.
     * @param dummyIvs <code>true</code> turns off the RNG
     */
    public void setDummyIvs(boolean dummyIvs) {
        this.dummyIvs = dummyIvs;
    }

    /**
     * Sets whether styp and sidx should be generated when 'exploding' single file into one file per segement.
     * <p/>
     * This option has no effect when <code>explode==false</code>
     *
     * @param generateStypSdix yes/no
     * @see #setExplode(boolean)
     */
    public void setGenerateStypSdix(boolean generateStypSdix) {
        this.generateStypSdix = generateStypSdix;
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

    public void setLogger(Logger l) {
        this.l = l;
    }

    public void setClearlead(int clearlead) {
        this.clearlead = clearlead;
    }


    public int run() throws IOException, ExitCodeException {

        if (!(outputDirectory.getAbsoluteFile().exists() ^ outputDirectory.getAbsoluteFile().mkdirs())) {
            l.severe("Output directory does not exist and cannot be created.");
        }

        long start = System.currentTimeMillis();

        Map<TrackProxy, String> track2File = createTracks();
        List<File> subtitles = findSubtitles();
        Map<File, String> subtitleLanguages = getSubtitleLanguages(subtitles);
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

        MPDDocument manifest = createManifest(subtitleLanguages,
                adaptationSets, trackBitrate, trackFilename, track2CsfStructure, trackToFileRepresentation, adaptationSet2Role);

        writeManifest(manifest);

        l.info("Finished write in " + (System.currentTimeMillis() - start) + "ms");
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


    public MPDDocument createManifest(Map<File, String> subtitleLanguages,
                                      Map<String, List<TrackProxy>> trackFamilies, Map<TrackProxy, Long> trackBitrate,
                                      Map<TrackProxy, String> representationIds,
                                      Map<TrackProxy, Container> dashedFiles, Map<TrackProxy, List<File>> trackToFile, Map<String, String> adaptationSet2Role) throws IOException {
        MPDDocument mpdDocument;
        if (!explode) {
            mpdDocument = getManifestSingleSidx(trackFamilies, trackBitrate, representationIds, dashedFiles, adaptationSet2Role);
        } else {
            mpdDocument = getManifestSegmentList(trackFamilies, trackBitrate, representationIds, dashedFiles, trackToFile, adaptationSet2Role);
        }
        addSubtitles(mpdDocument, subtitleLanguages);

        return mpdDocument;
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
        Map<T, List<Track>> mapOut = new HashMap<T, List<Track>>();

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

    public void addSubtitles(MPDDocument mpdDocument, Map<File, String> subtitleLanguages) throws IOException {
        for (File subtitle : subtitleLanguages.keySet()) {
            PeriodType period = mpdDocument.getMPD().getPeriodArray()[0];
            AdaptationSetType adaptationSet = period.addNewAdaptationSet();
            if (subtitle.getName().endsWith(".xml")) {
                adaptationSet.setMimeType("application/ttml+xml");
            } else if (subtitle.getName().endsWith(".vtt")) {
                adaptationSet.setMimeType("text/vtt");
            } else {
                throw new RuntimeException("Not sure what kind of subtitle " + subtitle.getName() + " is.");
            }
            adaptationSet.setLang(subtitleLanguages.get(subtitle));
            DescriptorType descriptor = adaptationSet.addNewRole();
            descriptor.setSchemeIdUri("urn:mpeg:dash:role");
            descriptor.setValue("subtitle");
            RepresentationType representation = adaptationSet.addNewRepresentation();
            representation.setId(FilenameUtils.getBaseName(subtitle.getName()));
            representation.setBandwidth(128); // pointless - just invent a small number
            BaseURLType baseURL = representation.addNewBaseURL();
            baseURL.setStringValue(subtitle.getName());
            FileUtils.copyFileToDirectory(subtitle, outputDirectory);

        }
    }

    protected XmlOptions getXmlOptions() {
        XmlOptions xmlOptions = new XmlOptions();
        //xmlOptions.setUseDefaultNamespace();
        HashMap<String, String> ns = new HashMap<String, String>();
        //ns.put("urn:mpeg:DASH:schema:MPD:2011", "");
        ns.put("urn:mpeg:cenc:2013", "cenc");
        xmlOptions.setSaveSuggestedPrefixes(ns);
        xmlOptions.setSaveAggressiveNamespaces();
        xmlOptions.setUseDefaultNamespace();
        xmlOptions.setSavePrettyPrint();
        return xmlOptions;
    }

    public void writeManifest(MPDDocument mpdDocument) throws IOException {
        File manifest1 = new File(outputDirectory, "Manifest.mpd");
        l.info("Writing " + manifest1 + "... ");
        mpdDocument.save(manifest1, getXmlOptions());
        l.info("Done.");

    }

    private void checkUnhandledFile() throws ExitCodeException {
        for (File inputFile : inputFiles) {
            l.severe("Cannot identify type of " + inputFile);
        }
        if (inputFiles.size() > 0) {
            throw new ExitCodeException("Only extensions mp4, ismv, mov, m4v, aac, ac3, ec3, dtshd and xml/vtt are known.", 1);
        }
    }

    private List<File> findSubtitles() {
        List<File> subs = new ArrayList<File>();
        List<File> unhandled = new ArrayList<File>();
        for (File inputFile : inputFiles) {
            if (inputFile.getName().endsWith(".xml")) {
                subs.add(inputFile);
            } else if (inputFile.getName().endsWith(".vtt")) {
                subs.add(inputFile);
            } else {
                unhandled.add(inputFile);
            }
        }
        inputFiles.retainAll(unhandled);
        return subs;
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
            SingleSidxExplode singleSidxExplode = new SingleSidxExplode(l);
            singleSidxExplode.setGenerateStypSdix(generateStypSdix);
            List<File> segments = singleSidxExplode.doIt(
                    dashedFiles.get(t), trackFilename.get(t),
                    trackBitrate.get(t), outputDirectory, initPattern, mediaPattern);

            l.info("Done.");
            trackToSegments.put(t, segments);
        }
        return trackToSegments;
    }

    public Map<TrackProxy, List<File>> writeFilesSingleSidx(Map<TrackProxy, String> trackFilename, Map<TrackProxy, Container> dashedFiles) throws IOException {
        Map<TrackProxy, List<File>> track2Files = new HashMap<TrackProxy, List<File>>();
        for (Map.Entry<TrackProxy, Container> trackContainerEntry : dashedFiles.entrySet()) {
            l.info("Writing... ");
            TrackProxy t = trackContainerEntry.getKey();
            File f = new File(outputDirectory, trackFilename.get(t));
            WritableByteChannel wbc = new FileOutputStream(f).getChannel();
            try {
                List<Box> boxes = trackContainerEntry.getValue().getBoxes();
                for (int i = 0; i < boxes.size(); i++) {
                    l.finest("Writing... " + boxes.get(i).getType() + " [" + i + " of " + boxes.size() + "]");
                    boxes.get(i).getBox(wbc);
                }

            } finally {
                wbc.close();
            }
            l.info("Done.");
            track2Files.put(t, Collections.singletonList(f));
        }
        return track2Files;
    }

    public DashBuilder getFileBuilder(FragmentIntersectionFinder fragmentIntersectionFinder, Movie m) {
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

            l.info("Creating model for " + filename + "... ");


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
                long size = 0;
                List<Sample> samples = track.getSamples();
                for (int i = 0; i < Math.min(samples.size(), 10000); i++) {
                    size += samples.get(i).getSize();
                }
                size = (size / Math.min(track.getSamples().size(), 10000)) * track.getSamples().size();
                sizes.put(track, size);
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

                bitrates.put(track, (long) ((size * 8 / duration / 100)) * 100);
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
                    FragmentIntersectionFinder videoIntersectionFinder = new SyncSampleIntersectFinderImpl(movie, null, 4);
                    fragmentStartSamples.put(track, videoIntersectionFinder.sampleNumbers(track.getTarget()));
                    //fragmentStartSamples.put(track, checkMaxFragmentDuration(track, videoIntersectionFinder.sampleNumbers(track)));
                } else if (track.getHandler().startsWith("soun") || track.getHandler().startsWith("subt")) {
                    FragmentIntersectionFinder soundIntersectionFinder = new SoundIntersectionFinderImpl(tracks, 15);
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

        Map<TrackProxy, String> track2File = new LinkedHashMap<TrackProxy, String>();
        for (File inputFile : inputFiles) {
            if (inputFile.isFile()) {
                if (inputFile.getName().endsWith(".mp4") ||
                        inputFile.getName().endsWith(".mov") ||
                        inputFile.getName().endsWith(".ismv") ||
                        inputFile.getName().endsWith(".m4a") ||
                        inputFile.getName().endsWith(".m4v")) {
                    Movie movie = MovieCreator.build(new FileDataSourceImpl(inputFile));
                    for (Track track : movie.getTracks()) {
                        String codec = DashHelper.getFormat(track);
                        if (!supportedTypes.contains(codec)) {
                            l.warning("Excluding " + inputFile + " track " + track.getTrackMetaData().getTrackId() + " as its codec " + codec + " is not yet supported");
                            break;
                        }
                        if (track instanceof CencEncryptedTrack) {
                            l.warning("Excluding " + inputFile + " track " + track.getTrackMetaData().getTrackId() + " as it is encrypted. Encrypted source tracks are not yet supported");
                            break;
                        }
                        track2File.put(new TrackProxy(track), inputFile.getName());
                    }
                } else if (inputFile.getName().endsWith(".aac")) {
                    Track track = new AACTrackImpl(new FileDataSourceImpl(inputFile));
                    track2File.put(new TrackProxy(track), inputFile.getName());
                    l.fine("Created AAC Track from " + inputFile.getName());
                } else if (inputFile.getName().endsWith(".h264")) {
                    Track track = new H264TrackImpl(new FileDataSourceImpl(inputFile));
                    track2File.put(new TrackProxy(track), inputFile.getName());
                    l.fine("Created H264 Track from " + inputFile.getName());
                } else if (inputFile.getName().endsWith(".ac3")) {
                    Track track = new AC3TrackImpl(new FileDataSourceImpl(inputFile));
                    track2File.put(new TrackProxy(track), inputFile.getName());
                    l.fine("Created AC3 Track from " + inputFile.getName());
                } else if (inputFile.getName().endsWith(".ec3")) {
                    Track track = new EC3TrackImpl(new FileDataSourceImpl(inputFile));
                    track2File.put(new TrackProxy(track), inputFile.getName());
                    l.fine("Created EC3 Track from " + inputFile.getName());
                } else if (inputFile.getName().endsWith(".dtshd")) {
                    Track track = new DTSTrackImpl(new FileDataSourceImpl(inputFile));
                    track2File.put(new TrackProxy(track), inputFile.getName());
                    l.fine("Created DTS HD Track from " + inputFile.getName());
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
            boolean acceptEdit = true;
            boolean acceptDwell = true;
            List<Edit> edits = track.getEdits();
            double earliestTrackPresentationTime = 0;
            for (Edit edit : edits) {
                if (edit.getMediaTime() == -1 && !acceptDwell) {
                    throw new RuntimeException("Cannot accept edit list for processing (1)");
                }
                if (edit.getMediaTime() >= 0 && !acceptEdit) {
                    throw new RuntimeException("Cannot accept edit list for processing (2)");
                }
                if (edit.getMediaTime() == -1) {
                    earliestTrackPresentationTime += edit.getSegmentDuration();
                } else /* if edit.getMediaTime() >= 0 */ {
                    earliestTrackPresentationTime -= (double) edit.getMediaTime() / edit.getTimeScale();
                    acceptEdit = false;
                    acceptDwell = false;
                }
            }

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

            l.info("Adjusted earliest presentation of " + track.getName() + " from " + startTimes.get(track) + " to " + (startTimes.get(track) - earliestMoviePresentationTime));

            final List<Edit> edits = new ArrayList<Edit>();
            if (adjustedStartTime < 0) {
                edits.add(new Edit((long) (-adjustedStartTime * track.getTrackMetaData().getTimescale()), track.getTrackMetaData().getTimescale(), 1.0, (double) track.getDuration() / track.getTrackMetaData().getTimescale()));
            } else if (adjustedStartTime > 0) {
                edits.add(new Edit(-1, track.getTrackMetaData().getTimescale(), 1.0, adjustedStartTime));
                edits.add(new Edit(0, track.getTrackMetaData().getTimescale(), 1.0, (double) track.getDuration() / track.getTrackMetaData().getTimescale()));
            }
            track.setTarget(new WrappingTrack(track.getTarget()) {
                @Override
                public List<Edit> getEdits() {
                    return edits;
                }
            });
        }
    }

    public Map<String, List<TrackProxy>> findAdaptationSets(Set<TrackProxy> allTracks) throws IOException, ExitCodeException {
        HashMap<String, List<TrackProxy>> trackFamilies = new HashMap<String, List<TrackProxy>>();
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


    public Map<File, String> getSubtitleLanguages(List<File> subtitles) throws ExitCodeException, IOException {
        Map<File, String> languages = new HashMap<File, String>();

        Pattern patternFilenameIncludesLanguage = Pattern.compile(".*-([a-z][a-z])");
        for (File subtitle : subtitles) {
            String ext = FilenameUtils.getExtension(subtitle.getName());
            String basename = FilenameUtils.getBaseName(subtitle.getName());
            if (ext.equals("vtt")) {
                Matcher m = patternFilenameIncludesLanguage.matcher(basename);
                if (m.matches()) {
                    languages.put(subtitle, m.group(1));
                } else {
                    throw new ExitCodeException("Cannot determine language of " + subtitle + " please use the pattern name-[2-letter-lang].vtt", 1387);
                }
            } else if (ext.equals("xml")) {
                DocumentBuilderFactory builderFactory =
                        DocumentBuilderFactory.newInstance();
                try {
                    DocumentBuilder builder = builderFactory.newDocumentBuilder();

                    String xml = FileUtils.readFileToString(subtitle);
                    Document xmlDocument = builder.parse(new ByteArrayInputStream(xml.getBytes()));
                    String lang  = xmlDocument.getDocumentElement().getAttribute("xml:lang");
                    if (lang != null) {
                        languages.put(subtitle, lang);
                    } else {
                        Matcher m2 = patternFilenameIncludesLanguage.matcher(basename);
                        if (m2.matches()) {
                            languages.put(subtitle, m2.group(1));
                        } else {
                            throw new ExitCodeException("Cannot determine language of " + subtitle + " please use either the xml:lang attribute or a filename pattern like name-[2-letter-lang].xml", 1388);
                        }
                    }
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                    throw new ExitCodeException("Cannot instantiate XML parser to determine subtitle language", 3242);
                } catch (SAXException e) {
                    e.printStackTrace();
                    throw new ExitCodeException("Cannot parse XML to extract subtitle language", 87433);
                }





            }
        }
        return languages;
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
    }
}
