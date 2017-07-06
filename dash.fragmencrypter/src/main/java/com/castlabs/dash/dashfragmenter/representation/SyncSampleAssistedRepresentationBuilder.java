package com.castlabs.dash.dashfragmenter.representation;


import org.mp4parser.boxes.iso23001.part7.ProtectionSystemSpecificHeaderBox;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.DefaultFragmenterImpl;

import java.util.List;

public class SyncSampleAssistedRepresentationBuilder extends AbstractRepresentationBuilder {

    public SyncSampleAssistedRepresentationBuilder(Track track, String source, double minFragmentTime, List<ProtectionSystemSpecificHeaderBox> psshs) {
        super(track, psshs, source, new DefaultFragmenterImpl(minFragmentTime).sampleNumbers(track), new DefaultFragmenterImpl(minFragmentTime).sampleNumbers(track));
    }


}

