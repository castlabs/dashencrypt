/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.castlabs.dash.dashfragmenter.mp4todash;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Track;
import mpegDashSchemaMpd2011.*;
import org.apache.xmlbeans.GDuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.castlabs.dash.helpers.ManifestHelper.calculateIndexRange;
import static com.castlabs.dash.helpers.ManifestHelper.createRepresentation;


/**
 * Creates a single SIDX manifest.
 */
public class SegmentBaseSingleSidxManifestWriterImpl extends AbstractManifestWriter {


    public SegmentBaseSingleSidxManifestWriterImpl(Map<String, List<Track>> trackFamilies, Map<Track, Container> trackContainer, Map<Track, Long> trackBitrates, Map<Track, String> trackFilenames, Map<Track, UUID> trackKeyIds) {
        super(trackFamilies, trackContainer, trackBitrates, trackFilenames, trackKeyIds);
    }

    protected void createPeriod(PeriodType periodType) throws IOException {

        double maxDurationInSeconds = -1;

        for (String trackFamily : trackFamilies.keySet()) {
            List<Track> tracks = trackFamilies.get(trackFamily);

            AdaptationSetType adaptationSet = createAdaptationSet(periodType, tracks);


            for (Track track : tracks) {
                RepresentationType representation = createRepresentation(adaptationSet, track);
                SegmentBaseType segBaseType = representation.addNewSegmentBase();
                createInitialization(segBaseType.addNewInitialization(), track);

                segBaseType.setTimescale(track.getTrackMetaData().getTimescale());
                segBaseType.setIndexRangeExact(true);
                segBaseType.setIndexRange(calculateIndexRange(trackContainer.get(track)));

                representation.setId(trackFilenames.get(track));

                representation.setBandwidth(trackBitrates.get(track));
                representation.addNewBaseURL().setStringValue(trackFilenames.get(track));


                double durationInSeconds = (double) track.getDuration() / track.getTrackMetaData().getTimescale();
                maxDurationInSeconds = Math.max(maxDurationInSeconds, durationInSeconds);
            }


        }


        periodType.setDuration(new GDuration(
                1, 0, 0, 0, (int) (maxDurationInSeconds / 3600),
                (int) ((maxDurationInSeconds % 3600) / 60),
                (int) (maxDurationInSeconds % 60), BigDecimal.ZERO));


    }


}
