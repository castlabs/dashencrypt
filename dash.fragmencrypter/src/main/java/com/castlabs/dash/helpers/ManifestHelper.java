/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.helpers;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.boxes.threegpp26244.SegmentIndexBox;
import com.googlecode.mp4parser.util.Path;
import mpegDashSchemaMpd2011.AdaptationSetType;
import mpegDashSchemaMpd2011.DescriptorType;
import mpegDashSchemaMpd2011.RepresentationType;
import org.apache.commons.lang.math.Fraction;
import org.apache.xmlbeans.XmlOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some conversion from Track representation to Manifest specifics shared by DASH manifests of all kinds.
 */
public class ManifestHelper {

    public static String convertFramerate(double vrate) {
        Fraction f1 = Fraction.getFraction((int) (vrate * 1001), 1001);
        Fraction f2 = Fraction.getFraction((int) (vrate * 1000), 1000);
        double d1 = Math.abs(f1.doubleValue() - vrate);
        double d2 = Math.abs(f2.doubleValue() - vrate);
        if (d1 < d2) {
            return f1.getNumerator() + "/" + f1.getDenominator();
        } else {
            return f2.getNumerator() + "/" + f2.getDenominator();
        }


    }

    public static String calculateIndexRange(Container isoFile) throws IOException {
        SegmentIndexBox sidx = Path.getPath(isoFile, "/sidx");
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

            Fraction fraction = Fraction.getFraction((int) videoWidth, (int) videoHeight).reduce();
            adaptationSet.setPar("" + fraction.getNumerator() + ":" + fraction.getDenominator());


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
            representation.setAudioSamplingRate("" + DashHelper.getAudioSamplingRate(ase));

            DescriptorType audio_channel_conf = representation.addNewAudioChannelConfiguration();
            DashHelper.ChannelConfiguration cc = DashHelper.getChannelConfiguration(ase);
            audio_channel_conf.setSchemeIdUri(cc.schemeIdUri);
            audio_channel_conf.setValue(cc.value);

        }
        return representation;
    }

    static Pattern REPRESENTATION_PATTERN = Pattern.compile("(\\$RepresentationID(%0[0-9]+d)?\\$)");
    static Pattern NUMBER_PATTERN = Pattern.compile("(\\$Number(%0[0-9]+d)?\\$)");
    static Pattern TIME_PATTERN = Pattern.compile("(\\$Time(%0[0-9]+d)?\\$)");
    static Pattern BANDWIDTH_PATTERN = Pattern.compile("(\\$Bandwidth(%0[0-9]+d)?\\$)");


    private static String replace(Matcher m, String rv, Object value) {
        while (m.find()) {
            if (m.group(2) != null) {
                rv = rv.replace(m.group(0), String.format(m.group(2), value));
            } else {
                rv = rv.replace(m.group(0), String.valueOf(value));
            }
        }
        return rv;
    }

    public static String templateReplace(String input_string, String representation_id, long number, long bandwidth, long time) {
        String rv = input_string;
        rv = rv.replace("$$", "$");
        rv = replace(REPRESENTATION_PATTERN.matcher(rv), rv, representation_id);
        rv = replace(NUMBER_PATTERN.matcher(rv), rv, number);
        rv = replace(TIME_PATTERN.matcher(rv), rv, time);
        rv = replace(BANDWIDTH_PATTERN.matcher(rv), rv, bandwidth);
        return rv;
    }

    public static XmlOptions getXmlOptions() {
        XmlOptions xmlOptions = new XmlOptions();
        //xmlOptions.setUseDefaultNamespace();
        HashMap<String, String> ns = new HashMap<String, String>();
        //ns.put("urn:mpeg:DASH:schema:MPD:2011", "");
        ns.put("urn:mpeg:cenc:2013", "cenc");
        ns.put("urn:mpeg:drmtdoday:cenc:2014", "drmtoday");
        ns.put("urn:microsoft:playready", "mspr");
        xmlOptions.setSaveSuggestedPrefixes(ns);
        xmlOptions.setSaveAggressiveNamespaces();
        xmlOptions.setUseDefaultNamespace();
        xmlOptions.setSavePrettyPrint();
        return xmlOptions;
    }

    public static long getApproxTrackSize(Track track) {
        long size = 0;
        List<Sample> samples = track.getSamples();
        for (int i = 0; i < Math.min(samples.size(), 10000); i++) {
            size += samples.get(i).getSize();
        }
        size = (size / Math.min(track.getSamples().size(), 10000)) * track.getSamples().size();
        return size;
    }
}
