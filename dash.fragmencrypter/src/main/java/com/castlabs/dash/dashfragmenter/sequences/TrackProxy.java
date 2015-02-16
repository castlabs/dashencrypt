package com.castlabs.dash.dashfragmenter.sequences;

import com.coremedia.iso.boxes.CompositionTimeToSample;
import com.coremedia.iso.boxes.SampleDependencyTypeBox;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.SubSampleInformationBox;
import com.googlecode.mp4parser.authoring.Edit;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.TrackMetaData;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.GroupEntry;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The TrackProxy is used to keep a track's identity over complete run time of the tool. I can wrapped and
 * modify tracks without loosing their identity.
 */
public class TrackProxy  {
    Track target;

    public void setTarget(Track target) {
        this.target = target;
    }

    public Track getTarget() {
        return target;
    }

    public TrackProxy(Track parent) {
        this.target = parent;
    }

    public SampleDescriptionBox getSampleDescriptionBox() {
        return target.getSampleDescriptionBox();
    }

    public long[] getSampleDurations() {
        return target.getSampleDurations();
    }

    public long getDuration() {
        return target.getDuration();
    }

    public List<CompositionTimeToSample.Entry> getCompositionTimeEntries() {
        return target.getCompositionTimeEntries();
    }

    public long[] getSyncSamples() {
        return target.getSyncSamples();
    }

    public List<SampleDependencyTypeBox.Entry> getSampleDependencies() {
        return target.getSampleDependencies();
    }

    public TrackMetaData getTrackMetaData() {
        return target.getTrackMetaData();
    }

    public String getHandler() {
        return target.getHandler();
    }

    public List<Sample> getSamples() {
        return target.getSamples();
    }

    public SubSampleInformationBox getSubsampleInformationBox() {
        return target.getSubsampleInformationBox();
    }

    public String getName() {
        return target.getName() + "'";
    }

    public List<Edit> getEdits() {
        return target.getEdits();
    }

    public void close() throws IOException {
        target.close();
    }

    public Map<GroupEntry, long[]> getSampleGroups() {
        return target.getSampleGroups();
    }

    @Override
    public String toString() {
        return "->" + target.toString();
    }
}
