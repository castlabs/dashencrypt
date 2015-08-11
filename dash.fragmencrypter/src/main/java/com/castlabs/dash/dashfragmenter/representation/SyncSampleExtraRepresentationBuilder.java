package com.castlabs.dash.dashfragmenter.representation;

import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.util.Mp4Arrays;
import com.mp4parser.iso23001.part7.ProtectionSystemSpecificHeaderBox;
import mpegDashSchemaMpd2011.RepresentationType;

import java.util.List;

public class SyncSampleExtraRepresentationBuilder extends AbstractRepresentationBuilder {

    public static long[] calcSegmentStartSamples(Track track, int minFragmentSamples) {
        long ss[] = track.getSyncSamples();

        long startSamples[] = new long[]{ss[0]};
        for (long s : ss) {
            if (startSamples[startSamples.length - 1] + minFragmentSamples <= s) {
                startSamples = Mp4Arrays.copyOfAndAppend(startSamples, new long[]{s});
            }
        }
        return startSamples;
    }

    public static long[] calcFragmentStartSamples(Track track, int minFragmentSamples) {
        long[] segmentStartSamples = calcSegmentStartSamples(track, minFragmentSamples);
        long[] fragmentStartSamples = new long[segmentStartSamples.length * 2];
        for (int i = 0; i < segmentStartSamples.length; i++) {
            fragmentStartSamples[i * 2] = segmentStartSamples[i];
            fragmentStartSamples[i * 2 + 1] = segmentStartSamples[i] + 1;
        }
        return fragmentStartSamples;
    }


    public SyncSampleExtraRepresentationBuilder(Track track, String source, List<ProtectionSystemSpecificHeaderBox> psshs) {
        super(track, psshs, source, calcSegmentStartSamples(track, 50), calcFragmentStartSamples(track, 50));
    }

    public RepresentationType getOnDemandRepresentation() {
        return null;
    }
}

