package com.castlabs.dash.helpers;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.lang.String;import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sannies on 04.09.2015.
 */
public class LanguageHelper {
    public static Locale getFilesLanguage(File f) throws IOException {
        Pattern patternFilenameIncludesLanguage = Pattern.compile(".*[-_](.+)$");
        String ext = FilenameUtils.getExtension(f.getName());
        String basename = FilenameUtils.getBaseName(f.getName());
        Matcher m = patternFilenameIncludesLanguage.matcher(basename);
        if (m.matches()) {
            return Locale.forLanguageTag(m.group(1));
        } else {
            throw new IOException("Cannot determine language of " + f + " please use the pattern filename-[language-tag].[ext]");
        }

    }
}
