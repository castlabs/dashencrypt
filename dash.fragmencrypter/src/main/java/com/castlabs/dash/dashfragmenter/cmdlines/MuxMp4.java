/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.Command;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;
import org.mp4parser.Container;
import org.mp4parser.muxer.FileDataSourceImpl;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.builder.Mp4Builder;
import org.mp4parser.muxer.tracks.AACTrackImpl;
import org.mp4parser.muxer.tracks.AC3TrackImpl;
import org.mp4parser.muxer.tracks.DTSTrackImpl;
import org.mp4parser.muxer.tracks.EC3TrackImpl;
import org.mp4parser.muxer.tracks.h264.H264TrackImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.castlabs.dash.helpers.LanguageHelper.getFilesLanguage;


public class MuxMp4 implements Command {
    private static Logger LOG = Logger.getLogger(MuxMp4.class.getName());
    @Argument(required = true, multiValued = true, handler = FileOptionHandler.class, usage = "Bitstream input files. In case that an audio input format cannot convey the input's language the filename is expected to be [basename]-[lang].[ext]", metaVar = "vid1.h264, aud1.dtshd ...")
    protected List<File> inputFiles;

    @Option(name = "--outputfile", aliases = "-o",
            usage = "output file - if no output file is given output.mp4 is used",
            metaVar = "PATH")
    protected File outputFile = new File(System.getProperty("user.dir") + File.separator + "output.mp4");


    public int run() {

        long start = System.currentTimeMillis();

        List<Track> tracks = null;
        try {
            tracks = createTracks();


            // export the dashed single track MP4s
            writeMp4(tracks);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return 8279;
        }

        //System.out.println("Finished writeOnDemand in " + (System.currentTimeMillis() - start) + "ms");
        return 0;
    }

    public void postProcessCmdLineArgs(CmdLineParser cmdLineParser) throws CmdLineException {

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
                track.getTrackMetaData().setLanguage(getFilesLanguage(inputFile).getISO3Language());
                tracks.add(track);
            } else if (inputFile.getName().endsWith(".ac3")) {
                Track track = new AC3TrackImpl(new FileDataSourceImpl(inputFile));
                track.getTrackMetaData().setLanguage(getFilesLanguage(inputFile).getISO3Language());
                tracks.add(track);
            } else if (inputFile.getName().endsWith(".h264")) {
                Track track = new H264TrackImpl(new FileDataSourceImpl(inputFile));
                tracks.add(track);
            } else if (inputFile.getName().endsWith(".ec3")) {
                Track track = new EC3TrackImpl(new FileDataSourceImpl(inputFile));
                track.getTrackMetaData().setLanguage(getFilesLanguage(inputFile).getISO3Language());
                tracks.add(track);
            } else if (inputFile.getName().endsWith(".dtshd")) {
                Track track = new DTSTrackImpl(new FileDataSourceImpl(inputFile));
                track.getTrackMetaData().setLanguage(getFilesLanguage(inputFile).getISO3Language());
                tracks.add(track);
            } else {
                System.err.println("Cannot identify type of " + inputFile + ". Extensions aac, ac3, ec3 or dtshd are known.");
            }
        }

        return tracks;
    }

}