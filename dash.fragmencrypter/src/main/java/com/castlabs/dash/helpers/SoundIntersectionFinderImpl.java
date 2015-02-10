package com.castlabs.dash.helpers;

import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.FragmentIntersectionFinder;

import java.lang.Error;import java.util.*;import java.util.ArrayList;import java.util.Arrays;import java.util.List;

/**
 * Created by sannies on 05.02.2015.
 */
public class SoundIntersectionFinderImpl implements FragmentIntersectionFinder {


    long timscale = 1;
    private List<Long> syncPoints;

    /**
     * Creates an intersection finder for sound tracks. This finder will not take sync samples
     * into consideration but makes that all intersects for all tracks are at the same times.
     *
     * @param tracks                  all sound tracks
     * @param desiredFragmentDuration desired fragment duration in seconds
     */
    public SoundIntersectionFinderImpl(List<Track> tracks, int desiredFragmentDuration) {

        for (Track track : tracks) {
            timscale = lcmcal(timscale, track.getTrackMetaData().getTimescale());
        }
        long[] commonTimes = null;
        for (Track track : tracks) {
            long trackTimeScale = track.getTrackMetaData().getTimescale();
            long[] sampleTimes = new long[track.getSamples().size()];
            long[] sampleDurations = track.getSampleDurations();
            for (int i = 1; i < sampleDurations.length; i++) {
                sampleTimes[i] = sampleTimes[i - 1] + sampleDurations[i] * timscale / trackTimeScale;
            }
            if (commonTimes == null) {
                commonTimes = sampleTimes;
            } else {
                long[] nuCommonTimes = new long[0];
                for (long timeStamp : commonTimes) {
                    if (Arrays.binarySearch(sampleTimes, timeStamp) >= 0) {
                        nuCommonTimes = Arrays.copyOf(nuCommonTimes, nuCommonTimes.length + 1);
                        nuCommonTimes[nuCommonTimes.length - 1] = timeStamp;
                    }
                }
            }

        }
        syncPoints = new ArrayList<Long>();
        for (long timestamp : commonTimes) {
            if (timestamp >= desiredFragmentDuration * timscale * syncPoints.size() ) {
                syncPoints.add(timestamp);
            }
        }

    }


    /**
     * {@inheritDoc}
     */
    public long[] sampleNumbers(Track track) {
        long time = 0;
        long[] sampleNums = new long[0];
        long[] sampleDurations = track.getSampleDurations();
        long trackTimeScale = track.getTrackMetaData().getTimescale();
        for (int i = 0; i < sampleDurations.length; i++) {
            if (syncPoints.contains(time)) {
                sampleNums = Arrays.copyOf(sampleNums, sampleNums.length + 1);
                sampleNums[sampleNums.length - 1] = i + 1;
            }
            time += sampleDurations[i] * timscale / trackTimeScale;
        }
        return sampleNums;
    }

    long lcmcal(long x1, long x2) {
        long max,min;
        if (x1>x2) {
            max = x1;
            min = x2;
        } else {
            max = x2;
            min = x1;
        }
        for(int i=1; i<=min; i++) {
            if( (max*i)%min == 0 ) {
                return i*max;
            }
        }
        throw new Error("Cannot find the least common multiple of numbers "+
                x1+" and "+x2);
    }

}
