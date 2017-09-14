package com.castlabs.dash.helpers;



import org.mp4parser.Box;
import org.mp4parser.muxer.Edit;
import org.mp4parser.muxer.Track;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;

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

}
