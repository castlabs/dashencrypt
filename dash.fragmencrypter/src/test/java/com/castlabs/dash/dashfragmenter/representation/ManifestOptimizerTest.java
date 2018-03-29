package com.castlabs.dash.dashfragmenter.representation;

import mpeg.dash.schema.mpd._2011.AdaptationSetType;
import mpeg.dash.schema.mpd._2011.MPDtype;
import mpeg.dash.schema.mpd._2011.RepresentationType;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.*;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ManifestOptimizerTest {

    @Test
    public void testMinMaxFrameRate() throws IOException, JAXBException {

        MPDtype mpd = read(new File(ManifestOptimizerTest.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "/com/castlabs/dash/dashfragmenter/representation/exampleManifest.mpd"));


        ManifestOptimizer.adjustMinMaxFrameRate(mpd.getPeriod().get(0).getAdaptationSet().get(0));
        Assert.assertEquals("12000/1000", mpd.getPeriod().get(0).getAdaptationSet().get(0).getMinFrameRate());
        Assert.assertEquals("24000/1000", mpd.getPeriod().get(0).getAdaptationSet().get(0).getMaxFrameRate());

        MPDtype mpd2 = read(new File(ManifestOptimizerTest.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "/com/castlabs/dash/dashfragmenter/representation/exampleManifest.mpd"));

        mpd2.getPeriod().get(0).getAdaptationSet().remove(0);
        ManifestOptimizer.adjustMinMaxFrameRate(mpd2.getPeriod().get(0).getAdaptationSet().get(0));
        Assert.assertEquals(null, mpd2.getPeriod().get(0).getAdaptationSet().get(0).getMinFrameRate());
        Assert.assertEquals(null, mpd2.getPeriod().get(0).getAdaptationSet().get(0).getMaxFrameRate());
    }

    @Test
    public void testMinMax() throws IOException, JAXBException {
        MPDtype mpd = read(new File(ManifestOptimizerTest.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "/com/castlabs/dash/dashfragmenter/representation/exampleManifest.mpd"));

        ManifestOptimizer.adjustMinMax(mpd.getPeriod().get(0).getAdaptationSet().get(0), RepresentationType::getBandwidth, AdaptationSetType::setMinBandwidth, AdaptationSetType::setMaxBandwidth);
        Assert.assertEquals(new Long(549100), mpd.getPeriod().get(0).getAdaptationSet().get(0).getMinBandwidth());
        Assert.assertEquals(new Long(1356600), mpd.getPeriod().get(0).getAdaptationSet().get(0).getMaxBandwidth());

        ManifestOptimizer.adjustMinMax(mpd.getPeriod().get(0).getAdaptationSet().get(1), RepresentationType::getBandwidth, AdaptationSetType::setMinBandwidth, AdaptationSetType::setMaxBandwidth);
        Assert.assertNull(mpd.getPeriod().get(0).getAdaptationSet().get(1).getMinBandwidth());
        Assert.assertNull(mpd.getPeriod().get(0).getAdaptationSet().get(1).getMaxBandwidth());


    }

    MPDtype read(File f) {
        JAXBContext context = null;
        Unmarshaller m = null;
        try {
            context = JAXBContext.newInstance(MPDtype.class);
            m = context.createUnmarshaller();
            JAXBElement<MPDtype> o = m.unmarshal(
                    new StreamSource(new FileInputStream(f)),
                    MPDtype.class
            );
            MPDtype mpd = o.getValue();
            return  mpd;
        } catch (JAXBException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

}