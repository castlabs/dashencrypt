package com.castlabs.dash.dashfragmenter.representation;

import com.castlabs.dash.helpers.DashHelper;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.util.Mp4Arrays;
import com.mp4parser.iso23001.part7.ProtectionSystemSpecificHeaderBox;
import mpegDashSchemaMpd2011.DescriptorType;
import mpegDashSchemaMpd2011.RepresentationType;
import mpegDashSchemaMpd2011.SegmentBaseType;
import mpegDashSchemaMpd2011.URLType;

import java.util.ArrayList;
import java.util.List;

import static com.castlabs.dash.helpers.ManifestHelper.convertFramerate;

public class SyncSampleAssistedRepresentationBuilder extends AbstractRepresentationBuilder {


    public static long[] calcStartSamples(Track track, int minFragmentSamples) {
        long ss[] = track.getSyncSamples();
        if (ss == null || ss.length ==0) {
            long sampleNo = 1;
            long startSamples[] = new long[]{};
            int sampleCount = track.getSamples().size();
            while (sampleNo <= sampleCount) {
                startSamples = Mp4Arrays.copyOfAndAppend(startSamples, sampleNo);
                sampleNo += minFragmentSamples;
            }
            return startSamples;
        } else {
            long startSamples[] = new long[]{ss[0]};
            for (long s : ss) {
                if (startSamples[startSamples.length - 1] + minFragmentSamples <= s) {
                    startSamples = Mp4Arrays.copyOfAndAppend(startSamples, s);
                }
            }
            return startSamples;
        }
    }

    public SyncSampleAssistedRepresentationBuilder(Track track, String source, int minFragmentSamples, List<ProtectionSystemSpecificHeaderBox> psshs) {
        super(track, psshs, source, calcStartSamples(track, minFragmentSamples), calcStartSamples(track, minFragmentSamples));

    }





}

