package com.castlabs.dash.dashfragmenter;

import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Most basic Command to handle the log setup.
 */
public abstract class AbstractCommand implements Command {

    @Option(name = "--verbose", usage = "use switch to produce log output")
    protected boolean verbose = false;


    public Logger setupLogger() {
        Logger logger = Logger.getLogger("dash");
        InputStream stream;
        if (verbose) {
            stream = AbstractCommand.class.getResourceAsStream("/log-verbose.properties");
        } else {
            stream = AbstractCommand.class.getResourceAsStream("/log.properties");
        }
        try {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.setLevel(Level.FINE);
        logger.addHandler(new java.util.logging.ConsoleHandler());
        logger.setUseParentHandlers(false);

        return logger;
    }
}
