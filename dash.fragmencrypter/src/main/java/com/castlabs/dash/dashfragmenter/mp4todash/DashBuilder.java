/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.dashfragmenter.mp4todash;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.CompositionTimeToSample;
import com.coremedia.iso.boxes.EditListBox;
import com.coremedia.iso.boxes.FileTypeBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentHeaderBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.FragmentedMp4Builder;
import com.googlecode.mp4parser.boxes.threegpp26244.SegmentIndexBox;
import com.googlecode.mp4parser.util.Path;

import java.util.*;

/**
 * Creates a fragmented Dash conforming MP4 file.
 */
public class DashBuilder extends FragmentedMp4Builder {

    public DashBuilder() {
    }

    @Override
    protected List<Box> createMoofMdat(Movie movie) {
        List<Box> moofsMdats = new LinkedList<Box>();
        HashMap<Track, long[]> intersectionMap = new HashMap<Track, long[]>();

        int maxNumberOfFragments = 0;
        for (Track track : movie.getTracks()) {
            long[] intersects = intersectionFinder.sampleNumbers(track);
            intersectionMap.put(track, intersects);
            maxNumberOfFragments = Math.max(maxNumberOfFragments, intersects.length);
        }


        int sequence = 1;
        // this loop has two indices:

        for (int cycle = 0; cycle < maxNumberOfFragments; cycle++) {

            final List<Track> sortedTracks = sortTracksInSequence(movie.getTracks(), cycle, intersectionMap);

            for (Track track : sortedTracks) {
                if (getAllowedHandlers().isEmpty() || getAllowedHandlers().contains(track.getHandler())) {
                    long[] startSamples = intersectionMap.get(track);
                    sequence = createFragment(moofsMdats, track, startSamples, cycle, sequence);
                }
            }
        }

        List<SegmentIndexBox> sidx_boxes = new LinkedList<SegmentIndexBox>();

        int inserter = 0;
        List<Box> newboxes = new ArrayList<Box>();
        int counter = 0;
        SegmentIndexBox sidx = new SegmentIndexBox();

        for (int i = 0; i < moofsMdats.size(); i++) {

            if (moofsMdats.get(i).getType().equals("sidx")) {
                sidx_boxes.add((SegmentIndexBox) moofsMdats.get(i));
                counter++;
                if (counter == 1) {
                    inserter = i;
                }
            } else {
                newboxes.add(moofsMdats.get(i));
            }
        }
        long earliestPresentationTime = sidx_boxes.get(0).getEarliestPresentationTime();
        if ( earliestPresentationTime < 0) {
            System.err.println("negative earlist_presentation_time in sidx. Setting to 0. May cause sync issues");
            earliestPresentationTime = 0;
        }
        sidx.setEarliestPresentationTime(earliestPresentationTime);
        sidx.setFirstOffset(sidx_boxes.get(0).getFirstOffset());
        sidx.setReferenceId(sidx_boxes.get(0).getReferenceId());
        sidx.setTimeScale(sidx_boxes.get(0).getTimeScale());
        sidx.setFlags(sidx_boxes.get(0).getFlags());
        List<SegmentIndexBox.Entry> sidxbox_entries = new ArrayList<SegmentIndexBox.Entry>();
        for (SegmentIndexBox sidxbox : sidx_boxes) {
            List<SegmentIndexBox.Entry> entryfrag = sidxbox.getEntries();
            for (SegmentIndexBox.Entry entry : entryfrag) {
                sidxbox_entries.add(entry);
            }
        }

        sidx.setEntries(sidxbox_entries);
        newboxes.add(inserter, sidx);
        return newboxes;
    }

    @Override
    protected int createFragment(List<Box> moofsMdats, Track track, long[] startSamples, int cycle, int sequence) {
        List<Box> moofMdat = new ArrayList<Box>();
        int newSequence = super.createFragment(moofMdat, track, startSamples, cycle, sequence);

        if (moofMdat.isEmpty()) return newSequence;

        final MovieFragmentBox moof = (MovieFragmentBox) moofMdat.get(0);
        final TrackRunBox trun = moof.getTrackRunBoxes().get(0);
        final long[] ptss = getPtss(trun);
        final int firstFrameSapType = getFirstFrameSapType(ptss);
        long firstOffset = 0;
        int referencedSize = (int) (moof.getSize() + moofMdat.get(1).getSize());
        long timeMappingEdit = getTimeMappingEditTime(track);
        final Box sidx = createSidx(track, ptss[0] - timeMappingEdit, firstOffset, referencedSize, getSegmentDuration(trun), (byte) firstFrameSapType, 0);

        moofsMdats.add(sidx);
        moofsMdats.addAll(moofMdat);

        return newSequence;
    }

