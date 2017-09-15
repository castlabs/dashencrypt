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
    public void testEncrypt2_plain() throws Exception {
        File outputDir = File.createTempFile("FullRoundtrip", "testEncrypt2");
        outputDir.delete();
        outputDir.mkdir();

        Main.main(new String[]{
                "encrypt2",
                "-o", outputDir.getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_1000000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_1400000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_800000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_600000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_eng.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_ita.mp4").getAbsolutePath(),
        });

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testEncrypt2_plain.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
        FileUtils.deleteDirectory(outputDir);
    }

    @Test
    public void testEncrypt2_encrypted1() throws Exception {
        File outputDir = File.createTempFile("FullRoundtrip", "testEncrypt2");
        outputDir.delete();
        outputDir.mkdir();

        Main.main(new String[]{
                "encrypt2",
                "-o", outputDir.getAbsolutePath(),
                "--secretKey:v", "cbfed0736042e5962db8e23ddf1d0425",
                "--uuid:v", "22089b3b-527f-4612-a7bb-944559547306",
                new File(tos, "tears_of_steel/Tears_Of_Steel_1000000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_1400000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_800000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_600000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_eng.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_ita.mp4").getAbsolutePath(),
        });

        XMLUnit.setIgnoreWhitespace(true);
        System.out.println(FileUtils.readFileToString(new File(outputDir, "Manifest.mpd")));
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testEncrypt2_encrypted1.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
        FileUtils.deleteDirectory(outputDir);
    }

    @Test
    public void testEncrypt2_encrypted1_clearlead() throws Exception {
        File outputDir = File.createTempFile("FullRoundtrip", "testEncrypt2");
        outputDir.delete();
        outputDir.mkdir();

        Main.main(new String[]{
                "encrypt2",
                "-o", outputDir.getAbsolutePath(),
                "-clearlead", "30",
                "--secretKey:v", "cbfed0736042e5962db8e23ddf1d0425",
                "--uuid:v", "22089b3b-527f-4612-a7bb-944559547306",
                new File(tos, "tears_of_steel/Tears_Of_Steel_1000000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_1400000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_800000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_600000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_eng.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_ita.mp4").getAbsolutePath(),
        });

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testEncrypt2_encrypted1_clearlead.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
        FileUtils.deleteDirectory(outputDir);
    }

    @Test
    public void testEncrypt2_encrypted2() throws Exception {
        File outputDir = File.createTempFile("FullRoundtrip", "testEncrypt2");
        outputDir.delete();
        outputDir.mkdir();

        Main.main(new String[]{
                "encrypt2",
                "-o", outputDir.getAbsolutePath(),
                "--secretKey:v", "550e8400e29b11d4a716446655441111",
                "--uuid:v", "550e8400-e29b-11d4-a716-446655440000",
                "--secretKey:a", "660e8400e29b11d4a716446655441111",
                "--uuid:a", "660e8400-e29b-11d4-a716-446655440000",
                new File(tos, "tears_of_steel/Tears_Of_Steel_1000000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_1400000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_800000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_600000.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_eng.mp4").getAbsolutePath(),
                new File(tos, "tears_of_steel/Tears_Of_Steel_128000_ita.mp4").getAbsolutePath(),
        });

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(new InputSource(getClass().getResourceAsStream("testEncrypt2_encrypted2.mpd")), new InputSource(new FileInputStream(new File(outputDir, "Manifest.mpd"))));
        FileUtils.deleteDirectory(outputDir);
    }


}
