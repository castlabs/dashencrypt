package com.castlabs.dash.dashfragmenter.sequences;

import com.castlabs.dash.dashfragmenter.representation.RepresentationBuilderImpl;
import org.junit.Test;
import org.junit.Assert;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.container.mp4.MovieCreator;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

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
        d.writeDataAndCreateRepresentation(new RepresentationBuilderImpl(m.getTracks().get(0), null, "id", new long[]{1}, new long[]{1}) {

        }, Locale.ENGLISH);
        Assert.assertEquals("en", td.list()[0]);
        Assert.assertTrue(td.list().length == 1);
    }

    @Test
    public void testGetCommonIndices() {
        long[] l1 = new long[]{1,3,6,8,10,11,13};
        long[] l2 = new long[]{1,3,4,6,8,10,13};
        long[] res = DashFileSetSequence.getCommonIndices(l1,l2);
        Assert.assertArrayEquals(new long[]{1,3,6,8,10,13}, res);
    }
}