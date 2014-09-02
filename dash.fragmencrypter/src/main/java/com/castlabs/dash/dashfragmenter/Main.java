/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.dashfragmenter;

import com.castlabs.dash.dashfragmenter.cmdlines.DashFileSet;
import com.castlabs.dash.dashfragmenter.cmdlines.DashFileSetEncrypt;
import com.castlabs.dash.dashfragmenter.cmdlines.MuxMp4;
import org.apache.commons.io.IOUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.spi.SubCommand;
import org.kohsuke.args4j.spi.SubCommandHandler;
import org.kohsuke.args4j.spi.SubCommands;

import java.io.IOException;


public class Main {

    public static final String TOOL;

    @Argument(
            handler = SubCommandHandler.class,
            required = true,
            metaVar = "command",
            usage = "Command required. Supported commands are: [dash, encrypt, mux]"
    )
    @SubCommands({
            @SubCommand(name = "dash", impl = DashFileSet.class),
            @SubCommand(name = "encrypt", impl = DashFileSetEncrypt.class),
            @SubCommand(name = "mux", impl = MuxMp4.class)
    })
    Command command;

    public static void main(String[] args) throws Exception {
        System.out.println(TOOL);
        Main m = new Main();
        CmdLineParser parser = new CmdLineParser(m);
        try {
            parser.parseArgument(args);
            m.command.run();
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.exit(1023);
        } catch (ExitCodeException e) {
            System.err.println(e.getMessage());
            System.exit(e.getExitCode());
        }

    }


    static {
        String tool;
        try {
            tool = IOUtils.toString(Main.class.getResourceAsStream("/tool.txt"));
        } catch (IOException e) {
            tool = "Could not determine version";
        }
        TOOL = tool;
    }

}