package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.Command;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;
import org.mp4parser.Box;
import org.mp4parser.IsoFile;
import org.mp4parser.tools.Hex;
import org.mp4parser.tools.Path;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.List;

/**
 * Created by sannies on 22.01.2016.
 */
public class DumpBox implements Command {

    @Argument(required = true, multiValued = false, handler = FileOptionHandler.class, usage = "MP4 and bitstream input files. In case that an audio input format cannot convey the input's language the filename is expected to be [basename]-[lang].[ext]", metaVar = "vid1.mp4, vid2.mp4, aud1.mp4, aud2-eng.ec3, aud3-fra.aac ...")
    protected File inputFile;

    @Option(name = "--path", required = true)
    protected String path;


    public int run() {
        try (IsoFile isoFile = new IsoFile(inputFile)){
            List<Box> boxes = Path.getPaths(isoFile, path);
            for (Box box : boxes) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                box.getBox(Channels.newChannel(baos));
                System.out.println(Hex.encodeHex(baos.toByteArray()));
                System.out.println();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return 6482;
        }
        return 0;
    }

    public void postProcessCmdLineArgs(CmdLineParser cmdLineParser) throws CmdLineException {

    }
}
