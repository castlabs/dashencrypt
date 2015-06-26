package com.castlabs.dash.dashfragmenter.representation;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Track;
import mpegDashSchemaMpd2011.RepresentationType;

import java.util.List;

/**
 * Created by sannies on 21.05.2015.
 */
public interface RepresentationBuilder extends List<Container> {
    Track getTrack();
    Container getInitSegment();
    Container getIndexSegment();
    RepresentationType getSegmentTemplateRepresentation();
}
