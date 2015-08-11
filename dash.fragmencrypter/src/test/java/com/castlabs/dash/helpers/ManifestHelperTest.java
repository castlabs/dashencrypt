package com.castlabs.dash.helpers;

import org.junit.Assert;
import org.junit.Test;


/**
 * Created by sannies on 11.08.2015.
 */
public class ManifestHelperTest {

    @Test
    public void testTemplateReplace() throws Exception {
        String mediaPattern1 = "$RepresentationID$/media-$Time$.mp4";
        String mediaPattern2 = "$RepresentationID$/media-$Number$.mp4";
        String mediaPattern3 = "$RepresentationID$/media-$Number%04d$.mp4";

        String mediaPattern1Replaced = ManifestHelper.templateReplace(mediaPattern1, "abc", 213, 4354, 5463);
        Assert.assertEquals("abc/media-5463.mp4", mediaPattern1Replaced);
        String mediaPattern2Replaced = ManifestHelper.templateReplace(mediaPattern2, "abc", 213, 4354, 5463);
        Assert.assertEquals("abc/media-213.mp4", mediaPattern2Replaced);
        String mediaPattern3Replaced = ManifestHelper.templateReplace(mediaPattern3, "abc", 213, 4354, 5463);
        Assert.assertEquals("abc/media-0213.mp4", mediaPattern3Replaced);
    }
}