package com.castlabs.dash.helpers;

import com.castlabs.dash.dashfragmenter.representation.RepresentationBuilder;
import com.coremedia.iso.boxes.Container;
import mpegDashSchemaMpd2011.RepresentationType;
import mpegDashSchemaMpd2011.SegmentTimelineType;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.logging.Logger;

import static com.castlabs.dash.helpers.ManifestHelper.templateReplace;

public class RepresentationBuilderToFile {
    private static Logger LOG = Logger.getLogger(RepresentationBuilderToFile.class.getName());

    public static void writeOnDemand(RepresentationBuilder representationBuilder, RepresentationType representation, File outputDir) throws IOException {
        assert representation.getBaseURLArray().length == 1;
        assert representation.getBaseURLArray()[0].getStringValue() != null && !"".equals(representation.getBaseURLArray()[0].getStringValue());
        File f = new File(outputDir, representation.getBaseURLArray()[0].getStringValue());
        LOG.info("Writing " + f.getAbsolutePath());
        WritableByteChannel wbc = Channels.newChannel(new FileOutputStream(f));
        LOG.fine("Writing init segment");
        representationBuilder.getInitSegment().writeContainer(wbc);
        LOG.fine("Writing index segment");
        representationBuilder.getIndexSegment().writeContainer(wbc);

        LOG.fine("Writing segments");
        for (Container fragment : representationBuilder) {
            fragment.writeContainer(wbc);
        }
        wbc.close();
    }


    public static void writeLive(RepresentationBuilder representationBuilder, RepresentationType representation, File outputDirectory) throws IOException {
        String initPattern = representation.getSegmentTemplate().getInitialization2();
        initPattern = templateReplace(initPattern, representation.getId(), 0, representation.getBandwidth(), 0);
        File initFile = new File(outputDirectory, initPattern);
        FileUtils.forceMkdir(initFile.getParentFile());
        LOG.info("Writing init file " + initFile.getAbsolutePath());
        WritableByteChannel wbc = Channels.newChannel(new FileOutputStream(initFile));
        representationBuilder.getInitSegment().writeContainer(wbc);
        wbc.close();
        long time = 0;
        long number = representation.getSegmentTemplate().getStartNumber();
        Iterator<Container> segments = representationBuilder.iterator();
        LOG.info("Writing segment files " + representation.getSegmentTemplate().getMedia().replace("$RepresentationID$", representation.getId()));
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
                while (r > 0) {
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
