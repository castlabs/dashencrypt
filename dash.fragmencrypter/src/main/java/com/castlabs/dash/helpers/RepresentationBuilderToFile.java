package com.castlabs.dash.helpers;

import com.castlabs.dash.dashfragmenter.representation.RepresentationBuilder;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.googlecode.mp4parser.util.Path;
import mpegDashSchemaMpd2011.RepresentationType;
import mpegDashSchemaMpd2011.SegmentTimelineType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.List;

import static com.castlabs.dash.helpers.ManifestHelper.templateReplace;
import static com.castlabs.dash.helpers.Timing.getDuration;

public class RepresentationBuilderToFile {
    public static void writeOnDemand(RepresentationBuilder representationBuilder, RepresentationType representation, File outputDir) throws IOException {
        assert representation.getBaseURLArray().length == 1;
        assert representation.getBaseURLArray()[0].getStringValue() != null && !"".equals(representation.getBaseURLArray()[0].getStringValue());
        WritableByteChannel wbc = Channels.newChannel(new FileOutputStream(representation.getBaseURLArray()[0].getStringValue()));
        representationBuilder.getInitSegment().writeContainer(wbc);
        representationBuilder.getIndexSegment().writeContainer(wbc);

        for (Container fragment : representationBuilder) {
            fragment.writeContainer(wbc);
        }
        wbc.close();
    }


    public static void writeLive(RepresentationBuilder representationBuilder, RepresentationType representation, File outputDirectory) throws IOException {
        String initPattern = representation.getSegmentTemplate().getInitialization2();
        initPattern = templateReplace(initPattern,representation.getId(), 0, representation.getBandwidth(), 0);
        File initFile = new File(outputDirectory, initPattern);
        FileUtils.forceMkdir(initFile.getParentFile());
        WritableByteChannel wbc = Channels.newChannel(new FileOutputStream(initFile));
        representationBuilder.getInitSegment().writeContainer(wbc);
        wbc.close();
        long time = 0;
        long number = representation.getSegmentTemplate().getStartNumber();
        Iterator<Container> segments = representationBuilder.iterator();

        for (SegmentTimelineType.S s : representation.getSegmentTemplate().getSegmentTimeline().getSArray()) {
            if (s.isSetT()) {
                time = s.getT().longValue();
            }
            String segmentFilename =
                    templateReplace(
                            representation.getSegmentTemplate().getMedia(),
                            representation.getId(), number, representation.getBandwidth(), time);
            Container segment = segments.next();
            File segmentFile = new File(outputDirectory, segmentFilename);
            FileUtils.forceMkdir(segmentFile.getParentFile());

            WritableByteChannel swbc = new FileOutputStream(segmentFile).getChannel();
            segment.writeContainer(swbc);
            swbc.close();
            time += s.getD().longValue();
            number += 1;

            if (s.isSetR()) {
                long r = s.getR().longValue();
                while (r>0) {
                    String segmentFilenameRepeater =
                            templateReplace(
                                    representation.getSegmentTemplate().getMedia(),
                                    representation.getId(), number, representation.getBandwidth(), time);
                    Container segmentRepeater = segments.next();
                    File segmentFileRepeater = new File(outputDirectory, segmentFilenameRepeater);
                    FileUtils.forceMkdir(segmentFileRepeater.getParentFile());

                    WritableByteChannel swbcRepeater = new FileOutputStream(segmentFileRepeater).getChannel();
                    segmentRepeater.writeContainer(swbcRepeater);
                    swbcRepeater.close();
                    time += s.getD().longValue();
                    number += 1;
                    r -= 1;
                }
            }
        }

    }
}
