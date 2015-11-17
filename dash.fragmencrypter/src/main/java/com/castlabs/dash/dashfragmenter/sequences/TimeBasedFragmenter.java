package com.castlabs.dash.dashfragmenter.sequences;

import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.Fragmenter;
import com.googlecode.mp4parser.util.Mp4Arrays;

import java.util.Arrays;

/**
 * Finds start samples within a given track so that:
 * <ul>
 * <li>Each segment is at least <code>fragmentLength</code> seconds long</li>
 * <li>The last segment might be almost twice the size of the rest</li>
 * </ul>
 * @todo this is an exact copy of the DefaultFragmenter available in isoparser 1.1.18
 * @todo remove once 1.1.18 is published
 */
public class TimeBasedFragmenter implements Fragmenter {
    private double fragmentLength = 2.0D;

    public TimeBasedFragmenter(double fragmentLength) {
        this.fragmentLength = fragmentLength;
    }

    public long[] sampleNumbers(Track track) {
        long[] segmentStartSamples = new long[]{1L};
        long[] sampleDurations = track.getSampleDurations();
        long[] syncSamples = track.getSyncSamples();
        long timescale = track.getTrackMetaData().getTimescale();
        double time = 0.0D;

        for (int i = 0; i < sampleDurations.length; ++i) {
            time += (double) sampleDurations[i] / (double) timescale;
            if (time >= this.fragmentLength && (syncSamples == null || Arrays.binarySearch(syncSamples, (long) (i + 1)) >= 0)) {
                if (i > 0) {
                    segmentStartSamples = Mp4Arrays.copyOfAndAppend(segmentStartSamples, (long) (i + 1));
                }

                time = 0.0D;
            }
        }
        // In case the last Fragment is shorter: make the previous one a bigger and omit the small one
        if (time > 0) {
            long[] nuSegmentStartSamples = new long[segmentStartSamples.length - 1];
            System.arraycopy(segmentStartSamples, 0, nuSegmentStartSamples, 0, segmentStartSamples.length - 1);
            segmentStartSamples = nuSegmentStartSamples;
        }

        return segmentStartSamples;
    }


}
