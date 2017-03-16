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
public class FullRoundtripTest {
    File tos;

    @Before
    public void setUp() throws Exception {
        File de = new File(System.getProperty("user.home"), ".dash-encrypt-cache");
        de.mkdir();
        tos = new File(de, "tos");
        tos.mkdir();
        URI baseUri = new URI("http://castlabs-dl.s3.amazonaws.com/public/dash.encrypt/testdata/");
        String files[] = new String[]{
                "tears_of_steel/Tears_Of_Steel_1000000.mp4", "tears_of_steel/Tears_Of_Steel_128000_eng.mp4",
                "tears_of_steel/Tears_Of_Steel_128000_ita.mp4", "tears_of_steel/Tears_Of_Steel_128000_ac3_eng.mp4",
                "tears_of_steel/Tears_Of_Steel_128000_ac3_ita.mp4", "tears_of_steel/Tears_Of_Steel_1400000.mp4",
                "tears_of_steel/Tears_Of_Steel_600000.mp4", "tears_of_steel/Tears_Of_Steel_800000.mp4",
                "tears_of_steel/Tears_Of_Steel_deu.vtt", "tears_of_steel/Tears_Of_Steel_deu.xml",
                "tears_of_steel/Tears_Of_Steel_eng.vtt", "tears_of_steel/Tears_Of_Steel_eng.xml",
                "tears_of_steel/Tears_Of_Steel_esp.vtt", "tears_of_steel/Tears_Of_Steel_esp.xml",
                "tears_of_steel/Tears_Of_Steel_fra.vtt", "tears_of_steel/Tears_Of_Steel_fra.xml",
                "tears_of_steel/Tears_Of_Steel_nld.vtt", "tears_of_steel/Tears_Of_Steel_nld.xml",
                "tears_of_steel/Tears_Of_Steel_per.vtt", "tears_of_steel/Tears_Of_Steel_per.xml",
                "tears_of_steel/Tears_Of_Steel_rus.vtt", "tears_of_steel/Tears_Of_Steel_rus.xml",
                "tears_of_steel/chapters/0_08-Rocket-starts.jpg", "tears_of_steel/chapters/0_40-Fourty-Years-Later.jpg",
                "tears_of_steel/chapters/3_03-Show-Starts.jpg", "tears_of_steel/chapters/5_29-Things-go-south.jpg",
                "tears_of_steel/chapters/7_05-attack-starts.jpg", "tears_of_steel/chapters/8_51-making-peace.jpg",
                "tears_of_steel/chapters/9_49-Epilog.jpg", "tears_of_steel/chapters/tos-chapters-en.vtt",
                "tears_of_steel/chapters/tos-chapters-it.xml", "tears_of_steel/chapters/tos-chapters-en.xml",
                "tears_of_steel/trickplay/tears_of_steel_1080p_500x208_600-6fps.mp4",
                "tears_of_steel/trickplay/tears_of_steel_1080p_614x256_800-6fps.mp4",
                "tears_of_steel/trickplay/tears_of_steel_1080p_768x320_1000-6fps.mp4",
                "tears_of_steel/trickplay/tears_of_steel_1080p_1152x480_1400-6fps.mp4",
                "ffmpeg-generated.ismv"
        };

        for (String file : files) {
            File i = new File(tos, file);
            if (!i.exists()) {
                System.err.println("Downloading " + i);
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
                new File(tos, "tears_of_steel/Tears_Of_Steel_1000000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_1400000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_800000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_600000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_eng.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_ita.mp4").getAbsolutePath(),
        });

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testOnDemandPlain.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
        FileUtils.deleteDirectory(outputDir);
    }

