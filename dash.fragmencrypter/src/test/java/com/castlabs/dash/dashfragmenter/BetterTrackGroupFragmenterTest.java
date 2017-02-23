package com.castlabs.dash.dashfragmenter;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Created by sannies on 13.01.17.
 */
public class BetterTrackGroupFragmenterTest {
    @Test
    public void testSameTimescaleSameSyncSamples() {
        long[] timeScale = new long[]{25,25};
        long[][] syncSamples = new long[][]
                {
                        {1,26,51,76},
                        {1,26,51,76},
                };


        long[][] sampleDuration = new long[][] {
               generateArray(100, 1),
               generateArray(100, 1),
        };
        double targetDuration = 1.0;

        long[][] result =  BetterTrackGroupFragmenter.getCommonSyncSamples(timeScale, syncSamples, sampleDuration, targetDuration);

        for (long[] longs : result) {
            for (long aLong : longs) {
                System.err.print(aLong + ", ");
            }
            System.err.println();
        }

        long[][] expectedResult = new long[][]
                {
                        {1,26,51,76},
                        {1,26,51,76},
                };


        for (int i = 0; i < result.length; i++) {
            Assert.assertArrayEquals(expectedResult[i], result[i]);

        }
    }

    @Test
    public void testSameTimescaleSomeSyncSamplesMissing() {
        long[] timeScale = new long[]{25,25};
        long[][] syncSamples = new long[][]
                {
                        {1,26,76},
                        {1,26,51,76},
                };


        long[][] sampleDuration = new long[][] {
               generateArray(100, 1),
               generateArray(100, 1),
        };
        double targetDuration = 1.0;

        long[][] result =  BetterTrackGroupFragmenter.getCommonSyncSamples(timeScale, syncSamples, sampleDuration, targetDuration);

        for (long[] longs : result) {
            for (long aLong : longs) {
                System.err.print(aLong + ", ");
            }
            System.err.println();
        }

        long[][] expectedResult = new long[][]
                {
                        {1,26,76},
                        {1,26,76},
                };


        for (int i = 0; i < result.length; i++) {
            Assert.assertArrayEquals(expectedResult[i], result[i]);

        }
    }

    @Test
    public void testDiffTimescaleSameFps() {
        long[] timeScale = new long[]{50,25};
        long[][] syncSamples = new long[][]
                {
                        {1,26,51,76},
                        {1,26,51,76},
                };


        long[][] sampleDuration = new long[][] {
               generateArray(100, 2),
               generateArray(100, 1),
        };
        double targetDuration = 1.0;

        long[][] result =  BetterTrackGroupFragmenter.getCommonSyncSamples(timeScale, syncSamples, sampleDuration, targetDuration);

        for (long[] longs : result) {
            for (long aLong : longs) {
                System.err.print(aLong + ", ");
            }
            System.err.println();
        }

        long[][] expectedResult = new long[][]
                {
                        {1,26,51,76},
                        {1,26,51,76},
                };


        for (int i = 0; i < result.length; i++) {
            Assert.assertArrayEquals(expectedResult[i], result[i]);

        }
    }

    @Test
    public void testDifferentFps() {
        long[] timeScale = new long[]{25,50};
        long[][] syncSamples = new long[][]
                {
                        {1,26,51,76},
                        {1,51,101,151},
                };


        long[][] sampleDuration = new long[][] {
               generateArray(100, 1),
               generateArray(200, 1),
        };
        double targetDuration = 1.0;

        long[][] result =  BetterTrackGroupFragmenter.getCommonSyncSamples(timeScale, syncSamples, sampleDuration, targetDuration);

        for (long[] longs : result) {
            for (long aLong : longs) {
                System.err.print(aLong + ", ");
            }
            System.err.println();
        }

        long[][] expectedResult = new long[][]
                {
                        {1,26,51,76},
                        {1,51,101,151},
                };


        for (int i = 0; i < result.length; i++) {
            Assert.assertArrayEquals(expectedResult[i], result[i]);

        }
    }

    @Test
    public void testSlightOffset() {
        long[] timeScale = new long[]{25, 25};
        long[][] syncSamples = new long[][]
                {
                        new long[30],
                        new long[30],
                };

        for (long[] syncSample : syncSamples) {
            long t = 1;
            for (int i = 0; i < syncSample.length; i++) {
                syncSample[i] = t;
                t+=26;
            }
        }

        long[][] sampleDuration = new long[][]{
                generateArray(755, 1),
                generateArray(755, 1),
        };
        double targetDuration = 2.0;

        long[][] result = BetterTrackGroupFragmenter.getCommonSyncSamples(timeScale, syncSamples, sampleDuration, targetDuration);

/*        for (long[] longs : result) {
            for (long aLong : longs) {
                System.err.print(aLong + ", ");
            }
            System.err.println();
        }
*/
        long[][] expectedResult = new long[][]
                {
                        {1, 53, 105, 157, 209, 261, 313, 365, 417, 469, 521, 573, 625, 677, 729},
                        {1, 53, 105, 157, 209, 261, 313, 365, 417, 469, 521, 573, 625, 677, 729},
                };


        for (int i = 0; i < result.length; i++) {
            Assert.assertArrayEquals(expectedResult[i], result[i]);

        }
    }

    @Test
    public void test() {
        long[] timeScale = new long[]{25,25};
        long[][] syncSamples = new long[][]
                {
                        {1,26,51,76},
                        {1,26,51,76},
                };


        long[][] sampleDuration = new long[][] {
               generateArray(100, 1),
               generateArray(100, 1),
        };
        double targetDuration = 1.0;

        long[][] result =  BetterTrackGroupFragmenter.getCommonSyncSamples(timeScale, syncSamples, sampleDuration, targetDuration);

        for (long[] longs : result) {
            for (long aLong : longs) {
                System.err.print(aLong + ", ");
            }
            System.err.println();
        }

        long[][] expectedResult = new long[][]
                {
                        {1,26,51,76},
                        {1,26,51,76},
                };


        for (int i = 0; i < result.length; i++) {
            Assert.assertArrayEquals(expectedResult[i], result[i]);

        }
    }


    long[] generateArray(int len, long content) {
        long[] arr = new long[len];
        Arrays.fill(arr, content);
        return arr;
    }
}