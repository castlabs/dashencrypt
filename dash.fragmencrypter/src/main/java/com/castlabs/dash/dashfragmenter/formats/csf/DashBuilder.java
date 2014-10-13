/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.dashfragmenter.formats.csf;

import com.coremedia.iso.boxes.*;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.googlecode.mp4parser.BasicContainer;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.FragmentedMp4Builder;
import com.googlecode.mp4parser.boxes.threegpp26244.SegmentIndexBox;
import com.googlecode.mp4parser.util.Path;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.castlabs.dash.helpers.Timing.getPtss;
import static com.castlabs.dash.helpers.Timing.getTimeMappingEditTime;
import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * Creates a fragmented Dash conforming MP4 file.
 */
public class DashBuilder extends FragmentedMp4Builder {

    public DashBuilder() {
    }



    private SegmentIndexBox createSidx(BasicContainer isoFile, List<Box> moofMdats, long offsetBetweenSidxAndFirstMoof) {
        SegmentIndexBox sidx = new SegmentIndexBox();
        sidx.setVersion(0);
        sidx.setFlags(0);
        sidx.setReserved(0);
        sidx.setFirstOffset(offsetBetweenSidxAndFirstMoof);
        List<SegmentIndexBox.Entry> entries = sidx.getEntries();
        MovieFragmentBox firstMoof = null;
        for (Box moofMdat : moofMdats) {
            if (moofMdat.getType().equals("moof")) {
                if (firstMoof == null) {
                    firstMoof = (MovieFragmentBox) moofMdat;
                }
                entries.add(new SegmentIndexBox.Entry());
            }
        }
        TrackHeaderBox tkhd = Path.getPath(isoFile, "/moov[0]/trak[0]/tkhd[0]");
        MediaHeaderBox mdhd = Path.getPath(isoFile, "/moov[0]/trak[0]/mdia[0]/mdhd[0]");
        sidx.setReferenceId(tkhd.getTrackId());
        sidx.setTimeScale(mdhd.getTimescale());
        // we only have one
        TrackRunBox trun = firstMoof.getTrackRunBoxes().get(0);
        long[] ptss = getPtss(trun);
        Arrays.sort(ptss); // index 0 has now the earliest presentation time stamp!
        long timeMappingEdit = getTimeMappingEditTime(isoFile);
        sidx.setEarliestPresentationTime(ptss[0] - timeMappingEdit);


        // ugly code ...

        int size = 0;
        int i = 0;
        MovieFragmentBox lassMoof = null;
        for (Box moofMdat : moofMdats) {

            if (moofMdat.getType().equals("moof") && size > 0) {
                SegmentIndexBox.Entry entry = entries.get(i++);
                entry.setReferencedSize(size);
                ptss = getPtss(Path.<TrackRunBox>getPath(lassMoof, "traf[0]/trun[0]"));
                entry.setSapType(getFirstFrameSapType(ptss));
                entry.setSubsegmentDuration(getTrunDuration(Path.<TrackRunBox>getPath(lassMoof, "traf[0]/trun[0]")));
                entry.setStartsWithSap((byte) 1); // we know it - no need to lookup
                size = l2i(moofMdat.getSize());
            } else {
                size += l2i(moofMdat.getSize());
            }
            if (moofMdat.getType().equals("moof")) {
                lassMoof = (MovieFragmentBox) moofMdat;
            }

        }
        SegmentIndexBox.Entry entry = entries.get(i);
        ptss = getPtss(Path.<TrackRunBox>getPath(lassMoof, "traf[0]/trun[0]"));
        entry.setSapType(getFirstFrameSapType(ptss));
        entry.setSubsegmentDuration(getTrunDuration(Path.<TrackRunBox>getPath(lassMoof, "traf[0]/trun[0]")));
        entry.setReferencedSize(size);
        entry.setStartsWithSap((byte) 1); // we know it - no need to lookup

        return sidx;
    }

    protected long getTrunDuration(TrackRunBox trun) {
        final List<TrackRunBox.Entry> trunEntries = trun.getEntries();
        long duration = 0;
        for (TrackRunBox.Entry trunEntry : trunEntries) {
            duration += trunEntry.getSampleDuration();
        }
        return duration;
    }


    protected byte getFirstFrameSapType(long[] ptss) {
        long idrTimeStamp = ptss[0];
        Arrays.sort(ptss);
        if (idrTimeStamp > ptss[0]) {
            return 0;
        } else {
            return 1;
        }
    }


    @Override
    public Container build(Movie movie) {

        if (movie.getTracks().size() != 1) {
            throw new RuntimeException("Only onetrack allowed");
        }

        BasicContainer isoFile = new BasicContainer();
        isoFile.addBox(createFtyp(movie));
        isoFile.addBox(createMoov(movie));
        List<Box> moofMdats = createMoofMdat(movie);

        isoFile.addBox(createSidx(isoFile, moofMdats, 0));

        for (Box box : moofMdats) {
            isoFile.addBox(box);
        }
        isoFile.addBox(createMfra(movie, isoFile));

        return isoFile;
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
