package com.castlabs.dash.helpers;

import com.coremedia.iso.boxes.Box;
import com.googlecode.mp4parser.authoring.Edit;
import com.googlecode.mp4parser.authoring.Track;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sannies on 27.06.2015.
 */
public class BoxHelper {
    public static byte[] boxToBytes(Box b) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            b.getBox(Channels.newChannel(baos));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
    public static byte[] boxToBytes(org.mp4parser.Box b) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            b.getBox(Channels.newChannel(baos));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    public static List<Edit> getEdits(Track track, double adjustedStartTime) {
        final List<Edit> edits = new ArrayList<Edit>();
        if (adjustedStartTime < 0) {
            edits.add(new Edit((long) (-adjustedStartTime * track.getTrackMetaData().getTimescale()), track.getTrackMetaData().getTimescale(), 1.0, (double) track.getDuration() / track.getTrackMetaData().getTimescale()));
        } else if (adjustedStartTime > 0) {
            edits.add(new Edit(-1, track.getTrackMetaData().getTimescale(), 1.0, adjustedStartTime));
            edits.add(new Edit(0, track.getTrackMetaData().getTimescale(), 1.0, (double) track.getDuration() / track.getTrackMetaData().getTimescale()));
        }
        return edits;
    }
}
