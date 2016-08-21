package com.castlabs.dash.dashfragmenter;

import com.coremedia.iso.boxes.TrackBox;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class FileAndTrackSelector {
    private static Map<String, String> handlerToType = new HashMap<>();
    static {
        handlerToType.put("soun", "audio");
        handlerToType.put("vide", "video");
    }
    private static Logger LOG = Logger.getLogger(FileAndTrackSelector.class.getName());

    public File file;

    public int trackId = -1;
    public String language = null;
    public String type = null;

    public boolean isSelected(TrackBox trackBox) {
        if (trackId >= 0 && trackBox.getTrackHeaderBox().getTrackId() != trackId) {
            LOG.fine("Excluding track " + trackBox.getTrackHeaderBox().getTrackId() + " of " + file + " from processing as trackId is not " + trackId);
            return false;
        }
        if (language != null && !trackBox.getMediaBox().getMediaHeaderBox().getLanguage().equals(language)) {
            LOG.fine("Excluding track " + trackBox.getTrackHeaderBox().getTrackId() + " of " + file + " from processing as language is " + trackBox.getMediaBox().getMediaHeaderBox().getLanguage() + " but not " + language + ".");
            return false;
        }
        if (type != null) {
            String handler = trackBox.getMediaBox().getHandlerBox().getHandlerType();
            if (!handlerToType.computeIfAbsent(handler, e -> e).equals(type)) {
                LOG.fine("Excluding track " + trackBox.getTrackHeaderBox().getTrackId() + " of " + file + " from processing as ^type is " + handlerToType.computeIfAbsent(handler, e -> e) + " but not " + type + ".");
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "FileAndTrackSelector{" +
                "file=" + file +
                ", trackId=" + trackId +
                ", language='" + language + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
