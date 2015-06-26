package com.castlabs.dash.dashfragmenter.representation;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * Created by sannies on 15.06.2015.
 */
public class ListContainer implements Container {
    List<Box> boxes = Collections.emptyList();

    public ListContainer(List<Box> boxes) {
        this.boxes = boxes;
    }

    public List<Box> getBoxes() {
        return boxes;
    }

    public void setBoxes(List<Box> boxes) {
        this.boxes = boxes;
    }

    public <T extends Box> List<T> getBoxes(Class<T> clazz) {
        List<T> boxesToBeReturned = null;
        T oneBox = null;
        List<Box> boxes = getBoxes();
        for (Box boxe : boxes) {
            //clazz.isInstance(boxe) / clazz == boxe.getClass()?
            // I hereby finally decide to use isInstance

            if (clazz.isInstance(boxe)) {
                if (oneBox == null) {
                    oneBox = (T) boxe;
                } else {
                    if (boxesToBeReturned == null) {
                        boxesToBeReturned = new ArrayList<T>(2);
                        boxesToBeReturned.add(oneBox);
                    }
                    boxesToBeReturned.add((T) boxe);
                }
            }
        }
        if (boxesToBeReturned != null) {
            return boxesToBeReturned;
        } else if (oneBox != null) {
            return Collections.singletonList(oneBox);
        } else {
            return Collections.emptyList();
        }
    }

    public <T extends Box> List<T> getBoxes(Class<T> clazz, boolean recursive) {
        List<T> boxesToBeReturned = new ArrayList<T>(2);
        List<Box> boxes = getBoxes();
        for (Box boxe : boxes) {
            //clazz.isInstance(boxe) / clazz == boxe.getClass()?
            // I hereby finally decide to use isInstance

            if (clazz.isInstance(boxe)) {
                boxesToBeReturned.add((T) boxe);
            }

            if (recursive && boxe instanceof Container) {
                boxesToBeReturned.addAll(((Container) boxe).getBoxes(clazz, recursive));
            }
        }
        return boxesToBeReturned;

    }

    public ByteBuffer getByteBuffer(long rangeStart, long size) throws IOException {
        ByteBuffer out = ByteBuffer.allocate(l2i(size));
        long rangeEnd = rangeStart + size;
        long boxStart;
        long boxEnd = 0;
        for (Box box : boxes) {
            boxStart = boxEnd;
            boxEnd = boxStart + box.getSize();
            if (!(boxEnd <= rangeStart || boxStart >= rangeEnd)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                WritableByteChannel wbc = Channels.newChannel(baos);
                box.getBox(wbc);
                wbc.close();

                if (boxStart >= rangeStart && boxEnd <= rangeEnd) {
                    out.put(baos.toByteArray());
                    // within -> use full box
                } else if (boxStart < rangeStart && boxEnd > rangeEnd) {
                    // around -> use 'middle' of box
                    int length = l2i(box.getSize() - (rangeStart - boxStart) - (boxEnd - rangeEnd));
                    out.put(baos.toByteArray(), l2i(rangeStart - boxStart), length);
                } else if (boxStart < rangeStart && boxEnd <= rangeEnd) {
                    // endwith
                    int length = l2i(box.getSize() - (rangeStart - boxStart));
                    out.put(baos.toByteArray(), l2i(rangeStart - boxStart), length);
                } else if (boxStart >= rangeStart && boxEnd > rangeEnd) {
                    int length = l2i(box.getSize() - (boxEnd - rangeEnd));
                    out.put(baos.toByteArray(), 0, length);
                }
            }
        }
        return (ByteBuffer) out.rewind();
    }

    public void writeContainer(WritableByteChannel bb) throws IOException {
        for (Box box : boxes) {
            box.getBox(bb);
        }
    }
}