    private long getTimeMappingEditTime(Track track) {
        final EditListBox editList = track.getTrackMetaData().getEditList();
        if (editList != null) {
            final List<EditListBox.Entry> entries = editList.getEntries();
            for (EditListBox.Entry entry : entries) {
                if (entry.getMediaTime() > 0) {
                    return entry.getMediaTime();
                }
            }
        }
        return 0;
    }


    protected long getSegmentDuration(TrackRunBox trun) {
        final List<TrackRunBox.Entry> trunEntries = trun.getEntries();
        long duration = 0;
        for (TrackRunBox.Entry trunEntry : trunEntries) {
            duration += trunEntry.getSampleDuration();
        }
        return duration;
    }

    protected int getFirstFrameSapType(long[] ptss) {
        long idrTimeStamp = ptss[0];
        Arrays.sort(ptss);
        if (idrTimeStamp > ptss[0]) {
            return 0;
        } else {
            return 1;
        }
    }

    private long[] getPtss(TrackRunBox trun) {
        long currentTime = 0;
        long[] ptss = new long[trun.getEntries().size()];
        for (int j = 0; j < ptss.length; j++) {
            ptss[j] = currentTime + trun.getEntries().get(j).getSampleCompositionTimeOffset();
            currentTime += trun.getEntries().get(j).getSampleDuration();
        }
        return ptss;
    }


    /**
     * Creates a 'sidx' box
     */
    protected Box createSidx(Track track, long earliestPresentationTime, long firstOffset, int referencedSize, long subSegmentDuration, byte sap, int sapDelta) {
        SegmentIndexBox sidx = new SegmentIndexBox();

        sidx.setEarliestPresentationTime(earliestPresentationTime);
        sidx.setFirstOffset(firstOffset);
        sidx.setReferenceId(track.getTrackMetaData().getTrackId());
        sidx.setTimeScale(track.getTrackMetaData().getTimescale());
        sidx.setFlags(0);
        sidx.setReserved(0);
        SegmentIndexBox.Entry sidxentry = createSidxEntry(referencedSize, subSegmentDuration, sap, sapDelta);

        ArrayList<SegmentIndexBox.Entry> sidxEntries = new ArrayList<SegmentIndexBox.Entry>();
        sidxEntries.add(sidxentry);
        sidx.setEntries(sidxEntries);
        return sidx;
    }

    private SegmentIndexBox.Entry createSidxEntry(int referencedSize, long subSegmentDuration, byte sapType, int sapDelta) {
        SegmentIndexBox.Entry sidxentry = new SegmentIndexBox.Entry();
        byte referenceType = 0; //media
        byte startWithSAP;
        int sapDeltaTime;
        if (sapType == 1) {
            startWithSAP = 1; // fragments are cut at I-Frames
            sapDeltaTime = 0;
        } else {
            //todo fix
            //if (true) throw new RuntimeException("can't handle other than sap_type 1 properly");
            startWithSAP = 0;
            sapDeltaTime = sapDelta;
        }
        sidxentry.setReferenceType(referenceType);
        sidxentry.setReferencedSize(referencedSize);
        sidxentry.setSubsegmentDuration(subSegmentDuration);
        sidxentry.setStartsWithSap(startWithSAP);
        sidxentry.setSapType(sapType);
        sidxentry.setSapDeltaTime(sapDeltaTime);
        return sidxentry;
    }

    @Override
    public Box createFtyp(Movie movie) {
        List<String> minorBrands = new LinkedList<String>();
        minorBrands.add("mp42");
        minorBrands.add("dash");
        minorBrands.add("msdh");
        minorBrands.add("msix");
        minorBrands.add("iso6");
        minorBrands.add("avc1");
        minorBrands.add("isom");

        return new FileTypeBox("iso6", 1, minorBrands);
    }


}
