package com.castlabs.dash.dashfragmenter.representation;

import com.castlabs.dash.helpers.DashHelper;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultFragmenterImpl;
import com.googlecode.mp4parser.authoring.builder.Fragmenter;
import com.googlecode.mp4parser.util.Mp4Arrays;
import com.mp4parser.iso23001.part7.ProtectionSystemSpecificHeaderBox;
import mpegDashSchemaMpd2011.DescriptorType;
import mpegDashSchemaMpd2011.RepresentationType;
import mpegDashSchemaMpd2011.SegmentBaseType;
import mpegDashSchemaMpd2011.URLType;

import java.util.ArrayList;
import java.util.List;

import static com.castlabs.dash.helpers.ManifestHelper.convertFramerate;

public class SyncSampleAssistedRepresentationBuilder extends AbstractRepresentationBuilder {

    public SyncSampleAssistedRepresentationBuilder(Track track, String source, double minFragmentTime, List<ProtectionSystemSpecificHeaderBox> psshs) {
        super(track, psshs, source, new DefaultFragmenterImpl(minFragmentTime).sampleNumbers(track), new DefaultFragmenterImpl(minFragmentTime).sampleNumbers(track));
    }





}

