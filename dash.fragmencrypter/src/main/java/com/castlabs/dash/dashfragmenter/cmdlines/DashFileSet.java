/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.AbstractCommand;
import com.castlabs.dash.dashfragmenter.ExitCodeException;
import com.castlabs.dash.dashfragmenter.sequences.DashFileSetSequence;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;

import java.io.*;
import java.util.*;


public class DashFileSet extends AbstractCommand {


    @Argument(required = true, multiValued = true, handler = FileOptionHandler.class, usage = "MP4 and bitstream input files", metaVar = "vid1.mp4, vid2.mp4, aud1.mp4, aud2.ec3 ...")
    protected List<File> inputFiles;

    @Option(name = "--outputdir", aliases = "-o",
            usage = "output directory - if no output directory is given the " +
                    "current working directory is used.",
            metaVar = "PATH")
    protected File outputDirectory = new File("");

    @Option(name = "--subtitles", aliases = "-st", usage = ".xml, .dfxp and .vtt are supported")
    protected List<File> subtitles;

    @Option(name = "--closed-captions", aliases = "-cc", usage = ".xml, .dfxp and .vtt are supported")
    protected List<File> closedCaptions;


    @Option(name = "--live-profile", aliases = "-x", usage = "If this option is set each segment will be written in a single file")
    protected boolean explode = false;

    @Option(name = "--trick-mode-files", aliases = "-tmh", usage = "Add reduced framerate representations here.")
    protected List<File> trickmodefiles;

    public void postProcessCmdLineArgs(CmdLineParser cmdLineParser) throws CmdLineException {
        for (File inputFile : inputFiles) {
            if (inputFile.getName().endsWith(".xml") || inputFile.getName().endsWith(".vtt") || inputFile.getName().endsWith(".dfxp")) {
                throw new CmdLineException(cmdLineParser, new AbstractEncryptOrNotCommand.Message("Subtitle files must either be supplied via command line option --subtitles or --closed-captions"));
            }
        }
    }

    public int run() throws IOException, ExitCodeException {
        DashFileSetSequence dashFileSetSequence = new DashFileSetSequence();
        dashFileSetSequence.setExplode(explode);
        dashFileSetSequence.setLogger(setupLogger());
        dashFileSetSequence.setOutputDirectory(outputDirectory);
        dashFileSetSequence.setInputFiles(inputFiles);
        dashFileSetSequence.setSubtitles(subtitles);
        dashFileSetSequence.setTrickModeFiles(trickmodefiles);
        dashFileSetSequence.setClosedCaptions(closedCaptions);
        return dashFileSetSequence.run();
    }
}