package com.castlabs.dash.dashfragmenter.mp4todash;

import com.castlabs.dash.dashfragmenter.Command;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.builder.Mp4Builder;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.AC3TrackImpl;
import com.googlecode.mp4parser.authoring.tracks.DTSTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.EC3TrackImpl;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MuxMp4 implements Command {
    @Argument(required = true, multiValued = true, handler = FileOptionHandler.class, usage = "Bitstream input files", metaVar = "vid1.avc1, aud1.dtshd ...")
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

        // generate filenames for later reference
        Map<Track, String> trackFilename = generateFilenames(trackOriginalFilename);

        // export the dashed single track MP4s
        Map<Track, Container> mp4Files = writeSingleTrackMp4s(trackFilename);

        System.out.println("Finished write in " + (System.currentTimeMillis() - start) + "ms");
        return 0;
    }

    Mp4Builder getFileBuilder(Movie m) {
        return new DefaultMp4Builder();
    }

    private HashMap<Track, Container> writeSingleTrackMp4s(Map<Track, String> filenames) throws IOException {

        HashMap<Track, Container> containers = new HashMap<Track, Container>();
        for (Track track : filenames.keySet()) {
            String filename = filenames.get(track);
            Movie movie = new Movie();
            movie.addTrack(track);

            System.out.print("Creating model for " + filename + "... ");
            Mp4Builder mp4Builder = getFileBuilder(movie);
            Container isoFile = mp4Builder.build(movie);

            System.out.print("Writing... ");
            WritableByteChannel wbc = new FileOutputStream(
                    new File(outputDirectory, filename)).getChannel();
            try {
                isoFile.writeContainer(wbc);
                containers.put(track, isoFile);
            } finally {
                wbc.close();
            }
            System.out.println("Done.");
        }
        return containers;
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
            originalFilename = originalFilename.replace(".aac", "");
            originalFilename = originalFilename.replace(".ec3", "");
            originalFilename = originalFilename.replace(".ac3", "");
            originalFilename = originalFilename.replace(".dtshd", "");
            filenames.put(track, String.format("%s.mp4", originalFilename));
        }
        return filenames;
    }

    /**
     * Creates a Map with Track as key and originating filename as value.
     *
     * @return Track too originating file map
     * @throws java.io.IOException
     */
    Map<Track, String> createTracks() throws IOException {
        HashMap<Track, String> track2File = new HashMap<Track, String>();
        for (File inputFile : inputFiles) {
            if (inputFile.getName().endsWith(".aac")) {
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
                System.err.println("Cannot identify type of " + inputFile + ". Extensions aac, ac3, ec3 or dtshd are known.");
            }
        }

        return track2File;
    }

}