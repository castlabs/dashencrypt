package com.castlabs.dash.helpers;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.googlecode.mp4parser.util.Path;

import java.io.IOException;
import java.util.List;

/**
 * Created by sannies on 27.09.2014.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        IsoFile isoFile = new IsoFile(args[0]);
        List<TrackHeaderBox> tkhds = Path.getPaths(isoFile, "/moov/trak/tkhd");
        for (TrackHeaderBox tkhd : tkhds) {
            System.err.println(tkhd.getHeight() + "x" + tkhd.getWidth());
        }
    }
}
