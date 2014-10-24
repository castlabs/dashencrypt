/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.Command;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.builder.Mp4Builder;
import com.googlecode.mp4parser.authoring.tracks.*;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class MuxMp4 implements Command {
    @Argument(required = true, multiValued = true, handler = FileOptionHandler.class, usage = "Bitstream input files", metaVar = "vid1.avc1, aud1.dtshd ...")
    protected List<File> inputFiles;

    @Option(name = "--outputfile", aliases = "-o",
            usage = "output file - if no output file is given output.mp4 is used",
            metaVar = "PATH")
    protected File outputFile = new File("output.mp4");


    public int run() throws IOException {

        long start = System.currentTimeMillis();

        List<Track> tracks = createTracks();


        // export the dashed single track MP4s
        writeMp4(tracks);

        System.out.println("Finished write in " + (System.currentTimeMillis() - start) + "ms");
        return 0;
    }

    Mp4Builder getFileBuilder(Movie m) {
        return new DefaultMp4Builder();
    }

    private void writeMp4(List<Track> tracks) throws IOException {
        Movie m = new Movie();
        m.setTracks(tracks);

        Mp4Builder mp4Builder = getFileBuilder(m);
        Container isoFile = mp4Builder.build(m);

        System.out.print("Writing t ");
        WritableByteChannel wbc = new FileOutputStream(outputFile).getChannel();
        try {
           isoFile.writeContainer(wbc);

        } finally {
            wbc.close();
        }
        System.out.println("Done.");
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
            originalFilename = originalFilename.replace(".h264", "");
            originalFilename = originalFilename.replace(".ac3", "");
            originalFilename = originalFilename.replace(".dtshd", "");
            filenames.put(track, String.format("%s.mp4", originalFilename));
        }
        return filenames;
    }

    /**
     * Creates a List of Track
     *
     * @throws java.io.IOException
     */
    List<Track> createTracks() throws IOException {
        List<Track> tracks = new LinkedList<Track>();
        for (File inputFile : inputFiles) {
            if (inputFile.getName().endsWith(".aac")) {
                Track track = new AACTrackImpl(new FileDataSourceImpl(inputFile));
                tracks.add(track);
            } else if (inputFile.getName().endsWith(".ac3")) {
                Track track = new AC3TrackImpl(new FileDataSourceImpl(inputFile));
                tracks.add(track);
            } else if (inputFile.getName().endsWith(".h264")) {
                Track track = new H264TrackImpl(new FileDataSourceImpl(inputFile));
                tracks.add(track);
            } else if (inputFile.getName().endsWith(".ec3")) {
                Track track = new EC3TrackImpl(new FileDataSourceImpl(inputFile));
                tracks.add(track);
            } else if (inputFile.getName().endsWith(".dtshd")) {
                Track track = new DTSTrackImpl(new FileDataSourceImpl(inputFile));
                tracks.add(track);
            } else {
                System.err.println("Cannot identify type of " + inputFile + ". Extensions aac, ac3, ec3 or dtshd are known.");
            }
        }

        return tracks;
    }

}