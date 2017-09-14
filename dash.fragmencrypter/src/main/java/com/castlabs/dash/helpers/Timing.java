package com.castlabs.dash.helpers;

import org.mp4parser.boxes.iso14496.part12.EditListBox;
import org.mp4parser.boxes.iso14496.part12.MediaHeaderBox;
import org.mp4parser.boxes.iso14496.part12.MovieHeaderBox;
import org.mp4parser.boxes.iso14496.part12.TrackRunBox;

import java.util.List;

public class Timing {


    public static long getDuration(TrackRunBox trun) {
        long[] ptss = new long[trun.getEntries().size()];
        long duration = 0;
        for (int j = 0; j < ptss.length; j++) {
            duration += trun.getEntries().get(j).getSampleDuration();
        }
        return duration;
    }




    public static long[] getPtss(org.mp4parser.boxes.iso14496.part12.TrackRunBox trun) {
        long currentTime = 0;
        long[] ptss = new long[trun.getEntries().size()];
        for (int j = 0; j < ptss.length; j++) {
            ptss[j] = currentTime + trun.getEntries().get(j).getSampleCompositionTimeOffset();
            currentTime += trun.getEntries().get(j).getSampleDuration();
        }
        return ptss;
    }


    public static long getTimeMappingEditTime(org.mp4parser.Container file) {
        final EditListBox editList = org.mp4parser.tools.Path.getPath(file, "moov[0]/trak[0]/edts[0]/elst[0]");
        final MediaHeaderBox mdhd = org.mp4parser.tools.Path.getPath(file, "moov[0]/trak[0]/mdia[0]/mdhd[0]");
        final MovieHeaderBox mvhd = org.mp4parser.tools.Path.getPath(file, "moov[0]/mvhd[0]");

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
                    editStartTime -= (double) edit.getSegmentDuration() / mvhd.getTimescale();
                } else /* if edit.getMediaTime() >= 0 */ {
                    assert mdhd != null;
                    editStartTime += (double) edit.getMediaTime() / mdhd.getTimescale();
                    acceptEdit = false;
                    acceptDwell = false;
                }
            }
            assert mdhd != null;
            return (long) (editStartTime * mdhd.getTimescale());
        }
        return 0;
    }


}
