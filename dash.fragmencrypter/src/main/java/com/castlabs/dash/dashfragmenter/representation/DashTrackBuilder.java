package com.castlabs.dash.dashfragmenter.representation;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;

import java.util.List;

/**
 * Created by sannies on 21.05.2015.
 */
public interface DashTrackBuilder extends List<Container> {
    public Container getInitSegment();
    public Container getIndexSegment();
}
