package com.castlabs.dash.helpers;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

public class FileHelpers {
    private static final Logger LOG = Logger.getLogger(FileHelpers.class.getName());

    public static boolean isMp4(File f) {
        try {
            FileInputStream fis = new FileInputStream(f);
            try {
                fis.skip(4);
                boolean a = fis.read() == 'f';
                boolean b = fis.read() == 't';
                boolean c = fis.read() == 'y';
                boolean d = fis.read() == 'p';
                boolean isMP4 = a && b && c && d;
                if (!isMP4) {
                    LOG.fine(f.getAbsolutePath() + " is no MP4 file. Byte 4-8 are NOT 'f', 't', 'y', 'p'.");
                }
                return isMP4;
            } catch (IOException e) {
                LOG.fine("Exception when probing " + f.getAbsolutePath() + ". Cannot determine if MP4 or not.");
                return false;
            } finally {
                IOUtils.closeQuietly(fis);
            }

        } catch (IOException e) {
            LOG.fine("Exception when opening" + f.getAbsolutePath() + ". Cannot determine if MP4 or not.");
            return false;
        }
    }
}
