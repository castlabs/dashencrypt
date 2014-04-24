/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.dashfragmenter.mp4todash;

import com.castlabs.dash.dashfragmenter.Command;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.*;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.AC3TrackImpl;
import com.googlecode.mp4parser.authoring.tracks.DTSTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.EC3TrackImpl;
import com.googlecode.mp4parser.boxes.mp4.ESDescriptorBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.AudioSpecificConfig;
import mpegDashSchemaMpd2011.MPDDocument;
import org.apache.xmlbeans.XmlOptions;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.*;


public class DashFileSet implements Command {
    static Set<String> supportedTypes = new HashSet<String>(Arrays.asList("ac-3", "ec-3", "dtsl", "dtsh", "dtse", "avc1", "mp4a"));

    @Argument(required = true, multiValued = true, handler = FileOptionHandler.class, usage = "MP4 and bitstream input files", metaVar = "vid1.mp4, vid2.mp4, aud1.mp4, aud2.ec3 ...")
    protected List<File> inputFiles;

    @Option(name = "--outputdir", aliases = "-o",
            usage = "output directory - if no output directory is given the " +
                    "current working directory is used.",
            metaVar = "PATH")
    protected File outputDirectory = new File("");


    @Override
    public int run() throws IOException {
        if (!(outputDirectory.exists() ^ outputDirectory.mkdirs())) {
            System.err.println("Output directory does not exist and cannot be created.");
        }

        long start = System.currentTimeMillis();

        Map<Track, String> trackOriginalFilename = createTracks();

        // sort by language and codec
        Map<String, List<Track>> trackFamilies = findTrackFamilies(trackOriginalFilename.keySet());

        // Track sizes are expensive to calculate -> save them for later
        Map<Track, Long> trackSizes = calculateTrackSizes(trackFamilies);

        // sort within the track families by size to get stable output
        sortTrackFamilies(trackFamilies, trackSizes);

        // calculate the fragment start samples once & save them for later
        Map<Track, long[]> trackStartSamples = findFragmentStartSamples(trackFamilies);

        // calculate bitrates
        Map<Track, Long> trackBitrate = calculateBitrate(trackFamilies, trackSizes);

        // generate filenames for later reference
        Map<Track, String> trackFilename = generateFilenames(trackOriginalFilename);

        // export the dashed single track MP4s
        Map<Track, Container> dashedFiles = writeSingleTrackDashedMp4s(trackStartSamples, trackFilename);

        writeManifest(trackFamilies, trackBitrate, trackFilename, dashedFiles);

        System.out.println("Finished write in " + (System.currentTimeMillis() - start) + "ms");
        return 0;
    }

    protected void writeManifest(Map<String, List<Track>> trackFamilies,
                                 Map<Track, Long> trackBitrate,
                                 Map<Track, String> trackFilename,
                                 Map<Track, Container> dashedFiles) throws IOException {
        MPDDocument mpdDocument1 =
                new SegmentBaseSingleSidxManifestWriterImpl(trackFamilies, dashedFiles, trackBitrate,
                        trackFilename, Collections.<Track, UUID>emptyMap()).getManifest();

        MPDDocument mpdDocument2 =
                new SegmentListManifestWriterImpl(trackFamilies, dashedFiles, trackBitrate,
                        trackFilename, Collections.<Track, UUID>emptyMap()).getManifest();

        XmlOptions xmlOptions = new XmlOptions();
        //xmlOptions.setUseDefaultNamespace();
        HashMap<String, String> ns = new HashMap<String, String>();
        //ns.put("urn:mpeg:DASH:schema:MPD:2011", "");
        ns.put("urn:mpeg:cenc:2013", "cenc");
        xmlOptions.setSaveSuggestedPrefixes(ns);
        xmlOptions.setSaveAggressiveNamespaces();
        xmlOptions.setUseDefaultNamespace();
        xmlOptions.setSavePrettyPrint();
        File manifest1 = new File(outputDirectory, "Manifest.mpd");
        System.out.print("Writing " + manifest1 + "... ");
        mpdDocument1.save(manifest1, xmlOptions);
        System.out.println("Done.");
        File manifest2 = new File(outputDirectory, "Manifest-segment-list.mpd");
        System.out.print("Writing " + manifest2 + "... ");
        mpdDocument2.save(manifest2, xmlOptions);
        System.out.println("Done.");

    }

    Mp4Builder getFileBuilder(FragmentIntersectionFinder fragmentIntersectionFinder, Movie m) {
        DashBuilder dashBuilder = new DashBuilder();
        dashBuilder.setIntersectionFinder(fragmentIntersectionFinder);
        return dashBuilder;
    }

