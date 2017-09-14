package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.Command;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.spi.FileOptionHandler;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso23001.part7.AbstractTrackEncryptionBox;
import org.mp4parser.tools.Path;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by sannies on 22.01.2016.
 */
public class ExtractKeyId implements Command {

    @Argument(required = true, multiValued = false, handler = FileOptionHandler.class, usage = "MP4 and bitstream input files. In case that an audio input format cannot convey the input's language the filename is expected to be [basename]-[lang].[ext]", metaVar = "vid1.mp4, vid2.mp4, aud1.mp4, aud2-eng.ec3, aud3-fra.aac ...")
    protected File inputFile;



    public int run() {

        try (IsoFile isoFile = new IsoFile(inputFile)) {
            List<AbstractTrackEncryptionBox> cenc = Path.getPaths(isoFile, "/moov[0]/trak/mdia[0]/minf[0]/stbl[0]/stsd[0]/enc.[0]/sinf[0]/schi[0]/tenc[0]");
            for (AbstractTrackEncryptionBox trackEncryptionBox : cenc) {
                System.err.println(trackEncryptionBox.getDefault_KID());
            }
            List<AbstractTrackEncryptionBox> piff = Path.getPaths(isoFile, "/moov[0]/trak/mdia[0]/minf[0]/stbl[0]/stsd[0]/enc.[0]/sinf[0]/schi[0]/uuid[0]");
            for (AbstractTrackEncryptionBox trackEncryptionBox : piff) {
                System.err.println(trackEncryptionBox.getDefault_KID());
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
