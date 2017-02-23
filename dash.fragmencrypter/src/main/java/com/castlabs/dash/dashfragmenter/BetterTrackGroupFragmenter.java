package com.castlabs.dash.dashfragmenter;

import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.Fragmenter;
import com.googlecode.mp4parser.util.Mp4Arrays;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.googlecode.mp4parser.util.CastUtils.l2i;
import static java.util.Arrays.binarySearch;

public class BetterTrackGroupFragmenter implements Fragmenter {
    private final Map<Track, long[]> allTracks = new HashMap<>();


    public BetterTrackGroupFragmenter(double targetDuration, List<Track> allTracks) {
        if (allTracks.size() > 1) {
            for (Track track : allTracks) {
                if (track.getSyncSamples() == null) {
                    throw new RuntimeException("BetterTrackGroupFragmenter cannot be used to fragment groups of tracks without syncsamples");
                }
            }
        }

        long[] timeScale = new long[allTracks.size()];
        long[][] syncSamples = new long[allTracks.size()][];
        long[][] sampleDuration = new long[allTracks.size()][];
        for (int i = 0; i < allTracks.size(); i++) {
            syncSamples[i] = allTracks.get(i).getSyncSamples();
            sampleDuration[i] = allTracks.get(i).getSampleDurations();
            timeScale[i] = allTracks.get(i).getTrackMetaData().getTimescale();
        }

        long[][] fragmentSamples = getCommonSyncSamples(timeScale, syncSamples, sampleDuration, targetDuration);
        for (int i = 0; i < fragmentSamples.length; i++) {
            this.allTracks.put(allTracks.get(i), fragmentSamples[i]);
        }
    }

    protected static long[][] getCommonSyncSamples(long[] timeScale, long[][] syncSamples, long[][] sampleDuration, double targetDuration) {
        long[][] fragmentSamples = new long[syncSamples.length][];
        int[] currentSyncSample = new int[syncSamples.length];
        long[]  currentTicks = new long[syncSamples.length];
        double nextTarget = 0;
        do {
            // Check if we found a common sync sample
            if (hasFoundNextCommonSyncSample(currentTicks, timeScale, nextTarget)) {
                for (int i = 0; i < currentSyncSample.length; i++) {
                    fragmentSamples[i] = Mp4Arrays.copyOfAndAppend(fragmentSamples[i] ,syncSamples[i][currentSyncSample[i]]);
                    nextTarget = (double)currentTicks[0] / (double)timeScale[0];
                }
                nextTarget += targetDuration;
            }

            // Advance the least advanced track
            int j = 0;
            for (int i = 0; i < currentTicks.length; i++) {
                if ((double)currentTicks[i]/timeScale[i] < (double)currentTicks[j]/timeScale[j]) {
                    j = i;
                }
            }
            if (syncSamples[j].length > currentSyncSample[j] + 1) {
                int start = currentSyncSample[j];
                int end = ++currentSyncSample[j];
                for (int i = l2i(syncSamples[j][start]); i <  syncSamples[j][end]; i++) {
                    currentTicks[j] += sampleDuration[j][i];
                }
            } else {
                break;
            }


        } while (true);
        return fragmentSamples;
    }


    private static boolean hasFoundNextCommonSyncSample(long[] tickss, long[] timescales, double nextTarget) {
        double currentTime = (double)tickss[0] / timescales[0];

        for (int i = 0; i < tickss.length; i++) {
            long ticks = tickss[i];
            long timescale = timescales[i];
            if (ticks < nextTarget * timescale) {
                // not yet far enough!
                return false;
            }
            if (Math.abs(currentTime - ((double)tickss[i] / timescales[i])) > 0.0001) {
                return false;
            }


        }
        return true;
    }



    public long[] sampleNumbers(Track track) {
        if (!allTracks.containsKey(track)) {
            throw new IllegalArgumentException("track argument needs to be contained allTracks constructor argument");
        }

        return allTracks.get(track);

    }
}
