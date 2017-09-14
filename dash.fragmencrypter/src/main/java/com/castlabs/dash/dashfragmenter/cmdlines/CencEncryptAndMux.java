package com.castlabs.dash.dashfragmenter.cmdlines;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.FragmentedMp4Builder;
import org.mp4parser.muxer.container.mp4.MovieCreator;
import org.mp4parser.muxer.tracks.encryption.CencEncryptingTrackImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CencEncryptAndMux extends AbstractEncryptOrNotCommand {
    private static Logger LOG = Logger.getLogger(CencEncryptAndMux.class.getName());

    @Argument(required = true, handler = FileOptionHandler.class, usage = "Clear MP4 file", metaVar = "myVideo.mp4, myAudio.mp4")
    protected List<File> inputFiles;


    @Option(name = "--outputfile", aliases = "-o",
            usage = "output file - if no output file is given output.mp4 is used",
            metaVar = "PATH")
    protected File outputFile = new File(System.getProperty("user.dir") + File.separator + "output.mp4");


    public int run() {
        try {
            List<Track> tracks = new ArrayList<Track>();

            if (!(outputFile.getParentFile().exists() ^ outputFile.getParentFile().mkdirs())) {
                System.err.println("Output directory does not exist and cannot be created.");
            }

            for (File inputFile : inputFiles) {
                if (!inputFile.getName().endsWith(".mp4")) {
                    LOG.severe("Only MP4 files are supported as input.");
                    return 87263;
                }
                Movie m = MovieCreator.build(inputFile.getAbsolutePath());

                for (Track track : m.getTracks()) {
                    if ("soun".equals(track.getHandler()) && audioKeyId != null) {
                        tracks.add(new CencEncryptingTrackImpl(track, audioKeyId, audioKey, false));
                    } else if ("vide".equals(track.getHandler()) && videoKeyId != null) {
                        tracks.add(new CencEncryptingTrackImpl(track, videoKeyId, videoKey, false));
                    } else {
                        tracks.add(track);
                    }
                }

            }
            Movie nuMovie = new Movie();
            nuMovie.setTracks(tracks);

            WritableByteChannel wbc = new FileOutputStream(outputFile).getChannel();
            new FragmentedMp4Builder().build(nuMovie).writeContainer(wbc);
            wbc.close();
            return 0;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return 763;
        }
    }
}
