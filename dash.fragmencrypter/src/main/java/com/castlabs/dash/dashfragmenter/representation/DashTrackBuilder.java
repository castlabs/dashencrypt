package com.castlabs.dash.dashfragmenter.representation;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Track;

import java.util.List;

/**
 * Created by sannies on 21.05.2015.
 */
public interface DashTrackBuilder extends List<Container> {
    public Track getPrimaryTrack();
    public Container getInitSegment();
    public Container getIndexSegment();
}
