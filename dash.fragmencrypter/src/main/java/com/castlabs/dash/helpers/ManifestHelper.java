/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.helpers;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.coremedia.iso.boxes.sampleentry.DashHelper;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.boxes.threegpp26244.SegmentIndexBox;
import com.googlecode.mp4parser.util.Path;
import mpegDashSchemaMpd2011.AdaptationSetType;
import mpegDashSchemaMpd2011.DescriptorType;
import mpegDashSchemaMpd2011.RepresentationType;
import org.apache.commons.lang.math.Fraction;

import java.io.IOException;

/**
 * Some conversion from Track representation to Manifest specifics shared by DASH manifests of all kinds.
 */
public class ManifestHelper {

    public static String convertFramerate(double vrate) {
        vrate = (double) Math.round(vrate * 1000) / 1000;
        String frameRate = null;
        if ((vrate > 14) && (vrate < 15)) {
            frameRate = "15000/1001";
        } else if ((vrate == 15)) {
            frameRate = "15000/1000";
        } else if ((vrate > 23) && (vrate < 24)) {
            frameRate = "24000/1001";
        } else if (vrate == 24) {
            frameRate = "24000/1000";
        } else if ((vrate > 24) && ((vrate < 25) || (vrate == 25))) {
            frameRate = "25000/1000";
        } else if ((vrate > 29) && (vrate < 30)) {
            frameRate = "30000/1001";
        } else if (vrate == 30) {
            frameRate = "30000/1000";
        } else if (vrate == 50) {
            frameRate = "50000/1000";
        } else if ((vrate > 59) && (vrate < 60)) {
            frameRate = "60000/1001";
        } else if (vrate == 60) {
            frameRate = "60000/1000";
        } else {
            System.out.println("Framerate " + vrate + " is not supported");
            System.exit(1);
        }
        return frameRate;
    }

    public static String calculateIndexRange(Container isoFile) throws IOException {
        SegmentIndexBox sidx = (SegmentIndexBox) Path.getPath(isoFile, "/sidx");
        long start = 0;
        for (Box box : isoFile.getBoxes()) {
            if (box == sidx) {
                break;
            } else {
                start += box.getSize();
            }
        }
        // long start = sidx.getOffset(); getOffset works for parsed files only
        long end = sidx.getSize() - 1 + start; // start & end are inclusive!
        return String.format("%s-%s", start, end);
    }


    /**
     * Creates a representation and adjusts the AdaptionSet's attributes maxFrameRate, maxWidth, maxHeight.
     * Also creates AudioChannelConfiguration.
     */
    public static RepresentationType createRepresentation(AdaptationSetType adaptationSet, Track track) {
        RepresentationType representation = adaptationSet.addNewRepresentation();
        if (track.getHandler().equals("vide")) {

            long videoHeight = (long) track.getTrackMetaData().getHeight();
            long videoWidth = (long) track.getTrackMetaData().getWidth();
            double framesPerSecond = (double) (track.getSamples().size() * track.getTrackMetaData().getTimescale()) / track.getDuration();

            adaptationSet.setMaxFrameRate(convertFramerate(
                    Math.max(adaptationSet.isSetMaxFrameRate() ? Fraction.getFraction(adaptationSet.getMaxFrameRate()).doubleValue() : 0,
                            framesPerSecond)
            ));

            adaptationSet.setMaxWidth(Math.max(adaptationSet.isSetMaxWidth() ? adaptationSet.getMaxWidth() : 0,
                    videoWidth));
            adaptationSet.setMaxHeight(Math.max(adaptationSet.isSetMaxHeight() ? adaptationSet.getMaxHeight() : 0,
                    videoHeight));

            adaptationSet.setPar("1:1");
            // too hard to find it out. Ignoring even though it should be set according to DASH-AVC-264-v2.00-hd-mca.pdf

            //representation.setMimeType("video/mp4");
            representation.setCodecs(DashHelper.getRfc6381Codec(track.getSampleDescriptionBox().getSampleEntry()));
            representation.setWidth(videoWidth);
            representation.setHeight(videoHeight);
            representation.setFrameRate(convertFramerate(framesPerSecond));
            representation.setSar("1:1");
            // too hard to find it out. Ignoring even though it should be set according to DASH-AVC-264-v2.00-hd-mca.pdf
        }

        if (track.getHandler().equals("soun")) {


            AudioSampleEntry ase = (AudioSampleEntry) track.getSampleDescriptionBox().getSampleEntry();

            //representation.setMimeType("audio/mp4");
            representation.setCodecs(DashHelper.getRfc6381Codec(ase));
            representation.setAudioSamplingRate("" + ase.getSampleRate());

            DescriptorType audio_channel_conf = representation.addNewAudioChannelConfiguration();
            DashHelper.ChannelConfiguration cc = DashHelper.getChannelConfiguration(ase);
            audio_channel_conf.setSchemeIdUri(cc.schemeIdUri);
            audio_channel_conf.setValue(cc.value);

        }
        return representation;
    }




}
