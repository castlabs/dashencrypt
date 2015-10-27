package com.castlabs.dash.dashfragmenter.sequences;

import com.castlabs.dash.dashfragmenter.representation.AbstractRepresentationBuilder;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import org.junit.Test;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class DashFileSetSequenceTest {
    @Test
    public void testWriteDataAndCreateRepresentation() throws IOException {
        // checks that %lang% is replaced
        Movie m =  MovieCreator.build(
                DashFileSetSequenceTest.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "/v1.mp4");

        DashFileSetSequence d = new DashFileSetSequence();
        d.setInitPattern("%lang%/init");
        d.setMediaPattern("%lang%/media");
        d.setExplode(true);
        File td = File.createTempFile("testWriteDataAndCreateRepresentation", "DashFileSetSequenceTest");
        td.delete();
        d.setOutputDirectory(td);
        d.writeDataAndCreateRepresentation(new AbstractRepresentationBuilder(m.getTracks().get(0), null, "id", new long[]{1}, new long[]{1}) {

        });
        Assert.assertEquals("eng", td.list()[0]);
        Assert.assertTrue(td.list().length == 1);
    }

}