    private HashMap<Track, Container> writeSingleTrackDashedMp4s(
            Map<Track, long[]> fragmentStartSamples,
            Map<Track, String> filenames) throws IOException {

        HashMap<Track, Container> containers = new HashMap<Track, Container>();
        for (final Map.Entry<Track, long[]> trackEntry : fragmentStartSamples.entrySet()) {
            String filename = filenames.get(trackEntry.getKey());
            Movie movie = new Movie();
            movie.addTrack(trackEntry.getKey());

            System.out.print("Creating model for " + filename + "... ");
            Mp4Builder mp4Builder = getFileBuilder(
                    new StaticFragmentIntersectionFinderImpl(fragmentStartSamples),
                    movie);
            Container isoFile = mp4Builder.build(movie);

            System.out.print("Writing... ");
            WritableByteChannel wbc = new FileOutputStream(
                    new File(outputDirectory, filename)).getChannel();
            try {
                isoFile.writeContainer(wbc);
                containers.put(trackEntry.getKey(), isoFile);
            } finally {
                wbc.close();
            }
            System.out.println("Done.");
        }
        return containers;
    }

    private void sortTrackFamilies(Map<String, List<Track>> trackFamilies, final Map<Track, Long> sizes) {
        for (List<Track> tracks : trackFamilies.values()) {
            Collections.sort(tracks, new Comparator<Track>() {
                @Override
                public int compare(Track o1, Track o2) {
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
    private Map<Track, Long> calculateTrackSizes(Map<String, List<Track>> trackFamilies) {
        HashMap<Track, Long> sizes = new HashMap<Track, Long>();
        for (List<Track> tracks : trackFamilies.values()) {
            for (Track track : tracks) {
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
    private Map<Track, Long> calculateBitrate(Map<String, List<Track>> trackFamilies, Map<Track, Long> trackSize) {
        HashMap<Track, Long> bitrates = new HashMap<Track, Long>();
        for (List<Track> tracks : trackFamilies.values()) {
            for (Track track : tracks) {

                double duration = (double) track.getDuration() / track.getTrackMetaData().getTimescale();
                long size = trackSize.get(track);

                bitrates.put(track, (long) ((size * 8 / duration / 1000)) * 1000);
            }

        }
        return bitrates;
    }

    /**
     * Generates filenames from type, language and bitrate.
     *
     * @return a descriptive filename <code>type[-lang]-bitrate.mp4</code>
     */
    private Map<Track, String> generateFilenames(Map<Track, String> trackOriginalFilename) {
        HashMap<Track, String> filenames = new HashMap<Track, String>();
        for (Track track : trackOriginalFilename.keySet()) {
            String originalFilename = trackOriginalFilename.get(track);
            originalFilename = originalFilename.replace(".mp4", "");
            originalFilename = originalFilename.replace(".aac", "");
            originalFilename = originalFilename.replace(".ec3", "");
            originalFilename = originalFilename.replace(".ac3", "");
            originalFilename = originalFilename.replace(".dtshd", "");
            for (Track track1 : filenames.keySet()) {
                if (track1 != track &&
                        trackOriginalFilename.get(track1).equals(trackOriginalFilename.get(track))) {
                    // ouch multiple tracks point to same file
                    originalFilename += "_" + track.getTrackMetaData().getTrackId();
                }
            }
            filenames.put(track, String.format("%s.mp4", originalFilename));

        }
        return filenames;
    }

    Map<Track, long[]> findFragmentStartSamples(Map<String, List<Track>> trackFamilies) {
        Map<Track, long[]> fragmentStartSamples = new HashMap<Track, long[]>();

        for (String key : trackFamilies.keySet()) {
            List<Track> tracks = trackFamilies.get(key);
            Movie movie = new Movie();
            movie.setTracks(tracks);
            for (Track track : tracks) {
                if (track.getHandler().startsWith("vide")) {
                    FragmentIntersectionFinder videoIntersectionFinder = new SyncSampleIntersectFinderImpl(movie, null, 2);
                    fragmentStartSamples.put(track, videoIntersectionFinder.sampleNumbers(track));
                    //fragmentStartSamples.put(track, checkMaxFragmentDuration(track, videoIntersectionFinder.sampleNumbers(track)));
                } else if (track.getHandler().startsWith("soun")) {
                    FragmentIntersectionFinder soundIntersectionFinder = new TwoSecondIntersectionFinder(movie, 5);
                    fragmentStartSamples.put(track, soundIntersectionFinder.sampleNumbers(track));
                } else {
                    throw new RuntimeException("An engineer needs to tell me if " + key + " is audio or video!");
                }
            }
        }
        return fragmentStartSamples;
    }

    private long[] checkMaxFragmentDuration(Track track, long[] sampleNumbers) {
        ArrayList<Long> tweakedFragmentStartSampleNumbers = new ArrayList<Long>(sampleNumbers.length);
        for (int i = 0; i < sampleNumbers.length - 1; i++) {
            long startSampleNum = sampleNumbers[i];
            long endSampleNum = sampleNumbers[i + 1];
            long fragmentDuration = getFragmentDuration(track, startSampleNum, endSampleNum);
            tweakedFragmentStartSampleNumbers.add(sampleNumbers[i]);
            final long timescale = track.getTrackMetaData().getTimescale();
            if (fragmentDuration > 10 * timescale) {
                System.err.println("Warning: fragment " + i + 1 + " has a duration of >10 sec (" + fragmentDuration / timescale + ")");
//                //insert fragment start sample in between the original fragment start samples
                tweakedFragmentStartSampleNumbers.add(sampleNumbers[i] +
                        (sampleNumbers[i + 1] - sampleNumbers[i]) / 2);
            }
        }
        long[] returnvalue = new long[tweakedFragmentStartSampleNumbers.size()];
        for (int i = 0; i < tweakedFragmentStartSampleNumbers.size(); i++) {
            Long sampleNumber = tweakedFragmentStartSampleNumbers.get(i);
            returnvalue[i] = sampleNumber;
        }
        return returnvalue;
    }

    private long getFragmentDuration(Track track, long startSampleNum, long endSampleNum) {
        final long[] sampleDurations = track.getSampleDurations();
        long fragmentDuration = 0;
        for (int i = (int) startSampleNum - 1; i < endSampleNum - 1; i++) {
            fragmentDuration += sampleDurations[i];
        }
        return fragmentDuration;
    }

    /**
     * Creates a Map with Track as key and originating filename as value.
     *
     * @return Track too originating file map
     * @throws IOException
     */
    Map<Track, String> createTracks() throws IOException {
        HashMap<Track, String> track2File = new HashMap<Track, String>();
        for (File inputFile : inputFiles) {
            if (inputFile.getName().endsWith("mp4")) {
                Movie movie = MovieCreator.build(new FileDataSourceImpl(inputFile));
                for (Track track : movie.getTracks()) {
                    String codec = track.getSampleDescriptionBox().getSampleEntry().getType();
                    if (!supportedTypes.contains(codec)) {
                        System.out.println("Excluding " + inputFile + " track " + track.getTrackMetaData().getTrackId() + " as its codec " + codec + " is not yet supported");
                        break;
                    }
                    track2File.put(track, inputFile.getName());
                }
            } else if (inputFile.getName().endsWith(".aac")) {
                Track track = new AACTrackImpl(new FileDataSourceImpl(inputFile));
                track2File.put(track, inputFile.getName());
            } else if (inputFile.getName().endsWith(".ac3")) {
                Track track = new AC3TrackImpl(new FileDataSourceImpl(inputFile));
                track2File.put(track, inputFile.getName());
            } else if (inputFile.getName().endsWith(".ec3")) {
                Track track = new EC3TrackImpl(new FileDataSourceImpl(inputFile));
                track2File.put(track, inputFile.getName());
            } else if (inputFile.getName().endsWith(".dtshd")) {
                Track track = new DTSTrackImpl(new FileDataSourceImpl(inputFile));
                track2File.put(track, inputFile.getName());
            } else {
                System.err.println("Cannot identify type of " + inputFile + ". Extensions mp4, aac, ac3, ec3 or dtshd are known.");
            }
        }

        return track2File;

    }

    Map<String, List<Track>> findTrackFamilies(Set<Track> allTracks) throws IOException {
        HashMap<String, List<Track>> trackFamilies = new HashMap<String, List<Track>>();
        for (Track track : allTracks) {
            String family = track.getSampleDescriptionBox().getSampleEntry().getType() + "-" + track.getTrackMetaData().getLanguage();
            if ("mp4a".equals(track.getSampleDescriptionBox().getSampleEntry().getType())) {
                // we need to look at actual channel configuration
                ESDescriptorBox esds = track.getSampleDescriptionBox().getSampleEntry().getBoxes(ESDescriptorBox.class).get(0);
                AudioSpecificConfig audioSpecificConfig = esds.getEsDescriptor().getDecoderConfigDescriptor().getAudioSpecificInfo();
                family += "-" + audioSpecificConfig.getChannelConfiguration();
            }

            List<Track> tracks = trackFamilies.get(family);
            if (tracks == null) {
                tracks = new LinkedList<Track>();
                trackFamilies.put(family, tracks);
            }
            tracks.add(track);
        }

        return trackFamilies;
    }


}