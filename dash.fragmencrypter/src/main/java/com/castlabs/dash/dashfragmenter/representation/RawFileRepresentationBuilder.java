package com.castlabs.dash.dashfragmenter.representation;


import java.io.File;

public interface RawFileRepresentationBuilder extends RepresentationBuilder {
    String getMimeType();
    File getFile();
}
