package com.castlabs.dash.dashfragmenter.representation;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Track;
import mpegDashSchemaMpd2011.RepresentationType;

import java.util.List;

public interface Mp4RepresentationBuilder extends List<Container>, RepresentationBuilder {
    Track getTrack();
    Container getInitSegment();
    Container getIndexSegment();

}
