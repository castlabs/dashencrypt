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


    @Option(name = "--explode", aliases = "-x", usage = "If this option is set each segement will be written in a single file")
    protected boolean explode = false;


    public int run() throws IOException, ExitCodeException {
        DashFileSetSequence dashFileSetSequence = new DashFileSetSequence();
        dashFileSetSequence.setExplode(explode);
        dashFileSetSequence.setLogger(setupLogger());
        dashFileSetSequence.setOutputDirectory(outputDirectory);
        dashFileSetSequence.setInputFiles(inputFiles);

        return dashFileSetSequence.run();
    }
}