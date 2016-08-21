package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.FileAndTrackSelector;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileAndTrackSelectorOptionHandler extends
        OneArgumentOptionHandler<FileAndTrackSelector> {

    Pattern p1 = Pattern.compile("(\\[.+\\])?(.+)");
    Pattern p2 = Pattern.compile(" *(track|language|type|\\w+) *= *(\\w+) *");

    public FileAndTrackSelectorOptionHandler(CmdLineParser parser, OptionDef option,
                                             Setter<? super FileAndTrackSelector> setter) {
        super(parser, option, setter);
    }

    @Override
    protected FileAndTrackSelector parse(String argument) throws CmdLineException {
        try {
            Matcher m1 = p1.matcher(argument);
            FileAndTrackSelector fats = new FileAndTrackSelector();
            if (m1.matches()) {

                if (m1.group(1) != null) {
                    Matcher m2 = p2.matcher(m1.group(1));
                    while (m2.find()) {
                        if ("track".equals(m2.group(1))) {
                            fats.trackId = Integer.parseInt(m2.group(2));
                        } else if ("language".equals(m2.group(1))) {
                            fats.language = m2.group(2);
                        } else if ("type".equals(m2.group(1))) {
                            fats.type = m2.group(2);
                        } else {
                            throw new CmdLineException(owner,
                                    argument + " is not valid. The selector contains the key " + m2.group(1) + " but only track, language or type is allowed.");
                        }
                    }
                }
                fats.file = new File(m1.group(2));
                if (!fats.file.exists()) {
                    throw new CmdLineException(owner,
                            argument + " is not valid. The given file " + fats.file + " does not exist.");

                }
                return fats;
            } else {
                throw new CmdLineException(owner,
                        argument + " is not valid. It not [...]filename");
            }
        } catch (IllegalArgumentException e) {
            throw new CmdLineException(owner,
                    Messages.ILLEGAL_UUID, argument);
        }
    }


    @Override
    public String getDefaultMetaVariable() {
        return "[property=value]filename";
    }
}

