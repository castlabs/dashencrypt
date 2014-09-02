package com.castlabs.dash.dashfragmenter;

/**
* Created by sannies on 01.09.2014.
*/
public class ExitCodeException extends Exception {
    int exitCode;

    public ExitCodeException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
