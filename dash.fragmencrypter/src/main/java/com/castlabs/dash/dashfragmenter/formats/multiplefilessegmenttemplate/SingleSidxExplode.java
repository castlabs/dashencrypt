package com.castlabs.dash.dashfragmenter.formats.multiplefilessegmenttemplate;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.FileTypeBox;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.fragment.SegmentTypeBox;
import com.googlecode.mp4parser.boxes.threegpp26244.SegmentIndexBox;
import com.googlecode.mp4parser.util.Path;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Splits a single sidx file into one file per segment.
 */
public class SingleSidxExplode {
    boolean generateStypSdix = true;
    Logger l;

    public SingleSidxExplode(Logger l) {
        this.l = l;
    }

    public void setGenerateStypSdix(boolean generateStypSdix) {
        this.generateStypSdix = generateStypSdix;
    }

    public List<File> doIt(Container in, String representationId, Long bitrate, File outputDir, String initPattern, String mediaPattern) throws IOException {
        MediaHeaderBox mdhd = Path.getPath(in, "/moov[0]/trak[0]/mdia[0]/mdhd[0]");

        List<File> segments = new ArrayList<File>();
        String initFilename = initPattern.replace("$Bandwidth$", "" + bitrate);
        initFilename = initFilename.replace("$RepresentationID$", representationId);
        initFilename = initFilename.replace("%lang%", mdhd.getLanguage());

        File initFile = new File(outputDir, initFilename);
        FileUtils.forceMkdir(initFile.getParentFile());
        l.info("Writing " + representationId + " to " + initFile.getParentFile() + "...");
        segments.add(initFile);

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
            String filename = mediaPattern.replace("$Bandwidth$", "" + bitrate);
            filename = filename.replace("$Time$", "" + earliestPresentationTime);
            filename = filename.replace("$Number$", "" + i);
            filename = filename.replace("$RepresentationID$", representationId);
            filename = filename.replace("%lang%", mdhd.getLanguage());

            File segmentFile = new File(outputDir, filename);
            FileUtils.forceMkdir(segmentFile.getParentFile());
            segments.add(segmentFile);

            FileChannel fc = new FileOutputStream(segmentFile).getChannel();
            SegmentIndexBox.Entry entry = sidx.getEntries().get(i);

            if (generateStypSdix) {

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

                styp.getBox(fc);
                localSidx.getBox(fc);
            }
            fc.write(in.getByteBuffer(start, entry.getReferencedSize()));
            earliestPresentationTime += entry.getSubsegmentDuration();
            start += entry.getReferencedSize();
        }
        return segments;
    }
}
