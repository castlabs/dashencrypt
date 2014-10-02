/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.dashfragmenter.formats.csf;

import com.coremedia.iso.boxes.*;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentBaseMediaDecodeTimeBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.googlecode.mp4parser.BasicContainer;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.FragmentedMp4Builder;
import com.googlecode.mp4parser.boxes.threegpp26244.SegmentIndexBox;
import com.googlecode.mp4parser.util.Path;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * Creates a fragmented Dash conforming MP4 file.
 */
public class DashBuilder extends FragmentedMp4Builder {

    public DashBuilder() {
    }



    private long getTimeMappingEditTime(Container file) {
        final EditListBox editList = Path.getPath(file, "/moov[0]/trak[0]/edts[0]/elst[0]");
        final MediaHeaderBox mdhd = Path.getPath(file, "/moov[0]/trak[0]/mdia[0]/mdhd[0]");
        final MovieHeaderBox mvhd = Path.getPath(file, "/moov[0]/mvhd[0]");

        if (editList != null) {
            double editStartTime = 0;
            final List<EditListBox.Entry> entries = editList.getEntries();
            boolean acceptDwell = true;
            boolean acceptEdit = true;
            for (EditListBox.Entry edit : entries) {
                if (edit.getMediaTime() == -1 && !acceptDwell) {
                    throw new RuntimeException("Cannot accept edit list for processing (1)");
                }
                if (edit.getMediaTime() >= 0 && !acceptEdit) {
                    throw new RuntimeException("Cannot accept edit list for processing (2)");
                }
                if (edit.getMediaTime() == -1) {
                    assert mvhd != null;
                    editStartTime -= (double)edit.getSegmentDuration() / mvhd.getTimescale() ;
                } else /* if edit.getMediaTime() >= 0 */ {
                    assert mdhd != null;
                    editStartTime += (double) edit.getMediaTime() / mdhd.getTimescale();
                    acceptEdit = false;
                    acceptDwell = false;
                }
            }
            assert mdhd != null;
            return (long)(editStartTime * mdhd.getTimescale());
        }
        return 0;
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
    protected void createTrun(long startSample, long endSample, Track track, int sequenceNumber, TrackFragmentBox parent) {
        super.createTrun(startSample, endSample, track, sequenceNumber, parent);
        TrackRunBox trun = Path.getPath(parent, "trun");
        if (Path.getPath(track.getSampleDescriptionBox(), "avc.") != null) {
            List<Sample> samples = track.getSamples();
            for (int i = 0; i < endSample - startSample; i++) {
                Sample s = samples.get((int) (startSample - 1 + i));
                s.asByteBuffer();

            }
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
