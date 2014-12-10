package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.Command;
import com.castlabs.dash.dashfragmenter.ExitCodeException;
import com.coremedia.iso.IsoFile;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.SampleToGroupBox;
import com.googlecode.mp4parser.util.Path;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.spi.FileOptionHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by sannies on 08.12.2014.
 */
public class Test implements Command {

    @Argument(required = true, multiValued = true, handler = FileOptionHandler.class, usage = "Bitstream input files", metaVar = "vid1.avc1, aud1.dtshd ...")
    protected List<File> inputFiles;

    public int run() throws IOException, ExitCodeException {
        for (File inputFile : inputFiles) {
            IsoFile isoFile = new IsoFile(inputFile.getAbsolutePath());
            List<SampleToGroupBox> sbgps = Path.getPaths(isoFile, "/moof/traf/sbgp");
            for (SampleToGroupBox sbgp : sbgps) {
                for (SampleToGroupBox.Entry entry : sbgp.getEntries()) {
                    entry.setGroupDescriptionIndex(entry.getGroupDescriptionIndex() & 0xFF);
                }
            }
            String filename = FilenameUtils.getBaseName(inputFile.getName()) + "sbgp-repaired." + FilenameUtils.getExtension(inputFile.getName());
            isoFile.writeContainer(new FileOutputStream(filename).getChannel());
        }
        return 0;
    }
}
