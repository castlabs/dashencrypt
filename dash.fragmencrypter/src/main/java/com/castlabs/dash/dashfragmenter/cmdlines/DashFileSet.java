/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.AbstractCommand;
import com.castlabs.dash.dashfragmenter.sequences.DashFileSetSequence;
import com.castlabs.dash.dashfragmenter.FileAndTrackSelector;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DashFileSet extends AbstractCommand {


    @Argument(required = true, multiValued = true, handler = FileAndTrackSelectorOptionHandler.class, usage = "MP4 and bitstream input files. In case that an audio input format cannot convey the input's language the filename is expected to be [basename]-[lang].[ext]", metaVar = "vid1.mp4, vid2.mp4, aud1.mp4, aud2-eng.ec3, aud3-fra.aac ...")
    protected List<FileAndTrackSelector> inputFiles;

    @Option(name = "--outputdir", aliases = "-o",
            usage = "output directory - if no output directory is given the " +
                    "current working directory is used.",
            metaVar = "PATH")
    protected File outputDirectory = new File(System.getProperty("user.dir"));

    @Option(name = "--subtitles", aliases = "-st", usage = ".xml, .dfxp and .vtt are supported")
    protected List<File> subtitles;

    @Option(name = "--closed-captions", aliases = "-cc", usage = ".xml, .dfxp and .vtt are supported")
    protected List<File> closedCaptions;


    @Option(name = "--live-profile", aliases = "-x", usage = "If this option is set each segment will be written in a single file")
    protected boolean explode = false;

    @Option(name = "--trick-mode-files", aliases = "-tmh", usage = "Add reduced framerate representations here.")
    protected List<File> trickmodefiles;

    @Option(name = "--language-map", usage = "Adds a language mapping oldlang=newlang")
    protected Map<String,String> languageMap = new HashMap<>();

    public void postProcessCmdLineArgs(CmdLineParser cmdLineParser) throws CmdLineException {
        for (FileAndTrackSelector inputFile : inputFiles) {
            if (inputFile.file.getName().endsWith(".xml") || inputFile.file.getName().endsWith(".vtt") || inputFile.file.getName().endsWith(".dfxp")) {
                throw new CmdLineException(cmdLineParser, new AbstractEncryptOrNotCommand.Message("Subtitle files must either be supplied via command line option --subtitles or --closed-captions"));
            }
        }
    }

    public int run()  {
        DashFileSetSequence dashFileSetSequence = new DashFileSetSequence();
        dashFileSetSequence.setExplode(explode);
        dashFileSetSequence.setOutputDirectory(outputDirectory);
        dashFileSetSequence.setInputFiles(inputFiles);
        dashFileSetSequence.setSubtitles(subtitles);
        dashFileSetSequence.setTrickModeFiles(trickmodefiles);
        dashFileSetSequence.setClosedCaptions(closedCaptions);
        dashFileSetSequence.setLanguageMap(languageMap);
        return dashFileSetSequence.run();
    }
}