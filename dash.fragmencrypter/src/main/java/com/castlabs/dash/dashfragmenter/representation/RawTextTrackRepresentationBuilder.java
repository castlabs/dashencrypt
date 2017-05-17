package com.castlabs.dash.dashfragmenter.representation;

import com.castlabs.dash.dashfragmenter.representation.RawFileRepresentationBuilder;
import mpegDashSchemaMpd2011.BaseURLType;
import mpegDashSchemaMpd2011.RepresentationType;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

import static com.castlabs.dash.helpers.DashHelper.filename2UrlPath;

public class RawTextTrackRepresentationBuilder implements RawFileRepresentationBuilder {

    private String source;
    private File file;

    public RawTextTrackRepresentationBuilder(String source, File file) {
        this.source = source;
        this.file = file;
    }

    @Override
    public RepresentationType getOnDemandRepresentation() {
        return getLiveRepresentation();
    }

    @Override
    public RepresentationType getLiveRepresentation() {
        RepresentationType representation = RepresentationType.Factory.newInstance();
        representation.setId(filename2UrlPath(FilenameUtils.getBaseName(getSource()) + "-" + FilenameUtils.getExtension(getSource())));
        representation.setBandwidth(0); // pointless - just invent a small number
        representation.setMimeType(getMimeType());
        if (getCodec() != null) {
            representation.setCodecs(getCodec());
        }
        BaseURLType baseURL = representation.addNewBaseURL();
        baseURL.setStringValue(getSource());
        return representation;
    }


    @Override
    public String getSource() {
        return source;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public String getMimeType() {

        if (source.endsWith(".dfxp")) {
            return "application/ttaf+xml";
        } else if (source.endsWith(".xml") || source.endsWith(".ttml")) {
            return "application/ttml+xml";
        } else if (source.endsWith(".vtt")) {
            return "text/vtt";

        } else {
            throw new RuntimeException("Not sure what kind of textTrack " + source + " is.");
        }
    }

    @Override
    public String getCodec() {
        return null;
    }
}
