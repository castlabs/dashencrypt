package com.castlabs.dash.dashfragmenter.formats.csf;

import com.coremedia.iso.boxes.Box;

import java.util.List;

/**
 * Created by sannies on 21.05.2015.
 */
public interface DashTrackBuilder extends List<List<Box>> {
    public List<Box> getInitSegment();
}
