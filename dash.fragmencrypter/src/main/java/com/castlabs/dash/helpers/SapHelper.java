package com.castlabs.dash.helpers;


import org.mp4parser.boxes.iso14496.part12.SampleFlags;
import org.mp4parser.boxes.iso14496.part12.TrackRunBox;

import java.util.Arrays;

/**
 * Created by sannies on 26.06.2015.
 */
public class SapHelper {

    public static byte getFirstFrameSapType(long[] ptss, SampleFlags sapSampleFlags) {
        // I_SAP, T_SAP, I_SAU, T_DEC, T_EPT,  T_PTF
        if (sapSampleFlags.isSampleIsDifferenceSample()) {
            return 0;
        } else {
            long sapTS = ptss[0];
            Arrays.sort(ptss);
            if (sapTS == ptss[0]) {
                return 1; // pts = cts
            } else {
                return 2; // pts != cts
            }
        }
    }


    public static SampleFlags getSampleFlags(int i, TrackRunBox trun, org.mp4parser.boxes.iso14496.part12.TrackExtendsBox trex) {
        return trun.isFirstSampleFlagsPresent() ?
                trun.getFirstSampleFlags() :
                (trun.isSampleFlagsPresent() ? trun.getEntries().get(i).getSampleFlags() : trex.getDefaultSampleFlags());

    }

}
