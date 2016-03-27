package com.castlabs.dash.dashfragmenter.representation;

import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.BetterFragmenter;
import com.mp4parser.iso23001.part7.ProtectionSystemSpecificHeaderBox;

import java.util.List;

public class SyncSampleAssistedRepresentationBuilder extends AbstractRepresentationBuilder {

    public SyncSampleAssistedRepresentationBuilder(Track track, String source, double minFragmentTime, List<ProtectionSystemSpecificHeaderBox> psshs) {
        super(track, psshs, source, new BetterFragmenter(minFragmentTime).sampleNumbers(track), new BetterFragmenter(minFragmentTime).sampleNumbers(track));
    }


}

