/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.castlabs.dash.dashfragmenter.formats.csf;

import com.castlabs.dash.helpers.AbstractManifestWriter;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.googlecode.mp4parser.authoring.Track;
import mpegDashSchemaMpd2011.*;
import org.apache.xmlbeans.GDuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.castlabs.dash.helpers.ManifestHelper.createRepresentation;


/**
 * Creates a single SIDX manifest.
 */
public class SegmentListManifestWriterImpl extends AbstractManifestWriter {


    public SegmentListManifestWriterImpl(Map<String, List<Track>> trackFamilies, Map<Track, Container> trackContainer, Map<Track, Long> trackBitrates, Map<Track, String> trackFilenames, Map<Track, UUID> trackKeyIds) {
        super(trackFamilies, trackContainer, trackBitrates, trackFilenames, trackKeyIds);
    }


    protected void createPeriod(PeriodType periodType) throws IOException {

        double maxDurationInSeconds = -1;

        for (String trackFamily : trackFamilies.keySet()) {
            List<Track> tracks = trackFamilies.get(trackFamily);
            for (Track track : tracks) {
                double durationInSeconds = (double) track.getDuration() / track.getTrackMetaData().getTimescale();
                maxDurationInSeconds = Math.max(maxDurationInSeconds, durationInSeconds);
            }
            AdaptationSetType adaptationSet = createAdaptationSet(periodType, tracks);


            for (Track track : tracks) {
                RepresentationType representation = createRepresentation(adaptationSet, track);

                representation.setBandwidth(trackBitrates.get(track));
                representation.addNewBaseURL().setStringValue(trackFilenames.get(track));
                long offset = 0;
                Iterator<Box> boxes = trackContainer.get(track).getBoxes().iterator();
                SegmentListType segmentList = representation.addNewSegmentList();
                segmentList.setTimescale(track.getTrackMetaData().getTimescale());
                SegmentTimelineType segmentTimeline = segmentList.addNewSegmentTimeline();
                createInitialization(segmentList.addNewInitialization(), track);
                long time = 0;
                while (boxes.hasNext()) {
                    Box b = boxes.next();
                    if ("moof".equals(b.getType())) {

                        SegmentTimelineType.S s = segmentTimeline.addNewS();
                        MovieFragmentBox moof = (MovieFragmentBox) b;
                        assert moof.getTrackRunBoxes().size() == 1 : "Ouch - doesn't with mutiple trun";

                        TrackRunBox trun = moof.getTrackRunBoxes().get(0);
                        long segmentDuration = 0;
                        for (TrackRunBox.Entry entry : trun.getEntries()) {
                            segmentDuration += entry.getSampleDuration();
                        }
                        s.setD((BigInteger.valueOf(segmentDuration)));
                        s.setT(BigInteger.valueOf(time));
                        time += segmentDuration;

                        SegmentURLType segmentURL = segmentList.addNewSegmentURL();
                        Box mdat = boxes.next();
                        segmentURL.setMediaRange(
                                String.format("%s-%s",
                                        offset, offset + moof.getSize() + mdat.getSize())
                        );

                        offset += moof.getSize() + mdat.getSize();
                    } else {
                        offset += b.getSize();
                    }
                }

            }
        }

        //adaptationSetVid.setPar();

        periodType.setDuration(new GDuration(
                1, 0, 0, 0, (int) (maxDurationInSeconds / 3600),
                (int) ((maxDurationInSeconds % 3600) / 60),
                (int) (maxDurationInSeconds % 60), BigDecimal.ZERO));


    }

}
