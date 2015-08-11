package com.castlabs.dash.dashfragmenter.representation;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Track;
import mpegDashSchemaMpd2011.RepresentationType;

import java.util.List;

public interface RepresentationBuilder extends List<Container> {
    Track getTrack();
    Container getInitSegment();
    Container getIndexSegment();
    RepresentationType getOnDemandRepresentation();
    RepresentationType getLiveRepresentation();
    String getSource();
}
