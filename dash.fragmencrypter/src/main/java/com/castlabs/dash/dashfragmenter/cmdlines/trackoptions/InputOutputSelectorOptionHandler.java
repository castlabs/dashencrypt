package com.castlabs.dash.dashfragmenter.cmdlines.trackoptions;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sannies on 19.03.17.
 */
public class InputOutputSelectorOptionHandler extends
        OneArgumentOptionHandler<InputOutputSelector> {

    Pattern p1 = Pattern.compile("(\\[.+\\])?([^\\[]+)(\\[.+\\])?");
    Pattern p2 = Pattern.compile("([^,\\[=]+)=([^,\\]]+)+");

    public InputOutputSelectorOptionHandler(CmdLineParser parser, OptionDef option,
                                             Setter<? super InputOutputSelector> setter) {
        super(parser, option, setter);
    }

    @Override
    protected InputOutputSelector parse(String argument) throws CmdLineException {
        try {
            Matcher m1 = p1.matcher(argument);
            if (m1.matches()) {
                Map<String, String> in = new HashMap<>();
                Map<String, String> out = new HashMap<>();
                if (m1.group(1) != null) {
                    Matcher m2 = p2.matcher(m1.group(1));
                    while (m2.find()) {
                        in.put(m2.group(1).trim(), m2.group(2).trim());
                    }
                }
                if (m1.group(3) != null) {
                    Matcher m2 = p2.matcher(m1.group(3));
                    while (m2.find()) {
                        out.put(m2.group(1).trim(), m2.group(2).trim());
                    }
                }

                return new InputOutputSelector(in, m1.group(2), out);
            } else {
                throw new CmdLineException(owner,
                        argument + " is not valid. Format is [in-prop=value]filename[out-prop=value]");
            }
        } catch (IOException e) {
            throw new CmdLineException(owner,
                    e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new CmdLineException(owner,
                    e.getMessage());
        }
    }


    @Override
    public String getDefaultMetaVariable() {
        return "[in-prop=value]filename[out-prop=value]";
    }
}
