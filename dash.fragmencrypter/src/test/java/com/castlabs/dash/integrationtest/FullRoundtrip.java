package com.castlabs.dash.integrationtest;

import com.castlabs.dash.dashfragmenter.Main;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;

/**
 * Created by sannies on 27.08.2015.
 */
public class FullRoundtrip {
    File tos;

    @Before
    public void setUp() throws Exception {
        File de = new File(System.getProperty("java.io.tmpdir"), ".dashencrypt");
        de.mkdir();
        tos = new File(de, "tos");
        tos.mkdir();
        URI baseUri = new URI("http://com.mp4parser.s3.amazonaws.com/tears_of_steel/");
        String files[] = new String[]{
                "Tears_Of_Steel_1000000.mp4", "Tears_Of_Steel_128000_eng.mp4", "Tears_Of_Steel_128000_ita.mp4", "Tears_Of_Steel_1400000.mp4", "Tears_Of_Steel_600000.mp4", "Tears_Of_Steel_800000.mp4", "Tears_Of_Steel_deu.vtt", "Tears_Of_Steel_deu.xml", "Tears_Of_Steel_eng.vtt", "Tears_Of_Steel_eng.xml", "Tears_Of_Steel_esp.vtt", "Tears_Of_Steel_esp.xml", "Tears_Of_Steel_fra.vtt", "Tears_Of_Steel_fra.xml", "Tears_Of_Steel_nld.vtt", "Tears_Of_Steel_nld.xml", "Tears_Of_Steel_per.vtt", "Tears_Of_Steel_per.xml", "Tears_Of_Steel_rus.vtt", "Tears_Of_Steel_rus.xml", "chapters/0_08-Rocket-starts.jpg", "chapters/0_40-Fourty-Years-Later.jpg", "chapters/3_03-Show-Starts.jpg", "chapters/5_29-Things-go-south.jpg", "chapters/7_05-attack-starts.jpg", "chapters/8_51-making-peace.jpg", "chapters/9_49-Epilog.jpg", "chapters/tos-chapters-en.vtt", "chapters/tos-chapters-it.xml", "chapters/tos-chapters-en.xml", "trickplay/tears_of_steel_1080p_500x208_600-6fps.mp4", "trickplay/tears_of_steel_1080p_614x256_800-6fps.mp4", "trickplay/tears_of_steel_1080p_768x320_1000-6fps.mp4", "trickplay/tears_of_steel_1080p_1152x480_1400-6fps.mp4"};

        for (String file : files) {
            File i = new File(tos, file);
            if (!i.exists()) {
                i.getParentFile().mkdir();
                FileOutputStream fos = new FileOutputStream(i);
                IOUtils.copy(baseUri.resolve(file).toURL().openStream(), fos);
                fos.close();
            }
        }
    }

    @Test
    public void testOnDemandPlain() throws Exception {
        File outputDir = File.createTempFile("FullRoundtrip", "testOnDemandPlain");
        outputDir.delete();
        outputDir.mkdir();

        Main.main(new String[]{
                "dash",
                "-o", outputDir.getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_1000000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_1400000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_800000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_600000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_128000_eng.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_128000_ita.mp4").getAbsolutePath(),
        });

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testOnDemandPlain.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
        FileUtils.deleteDirectory(outputDir);
    }

    @Test
    public void testLivePlain() throws Exception {
        File outputDir = File.createTempFile("FullRoundtrip", "testLivePlain");
        outputDir.delete();
        outputDir.mkdir();

        Main.main(new String[]{
                "dash",
                "-x",
                "-o", outputDir.getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_1000000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_1400000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_800000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_600000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_128000_eng.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_128000_ita.mp4").getAbsolutePath(),
        });

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testLivePlain.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
        FileUtils.deleteDirectory(outputDir);
    }
    @Test
    public void testLiveEncrypted() throws Exception {
        File outputDir = File.createTempFile("FullRoundtrip", "testLiveEncrypted");
        outputDir.delete();
        outputDir.mkdir();

        Main.main(new String[]{
                "encrypt",
                "-x",
                "--dummyIvs",
                "--secretKey:v", "550e8400-e29b-11d4-a716-446655440000",
                "--uuid:v", "550e8400-e29b-11d4-a716-446655440000",
                "-o", outputDir.getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_1000000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_1400000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_800000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_600000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_128000_eng.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_128000_ita.mp4").getAbsolutePath(),
        });

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testLiveEncrypted.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
        FileUtils.deleteDirectory(outputDir);
    }

    @Test
    public void testOnDemandEncrypted() throws Exception {
        File outputDir = File.createTempFile("FullRoundtrip", "testOnDemandEncrypted");
        outputDir.delete();
        outputDir.mkdir();

        Main.main(new String[]{
                "encrypt",
                "--dummyIvs",
                "--secretKey:v", "550e8400-e29b-11d4-a716-446655440000",
                "--uuid:v", "550e8400-e29b-11d4-a716-446655440000",
                "-o", outputDir.getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_1000000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_1400000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_800000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_600000.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_128000_eng.mp4").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_128000_ita.mp4").getAbsolutePath(),
        });

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testOnDemandEncrypted.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
        FileUtils.deleteDirectory(outputDir);
    }

    @Test
    public void testOnDemandSubtiltlesAndCaptions() throws Exception {
        File outputDir = File.createTempFile("FullRoundtrip", "testOnDemandSubtiltlesAndCaptions");
        outputDir.delete();
        outputDir.mkdir();

        Main.main(new String[]{
                "dash",
                "-o", outputDir.getAbsolutePath(),
                "-st", new File(tos, "Tears_Of_Steel_nld.vtt").getAbsolutePath(),
                "-st", new File(tos, "Tears_Of_Steel_per.vtt").getAbsolutePath(),
                "-st", new File(tos, "Tears_Of_Steel_rus.vtt").getAbsolutePath(),
                "-cc", new File(tos, "Tears_Of_Steel_nld.vtt").getAbsolutePath(),
                "-cc", new File(tos, "Tears_Of_Steel_per.vtt").getAbsolutePath(),
                "-cc", new File(tos, "Tears_Of_Steel_rus.vtt").getAbsolutePath(),
                new File(tos, "Tears_Of_Steel_600000.mp4").getAbsolutePath(),

        });

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testOnDemandSubtiltlesAndCaptions.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
        FileUtils.deleteDirectory(outputDir);
    }


}
