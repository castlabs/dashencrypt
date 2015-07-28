package com.castlabs.dash.dashfragmenter.sequences;

import junit.framework.TestCase;

import java.io.File;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class DashFileSetSequenceTest extends TestCase {

    public void testGetSubtitleLanguages() throws Exception {
        File subtitle = new File(DashFileSetSequenceTest.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "/ttml-sub.xml");
        DashFileSetSequence seq = new DashFileSetSequence();
        Locale lang = seq.getTextTrackLocale(subtitle);
        assertEquals(Locale.ENGLISH, lang);
   }
}
