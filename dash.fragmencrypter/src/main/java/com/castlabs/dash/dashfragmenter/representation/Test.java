package com.castlabs.dash.dashfragmenter.representation;

import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import mpegDashSchemaMpd2011.*;
import org.apache.xmlbeans.GDuration;
import org.apache.xmlbeans.XmlOptions;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by sannies on 13.06.2015.
 */
public class Test {

    public static void main(String[] args) throws IOException {
        Test t = new Test();
        Movie m = MovieCreator.build("C:\\content\\dual_layer_dual_track\\ves_ArtGlassFHD_133427_640x480_560Kbps_hevc_320x240_140Kbps_BL_EL_RPU_pq2gamma_re.mp4");
        Track main = m.getTracks().get(0);
        Track dependent = m.getTracks().get(1);
        DependentTrackDashBuilder builder = new DependentTrackDashBuilder(main, dependent, "vdep", 48);
        DashTrackWriter.write(builder, "check.mp4");
        t.adaptationSets.put("a1", Collections.<SegmentTemplateRepresentation>singletonList(builder));
        File manifest1 = new File("Manifest.mpd");
        t.getManifest().save(manifest1, getXmlOptions());


    }

    public static XmlOptions getXmlOptions() {
        XmlOptions xmlOptions = new XmlOptions();
        //xmlOptions.setUseDefaultNamespace();
        HashMap<String, String> ns = new HashMap<String, String>();
        //ns.put("urn:mpeg:DASH:schema:MPD:2011", "");
        ns.put("urn:mpeg:cenc:2013", "cenc");
        xmlOptions.setSaveSuggestedPrefixes(ns);
        xmlOptions.setSaveAggressiveNamespaces();
        xmlOptions.setUseDefaultNamespace();
        xmlOptions.setSavePrettyPrint();
        return xmlOptions;
    }


    Map<String, List<SegmentTemplateRepresentation>> adaptationSets = new HashMap<String, List<SegmentTemplateRepresentation>>();

    public MPDDocument getManifest() throws IOException {

        MPDDocument mdd = MPDDocument.Factory.newInstance();
        MPDtype mpd = mdd.addNewMPD();
        PeriodType periodType = mpd.addNewPeriod();
        periodType.setId("0");
        periodType.setStart(new GDuration(1, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO));

        ProgramInformationType programInformationType = mpd.addNewProgramInformation();
        programInformationType.setMoreInformationURL("www.castLabs.com");


        createPeriod(periodType);


        mpd.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");
        mpd.setType(PresentationType.STATIC); // no mpd update strategy implemented yet, could be dynamic


        mpd.setMinBufferTime(new GDuration(1, 0, 0, 0, 0, 0, 4, BigDecimal.ZERO));
        mpd.setMediaPresentationDuration(periodType.getDuration());

        return mdd;
    }


    protected void createPeriod(PeriodType periodType) throws IOException {
        double maxDurationInSeconds = -1;

        for (String adaptationSetId : adaptationSets.keySet()) {
            List<SegmentTemplateRepresentation> tracks = adaptationSets.get(adaptationSetId);

            AdaptationSetType adaptationSet = periodType.addNewAdaptationSet();


            for (SegmentTemplateRepresentation representationSource : tracks) {
                RepresentationType representation = representationSource.getSegmentTemplateRepresentation();
                representation.setId("check-id");
                representation.addNewBaseURL().setStringValue("check.mp4");

                //maxDurationInSeconds = Math.max(maxDurationInSeconds, representationSource.);
                RepresentationType[] representations = adaptationSet.getRepresentationArray();
                representations = Arrays.copyOf(representations, representations.length+1);
                representations[representations.length-1] = representation;
                adaptationSet.setRepresentationArray(representations);
            }


        }


        periodType.setDuration(new GDuration(
                1, 0, 0, 0, (int) (maxDurationInSeconds / 3600),
                (int) ((maxDurationInSeconds % 3600) / 60),
                (int) (maxDurationInSeconds % 60), BigDecimal.ZERO));


    }


}
