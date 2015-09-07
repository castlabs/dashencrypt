/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.AbstractCommand;
import com.castlabs.dash.dashfragmenter.ExitCodeException;
import com.castlabs.dash.dashfragmenter.sequences.DashFileSetSequence;
import com.coremedia.iso.Hex;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;


public class DashFileSetEncrypt extends AbstractEncryptOrNotCommand {


    @Option(name = "--sparse",
            aliases = "-s",
            usage = "0=encrypt everything, 1=encrypted if default, some sample clear, 2=clear is default, important samples are encrypted",
            hidden = true
    )
    protected int sparse = 0;


    @Option(name = "--clearlead",
            aliases = "-C",
            usage = "seconds of unencrypted content after start",
            hidden = true
    )
    protected int clearLead = 0;


    @Argument(required = true, multiValued = true, handler = FileOptionHandler.class, usage = "MP4 and bitstream input files. In case that an audio input format cannot convey the input's language the filename is expected to be [basename]-[lang].[ext.]", metaVar = "vid1.mp4, vid2.mp4, aud1.mp4, aud2-eng.ec3, aud3-fra.aac ...")
    protected List<File> inputFiles;

    @Option(name = "--outputdir", aliases = "-o",
            usage = "output directory - if no output directory is given the " +
                    "current working directory is used.",
            metaVar = "PATH")
    protected File outputDirectory = new File(System.getProperty("user.dir"));


    @Option(name = "--explode", aliases = "-x", usage = "If this option is set each segement will be written in a single file")
    protected boolean explode = false;

    @Option(name = "--dummyIvs", hidden = true)
    protected boolean dummyIvs = false;

    @Option(name = "--subtitles", aliases = "-st")
    protected List<File> subtitles;

    @Option(name = "--closed-captions", aliases = "-cc")
    protected List<File> closedCaptions;

    @Option(name = "--trick-mode-files", aliases = "-tmh", usage = "Add reduced framerate representations here.")
    protected List<File> trickmodefiles;


    public void postProcessCmdLineArgs(CmdLineParser cmdLineParser) throws CmdLineException {
        super.postProcessCmdLineArgs(cmdLineParser);
        for (File inputFile : inputFiles) {
            if (inputFile.getName().endsWith(".xml") || inputFile.getName().endsWith(".vtt") || inputFile.getName().endsWith(".dfxp")) {
                throw new CmdLineException(cmdLineParser, new AbstractEncryptOrNotCommand.Message("Subtitle files must either be supplied via command line option --subtitles or --closed-captions"));
            }
        }

    }

    public int run() throws IOException, ExitCodeException {
        DashFileSetSequence d = new DashFileSetSequence();
        d.setExplode(explode);
        d.setSparse(sparse);
        d.setClearlead(clearLead);
        d.setOutputDirectory(outputDirectory);
        d.setInputFiles(inputFiles);
        d.setEncryptionAlgo("cenc");
        d.setDummyIvs(dummyIvs);
        d.setTrickModeFiles(trickmodefiles);

        d.setSubtitles(subtitles);
        d.setClosedCaptions(closedCaptions);



        d.setAudioKeyid(audioKeyId);
        d.setVideoKeyid(videoKeyId);
        d.setAudioKey(audioKey);
        d.setVideoKey(videoKey);


        return d.run();
    }


}


