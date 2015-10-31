package com.castlabs.dash.dashfragmenter.representation;

import mpegDashSchemaMpd2011.MPDDocument;
import org.apache.xmlbeans.XmlException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ManifestOptimizerTest {

    @Test
    public void testMinMaxFrameRate() throws IOException, XmlException {

        MPDDocument mpd = MPDDocument.Factory.parse(new File(ManifestOptimizerTest.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "/com/castlabs/dash/dashfragmenter/representation/exampleManifest.mpd"));
        ManifestOptimizer.adjustMinMaxFrameRate(mpd.getMPD().getPeriodArray(0).getAdaptationSetArray(0));
        Assert.assertEquals("12000/1000", mpd.getMPD().getPeriodArray(0).getAdaptationSetArray(0).getMinFrameRate());
        Assert.assertEquals("24000/1000", mpd.getMPD().getPeriodArray(0).getAdaptationSetArray(0).getMaxFrameRate());

        MPDDocument mpd2 = MPDDocument.Factory.parse(new File(ManifestOptimizerTest.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "/com/castlabs/dash/dashfragmenter/representation/exampleManifest.mpd"));
        mpd2.getMPD().getPeriodArray(0).getAdaptationSetArray(0).removeRepresentation(0);
        ManifestOptimizer.adjustMinMaxFrameRate(mpd2.getMPD().getPeriodArray(0).getAdaptationSetArray(0));
        Assert.assertEquals(null, mpd2.getMPD().getPeriodArray(0).getAdaptationSetArray(0).getMinFrameRate());
        Assert.assertEquals(null, mpd2.getMPD().getPeriodArray(0).getAdaptationSetArray(0).getMaxFrameRate());
    }

    @Test
    public void testMinMax() throws IOException, XmlException {

        MPDDocument mpd = MPDDocument.Factory.parse(new File(ManifestOptimizerTest.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "/com/castlabs/dash/dashfragmenter/representation/exampleManifest.mpd"));
        ManifestOptimizer.adjustMinMax(mpd.getMPD().getPeriodArray(0).getAdaptationSetArray(0), "bandwidth");
        Assert.assertEquals(549100, mpd.getMPD().getPeriodArray(0).getAdaptationSetArray(0).getMinBandwidth());
        Assert.assertEquals(1296300, mpd.getMPD().getPeriodArray(0).getAdaptationSetArray(0).getMaxBandwidth());

        ManifestOptimizer.adjustMinMax(mpd.getMPD().getPeriodArray(0).getAdaptationSetArray(1), "bandwidth");
        Assert.assertFalse(mpd.getMPD().getPeriodArray(0).getAdaptationSetArray(1).isSetMinBandwidth());
        Assert.assertFalse(mpd.getMPD().getPeriodArray(0).getAdaptationSetArray(1).isSetMaxBandwidth());


    }

}