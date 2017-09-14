/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.dashfragmenter;

import com.castlabs.dash.dashfragmenter.cmdlines.*;
import org.apache.commons.io.IOUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.SubCommand;
import org.kohsuke.args4j.spi.SubCommandHandler;
import org.kohsuke.args4j.spi.SubCommands;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class Main {

    public static final String TOOL;

    @Argument(
            handler = SubCommandHandler.class,
            required = true,
            metaVar = "command",
            usage = "Command required. Supported commands are: [dash, encrypt, mux]"
    )
    @SubCommands({
            @SubCommand(name = "encrypt2", impl = Encrypt2Command.class),
            @SubCommand(name = "mux", impl = MuxMp4.class),
            @SubCommand(name = "cenc", impl = CencEncryptAndMux.class),
            @SubCommand(name = "dumpbox", impl = DumpBox.class),
            @SubCommand(name = "extractkid", impl = ExtractKeyId.class)


    })
    Command command;

    @Option(name = "-v", usage = "Verbose output when enabled")
    boolean verbose;


    public static void main(String[] args) throws Exception {
        System.out.println(TOOL);
        Main m = new Main();
        CmdLineParser parser = new CmdLineParser(m);
        try {
            parser.parseArgument(args);
            m.setupLogger();
            m.command.postProcessCmdLineArgs(new CmdLineParser(m.command));
            m.command.run();
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            e.getParser().printSingleLineUsage(System.err);
            System.err.println();
            e.getParser().printUsage(System.err);
            System.exit(1023);
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

    public Logger setupLogger() {
        Logger logger = Logger.getLogger("dash");
        InputStream stream;
        if (verbose) {
            stream = com.castlabs.dash.dashfragmenter.AbstractCommand.class.getResourceAsStream("/log-verbose.properties");
        } else {
            stream = com.castlabs.dash.dashfragmenter.AbstractCommand.class.getResourceAsStream("/log.properties");
        }
        try {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return logger;
    }


}