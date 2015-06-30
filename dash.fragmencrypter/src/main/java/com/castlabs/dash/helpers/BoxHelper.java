package com.castlabs.dash.helpers;

import com.coremedia.iso.boxes.Box;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;

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

}
