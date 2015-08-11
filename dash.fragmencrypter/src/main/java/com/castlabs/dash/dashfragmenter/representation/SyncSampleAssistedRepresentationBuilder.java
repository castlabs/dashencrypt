package com.castlabs.dash.dashfragmenter.representation;

import com.castlabs.dash.helpers.DashHelper;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.Track;
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


    public static long[] calcStartSamples(Track track, int minFragmentSamples) {
        long ss[] = track.getSyncSamples();
        if (ss == null || ss.length ==0) {
            long sampleNo = 1;
            long startSamples[] = new long[]{};
            int sampleCount = track.getSamples().size();
            while (sampleNo <= sampleCount) {
                startSamples = Mp4Arrays.copyOfAndAppend(startSamples, sampleNo);
                sampleNo += minFragmentSamples;
            }
            return startSamples;
        } else {
            long startSamples[] = new long[]{ss[0]};
            for (long s : ss) {
                if (startSamples[startSamples.length - 1] + minFragmentSamples <= s) {
                    startSamples = Mp4Arrays.copyOfAndAppend(startSamples, s);
                }
            }
            return startSamples;
        }
    }

    public SyncSampleAssistedRepresentationBuilder(Track track, String source, int minFragmentSamples, List<ProtectionSystemSpecificHeaderBox> psshs) {
        super(track, psshs, source, calcStartSamples(track, minFragmentSamples), calcStartSamples(track, minFragmentSamples));

    }


    String getCodec() {
        return DashHelper.getRfc6381Codec(theTrack.getSampleDescriptionBox().getSampleEntry());
    }


    public RepresentationType getOnDemandRepresentation() {

        RepresentationType representation = RepresentationType.Factory.newInstance();
        representation.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");
        if (theTrack.getHandler().equals("vide")) {

            long videoHeight = (long) theTrack.getTrackMetaData().getHeight();
            long videoWidth = (long) theTrack.getTrackMetaData().getWidth();
            double framesPerSecond = (double) (theTrack.getSamples().size() * theTrack.getTrackMetaData().getTimescale()) / theTrack.getDuration();


            representation.setMimeType("video/mp4");
            representation.setCodecs(getCodec());
            representation.setWidth(videoWidth);
            representation.setHeight(videoHeight);
            representation.setFrameRate(convertFramerate(framesPerSecond));
            representation.setSar("1:1");
            // too hard to find it out. Ignoring even though it should be set according to DASH-AVC-264-v2.00-hd-mca.pdf
        } else if (theTrack.getHandler().equals("soun")) {

            AudioSampleEntry ase = (AudioSampleEntry) theTrack.getSampleDescriptionBox().getSampleEntry();

            representation.setMimeType("audio/mp4");
            representation.setCodecs(DashHelper.getRfc6381Codec(ase));
            representation.setAudioSamplingRate("" + DashHelper.getAudioSamplingRate(ase));

            DescriptorType audio_channel_conf = representation.addNewAudioChannelConfiguration();
            DashHelper.ChannelConfiguration cc = DashHelper.getChannelConfiguration(ase);
            audio_channel_conf.setSchemeIdUri(cc.schemeIdUri);
            audio_channel_conf.setValue(cc.value);



        } else if (theTrack.getHandler().equals("subt")) {
            representation.setMimeType("audio/mp4");
            representation.setCodecs(getCodec());

            representation.setStartWithSAP(1);

        }

        SegmentBaseType segBaseType = representation.addNewSegmentBase();

        segBaseType.setTimescale(theTrack.getTrackMetaData().getTimescale());
        segBaseType.setIndexRangeExact(true);

        long initSize = 0;
        for (Box b : getInitSegment().getBoxes()) {
            initSize += b.getSize();
        }
        URLType initialization = segBaseType.addNewInitialization();
        long indexSize = 0;
        for (Box b : getIndexSegment().getBoxes()) {
            indexSize += b.getSize();
        }

        segBaseType.setIndexRange(String.format("%s-%s", initSize, initSize + indexSize));
        initialization.setRange(String.format("0-%s", initSize - 1));

        long size = 0;
        List<Sample> samples = theTrack.getSamples();
        for (int i = 0; i < Math.min(samples.size(), 10000); i++) {
            size += samples.get(i).getSize();
        }
        size = (size / Math.min(theTrack.getSamples().size(), 10000)) * theTrack.getSamples().size();

        double duration = (double) theTrack.getDuration() / theTrack.getTrackMetaData().getTimescale();

        representation.setBandwidth((long) ((size * 8 / duration / 100)) * 100);

        addContentProtection(representation);
        return representation;


    }
}

