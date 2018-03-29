/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.helpers;

import org.apache.commons.lang.math.Fraction;
import org.mp4parser.Box;
import org.mp4parser.Container;
import org.mp4parser.boxes.iso14496.part12.SegmentIndexBox;
import org.mp4parser.tools.Path;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some conversion from Track representation to Manifest specifics shared by DASH manifests of all kinds.
 */
public class ManifestHelper {

    public static String convertFramerate(double vrate) {
        Fraction f1 = Fraction.getFraction((int) (vrate * 1001), 1001);
        Fraction f2 = Fraction.getFraction((int) (vrate * 1000), 1000);
        double d1 = Math.abs(f1.doubleValue() - vrate);
        double d2 = Math.abs(f2.doubleValue() - vrate);
        if (d1 < d2) {
            return f1.getNumerator() + "/" + f1.getDenominator();
        } else {
            return f2.getNumerator() + "/" + f2.getDenominator();
        }


    }

    public static String calculateIndexRange(Container isoFile) throws IOException {
        SegmentIndexBox sidx = Path.getPath(isoFile, "/sidx");
        long start = 0;
        for (Box box : isoFile.getBoxes()) {
            if (box == sidx) {
                break;
            } else {
                start += box.getSize();
            }
        }
        // long start = sidx.getOffset(); getOffset works for parsed files only
        long end = sidx.getSize() - 1 + start; // start & end are inclusive!
        return String.format("%s-%s", start, end);
    }




    static Pattern REPRESENTATION_PATTERN = Pattern.compile("(\\$RepresentationID(%0[0-9]+d)?\\$)");
    static Pattern NUMBER_PATTERN = Pattern.compile("(\\$Number(%0[0-9]+d)?\\$)");
    static Pattern TIME_PATTERN = Pattern.compile("(\\$Time(%0[0-9]+d)?\\$)");
    static Pattern BANDWIDTH_PATTERN = Pattern.compile("(\\$Bandwidth(%0[0-9]+d)?\\$)");


    private static String replace(Matcher m, String rv, Object value) {
        while (m.find()) {
            if (m.group(2) != null) {
                rv = rv.replace(m.group(0), String.format(m.group(2), value));
            } else {
                rv = rv.replace(m.group(0), String.valueOf(value));
            }
        }
        return rv;
    }

    public static String templateReplace(String input_string, String representation_id, long number, long bandwidth, long time) {
        String rv = input_string;
        rv = rv.replace("$$", "$");
        rv = replace(REPRESENTATION_PATTERN.matcher(rv), rv, representation_id);
        rv = replace(NUMBER_PATTERN.matcher(rv), rv, number);
        rv = replace(TIME_PATTERN.matcher(rv), rv, time);
        rv = replace(BANDWIDTH_PATTERN.matcher(rv), rv, bandwidth);
        return rv;
    }



}
