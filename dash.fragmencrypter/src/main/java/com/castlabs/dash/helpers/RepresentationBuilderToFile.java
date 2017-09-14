package com.castlabs.dash.helpers;

import com.castlabs.dash.dashfragmenter.representation.Mp4RepresentationBuilder;
import com.castlabs.dash.dashfragmenter.representation.RawFileRepresentationBuilder;
import com.castlabs.dash.dashfragmenter.representation.RepresentationBuilder;
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

    public static long writeOnDemand(RepresentationBuilder representationBuilder, RepresentationType representation, File outputDir) throws IOException {
        assert representation.getBaseURLArray().length == 1;
        assert representation.getBaseURLArray()[0].getStringValue() != null && !"".equals(representation.getBaseURLArray()[0].getStringValue());

        if (representationBuilder instanceof Mp4RepresentationBuilder) {
            File outFile = new File(outputDir, representation.getBaseURLArray()[0].getStringValue());
            Mp4RepresentationBuilder mp4RepresentationBuilder = (Mp4RepresentationBuilder)representationBuilder;

            LOG.info("Writing " + outFile.getAbsolutePath());
            WritableByteChannel wbc = Channels.newChannel(new FileOutputStream(outFile));
            LOG.fine("Writing init segment");
            mp4RepresentationBuilder.getInitSegment().writeContainer(wbc);
            LOG.fine("Writing index segment");
            mp4RepresentationBuilder.getIndexSegment().writeContainer(wbc);
            LOG.fine("Writing segments");
            for (org.mp4parser.Container fragment : mp4RepresentationBuilder) {
                fragment.writeContainer(wbc);
            }
            wbc.close();
            return outFile.length();
        } else if (representationBuilder instanceof RawFileRepresentationBuilder) {
            File outFile = new File(outputDir, representation.getBaseURLArray()[0].getStringValue());
            FileUtils.copyFile(((RawFileRepresentationBuilder) representationBuilder).getFile(), outFile);
            return outFile.length();
        }
        throw new RuntimeException();
    }
}
