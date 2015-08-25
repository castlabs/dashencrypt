package com.castlabs.dash.dashfragmenter.tracks;

import com.castlabs.dash.helpers.BoxHelper;
import com.castlabs.dash.helpers.DashHelper;
import com.coremedia.iso.boxes.CompositionTimeToSample;
import com.googlecode.mp4parser.authoring.Edit;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.WrappingTrack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by sannies on 04.11.2014.
 */
public class NegativeCtsInsteadOfEdit extends WrappingTrack {
    Track original;
    List<CompositionTimeToSample.Entry> ctts;
    List<Edit> edits;

    public NegativeCtsInsteadOfEdit(Track parent) {
        super(parent);
        this.original = parent;

        if (parent.getCompositionTimeEntries() != null && parent.getCompositionTimeEntries().size() > 0) {
            long currentTime = 0;
            int[] ptss = Arrays.copyOfRange(CompositionTimeToSample.blowupCompositionTimes(parent.getCompositionTimeEntries()), 0, 50);
            for (int j = 0; j < ptss.length; j++) {
                ptss[j] += currentTime;
                currentTime += parent.getSampleDurations()[j];
            }
            Arrays.sort(ptss);

            ctts = parent.getCompositionTimeEntries();

            for (CompositionTimeToSample.Entry ctt : ctts) {
                ctt.setOffset(ctt.getOffset() - ptss[0]);
            }
            List<Edit> orgEdits = original.getEdits();

            double earliestTrackPresentationTime = DashHelper.getEarliestTrackPresentationTime(orgEdits);

            double adjustedStartTime = earliestTrackPresentationTime + (double)ptss[0] / original.getTrackMetaData().getTimescale();
            edits = BoxHelper.getEdits(original, adjustedStartTime);
        }
    }


    @Override
    public List<CompositionTimeToSample.Entry> getCompositionTimeEntries() {
        return ctts;
    }

    @Override
    public List<Edit> getEdits() {
        return edits;
    }

    public static boolean benefitsFromChange(Track track) {

        List<Edit> edits = track.getEdits();
        double decodingStartInMovie = DashHelper.getEarliestTrackPresentationTime(edits);

        if (decodingStartInMovie != 0) {
            double presentationStartInTrack = 0;
            if (track.getCompositionTimeEntries() != null && track.getCompositionTimeEntries().size() > 0) {
                long currentTime = 0;
                int[] ptss = Arrays.copyOfRange(CompositionTimeToSample.blowupCompositionTimes(track.getCompositionTimeEntries()), 0, 50);
                for (int j = 0; j < ptss.length; j++) {
                    ptss[j] += currentTime;
                    currentTime += track.getSampleDurations()[j];
                }
                Arrays.sort(ptss);
                presentationStartInTrack = (double) ptss[0] / track.getTrackMetaData().getTimescale();
            }
            if (presentationStartInTrack + decodingStartInMovie == 0) {
                return true;
            } else {
                return false;
            }

        } else {
            return false;
        }
    }

    @Override
    public String getName() {
        return "NegativeCtsInsteadOfEdit(" + this.original.getName() + ")";
    }
}
