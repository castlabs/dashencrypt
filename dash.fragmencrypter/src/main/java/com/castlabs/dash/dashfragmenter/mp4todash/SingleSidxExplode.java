package com.castlabs.dash.dashfragmenter.mp4todash;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.FileTypeBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.SegmentTypeBox;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.boxes.threegpp26244.SegmentIndexBox;
import com.googlecode.mp4parser.util.Path;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by user on 25.08.2014.
 */
public class SingleSidxExplode {
    public SingleSidxExplode() {
    }


    public void doIt(String sourceFilename, Container in, Long bitrate, List<File> segments, File outputDir, String initPattern, String mediaPattern ) throws IOException {
        String initFilename = initPattern.replace("$Bandwidth$", "" + bitrate);
        initFilename = initFilename.replace("$RepresentationID$", sourceFilename);
        File initFile = new File(outputDir, initFilename);
        segments.add(initFile);
        initFile.getParentFile().mkdirs();
        FileChannel initChannel = new FileOutputStream(initFile).getChannel();
        long sidxBase = 0;
        for (Box box : in.getBoxes()) {
            sidxBase += box.getSize();
            if (box.getType().equals("sidx")) {
                break;
            }
            box.getBox(initChannel);
        }
        initChannel.close();

        SegmentIndexBox sidx = Path.getPath(in, "sidx");
        FileTypeBox ftyp = Path.getPath(in, "ftyp");

        long start = sidxBase + sidx.getFirstOffset();
        long earliestPresentationTime = sidx.getEarliestPresentationTime();
        for (int i = 0; i < sidx.getEntries().size(); i++) {
            SegmentIndexBox.Entry entry = sidx.getEntries().get(i);
            SegmentTypeBox styp = new SegmentTypeBox();
            styp.setMajorBrand(ftyp.getMajorBrand());
            List<String> compatibleBrands = new ArrayList<String>();
            compatibleBrands.addAll(ftyp.getCompatibleBrands());
            compatibleBrands.add("msdh");
            styp.setCompatibleBrands(compatibleBrands);
            styp.setMinorVersion(ftyp.getMinorVersion());
            SegmentIndexBox localSidx = new SegmentIndexBox();
            localSidx.getEntries().add(entry);
            localSidx.setEarliestPresentationTime(earliestPresentationTime);

            String filename = mediaPattern.replace("$Bandwidth$", "" + bitrate);
            filename = filename.replace("$Time$", "" + earliestPresentationTime);
            filename = filename.replace("$Number$", "" + i);
            filename = filename.replace("$RepresentationID$", sourceFilename);

            File segmentFile = new File(outputDir, filename);
            segments.add(segmentFile);
            segmentFile.getParentFile().mkdirs();
            FileChannel fc = new FileOutputStream(segmentFile).getChannel();
            styp.getBox(fc);
            localSidx.getBox(fc);
            fc.write(in.getByteBuffer(start, entry.getReferencedSize()));
            earliestPresentationTime += entry.getSubsegmentDuration();
            start += entry.getReferencedSize();
        }
    }
}