    @Test
    public void testOnDemandPlainIsmvInput() throws Exception {
        File outputDir = File.createTempFile("FullRoundtrip", "testOnDemandPlainIsmvInput");
        outputDir.delete();
        outputDir.mkdir();

        Main.main(new String[]{
                "dash",
                "-o", outputDir.getAbsolutePath(),
                new File(tos, "ffmpeg-generated.ismv").getAbsolutePath(),
        });
        System.err.println(new File(tos, "ffmpeg-generated.ismv"));
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testOnDemandPlainIsmvInput.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
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
                new File(tos, "tears_of_steel/Tears_Of_Steel_1000000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_1400000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_800000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_600000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_eng.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_ita.mp4").getAbsolutePath(),
        });

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testLivePlain.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
        FileUtils.deleteDirectory(outputDir);
    }

    @Test
    public void testLivePlainOneAudio() throws Exception {
        File outputDir = File.createTempFile("FullRoundtrip", "testLivePlain");
        outputDir.delete();
        outputDir.mkdir();

        Main.main(new String[]{
                "dash",
                "-x",
                "-o", outputDir.getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_600000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_eng.mp4").getAbsolutePath(),
        });

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testLivePlainOneAudio.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
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
                "--secretKey:v", "550e8400e29b11d4a716446655440000",
                "--uuid:v", "550e8400-e29b-11d4-a716-446655440000",
                "-o", outputDir.getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_1000000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_1400000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_800000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_600000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_eng.mp4").getAbsolutePath(),

                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_ita.mp4").getAbsolutePath(),

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
                "--secretKey:v", "550e8400e29b11d4a716446655440000",
                "--uuid:v", "550e8400-e29b-11d4-a716-446655440000",
                "-o", outputDir.getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_1000000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_1400000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_800000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_600000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_eng.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_ita.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_ac3_eng.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_ac3_ita.mp4").getAbsolutePath(),
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
                "-st", new File(tos, "tears_of_steel/Tears_Of_Steel_nld.vtt").getAbsolutePath(),
                "-st", new File(tos, "tears_of_steel/Tears_Of_Steel_per.vtt").getAbsolutePath(),
                "-st", new File(tos, "tears_of_steel/Tears_Of_Steel_rus.vtt").getAbsolutePath(),
                "-cc", new File(tos, "tears_of_steel/Tears_Of_Steel_nld.vtt").getAbsolutePath(),
                "-cc", new File(tos, "tears_of_steel/Tears_Of_Steel_per.vtt").getAbsolutePath(),
                "-cc", new File(tos, "tears_of_steel/Tears_Of_Steel_rus.vtt").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_600000.mp4").getAbsolutePath(),

        });

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testOnDemandSubtiltlesAndCaptions.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
        FileUtils.deleteDirectory(outputDir);
    }

    @Test
    public void testLivePlainWithSelectors() throws Exception {
        File outputDir = File.createTempFile("FullRoundtrip", "testLivePlainWithSelectors");
        outputDir.delete();
        outputDir.mkdir();

        Main.main(new String[]{
                "dash",
                "-o", outputDir.getAbsolutePath(),
                "[type=video]" + new File(tos, "tears_of_steel/Tears_Of_Steel_1000000.mp4").getAbsolutePath(),
                "[type=audio]" + new File(tos, "tears_of_steel/Tears_Of_Steel_1400000.mp4").getAbsolutePath(),
                "[track=1]" + new File(tos, "tears_of_steel/Tears_Of_Steel_800000.mp4").getAbsolutePath(),
                "[track=2]" + new File(tos, "tears_of_steel/Tears_Of_Steel_600000.mp4").getAbsolutePath(),
                "[language=eng]" + new File(tos, "tears_of_steel/Tears_Of_Steel_128000_eng.mp4").getAbsolutePath(),
                "[language=eng]" + new File(tos, "tears_of_steel/Tears_Of_Steel_128000_ita.mp4").getAbsolutePath(),
        });

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testLivePlainWithSelectors.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
        FileUtils.deleteDirectory(outputDir);
    }

    @Test
    public void testLanguageMap() throws Exception {
        File outputDir = File.createTempFile("FullRoundtrip", "testLivePlainWithSelectors");
        outputDir.delete();
        outputDir.mkdir();

        Main.main(new String[]{
                "dash",
                "-o", outputDir.getAbsolutePath(),
                "--language-map", "eng=spa",
                new File(tos, "tears_of_steel/Tears_Of_Steel_1000000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_eng.mp4").getAbsolutePath()
                ,
        });

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testLanguageMap.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
        FileUtils.deleteDirectory(outputDir);
    }



}
