package com.castlabs.dash.helpers;

import junit.framework.TestCase;
import org.junit.Assert;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.sampleentry.AudioSampleEntry;
import org.mp4parser.boxes.sampleentry.SampleEntry;
import org.mp4parser.muxer.MemoryDataSourceImpl;
import org.mp4parser.tools.Hex;
import org.mp4parser.tools.Path;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Locale;

public class DashHelperTest extends TestCase {

    public void testGetRfc6381Codec() throws Exception {
        IsoFile isoFile = new IsoFile(DashHelperTest.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "/foreman-hevc-384.mp4");

        SampleEntry sampleEntry = Path.getPath(isoFile, "moov[0]/trak[0]/mdia[0]/minf[0]/stbl[0]/stsd[0]/hev1[0]");

        Assert.assertEquals("hev1.1.6.L93.80", DashHelper2.getRfc6381Codec(sampleEntry));

    }


    public void testGetSubtitleLanguages() throws Exception {
        File subtitle = new File(DashHelperTest.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "/ttml-sub.xml");

        Locale lang = DashHelper2.getTextTrackLocale(subtitle);
        assertEquals(Locale.ENGLISH, lang);
    }

    public void testGetChannelConfiguration() throws Exception {
        byte[] ec3_1 = Hex.decodeHex("0000003265632D33000000000000000100000000000000000002001000000000BB8000000000000E646563330868200F0084");
        byte[] ec3_2 = Hex.decodeHex("0000003265632D33000000000000000100000000000000000002001000000000BB8000000000000E646563330C00200F0202");

        IsoFile isoFile1 = new IsoFile(Channels.newChannel(new ByteArrayInputStream(ec3_1)));
        DashHelper2.ChannelConfiguration channelConfiguration1 = DashHelper2.getChannelConfiguration((AudioSampleEntry) isoFile1.getBoxes().get(0));
        Assert.assertEquals("urn:dolby:dash:audio_channel_configuration:2011", channelConfiguration1.schemeIdUri);
        Assert.assertEquals("F801", channelConfiguration1.value);

        IsoFile isoFile2 = new IsoFile(Channels.newChannel(new ByteArrayInputStream(ec3_2)));
        DashHelper2.ChannelConfiguration channelConfiguration2 = DashHelper2.getChannelConfiguration((AudioSampleEntry) isoFile2.getBoxes().get(0));
        Assert.assertEquals("urn:dolby:dash:audio_channel_configuration:2011", channelConfiguration2.schemeIdUri);
        Assert.assertEquals("FA01", channelConfiguration2.value);
    }
}