package com.castlabs.dash.helpers;

import com.castlabs.dash.dashfragmenter.sequences.DashFileSetSequence;
import com.coremedia.iso.Hex;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;
import com.googlecode.mp4parser.MemoryDataSourceImpl;
import com.googlecode.mp4parser.util.Path;
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

    public void testGetChannelConfiguration() throws Exception {
        byte[] ec3_1 = Hex.decodeHex("0000003265632D33000000000000000100000000000000000002001000000000BB8000000000000E646563330868200F0084");
        byte[] ec3_2 = Hex.decodeHex("0000003265632D33000000000000000100000000000000000002001000000000BB8000000000000E646563330C00200F0202");

        IsoFile isoFile1 = new IsoFile(new MemoryDataSourceImpl(ec3_1));
        DashHelper.ChannelConfiguration channelConfiguration1 = DashHelper.getChannelConfiguration((AudioSampleEntry) isoFile1.getBoxes().get(0));
        Assert.assertEquals("urn:dolby:dash:audio_channel_configuration:2011", channelConfiguration1.schemeIdUri);
        Assert.assertEquals("F801", channelConfiguration1.value);

        IsoFile isoFile2 = new IsoFile(new MemoryDataSourceImpl(ec3_2));
        DashHelper.ChannelConfiguration channelConfiguration2 = DashHelper.getChannelConfiguration((AudioSampleEntry) isoFile2.getBoxes().get(0));
        Assert.assertEquals("urn:dolby:dash:audio_channel_configuration:2011", channelConfiguration2.schemeIdUri);
        Assert.assertEquals("FA01", channelConfiguration2.value);
    }
}