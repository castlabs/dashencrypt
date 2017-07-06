package com.castlabs.dash.dashfragmenter.representation;

import org.mp4parser.Container;
import org.mp4parser.muxer.Track;

import java.util.List;

public interface Mp4RepresentationBuilder extends List<Container>, RepresentationBuilder {
    Track getTrack();
    Container getInitSegment();
    Container getIndexSegment();

}
