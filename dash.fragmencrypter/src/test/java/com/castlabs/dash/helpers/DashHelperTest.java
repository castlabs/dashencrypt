package com.castlabs.dash.helpers;

import com.castlabs.dash.dashfragmenter.sequences.DashFileSetSequence;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;import com.googlecode.mp4parser.util.Path;
import junit.framework.TestCase;
import org.junit.Assert;

import java.io.File;
import java.lang.Exception;
import java.util.Locale;

public class DashHelperTest extends TestCase {

    public void testGetRfc6381Codec() throws Exception {
        IsoFile isoFile = new IsoFile(DashHelperTest.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "/foreman-hevc-384.mp4");

        SampleEntry sampleEntry = Path.getPath(isoFile, "/moov[0]/trak[0]/mdia[0]/minf[0]/stbl[0]/stsd[0]/hev1[0]");

        Assert.assertEquals("hev1.1.c.L93.80", DashHelper.getRfc6381Codec(sampleEntry));

    }


    public void testGetSubtitleLanguages() throws Exception {
        File subtitle = new File(DashHelperTest.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "/ttml-sub.xml");

        Locale lang = DashHelper.getTextTrackLocale(subtitle);
        assertEquals(Locale.ENGLISH, lang);
    }
}