/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.castlabs.dash.dashfragmenter.formats.csf;

import com.castlabs.dash.dashfragmenter.sequences.DashFileSetSequence;
import com.castlabs.dash.helpers.AbstractManifestWriter;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Track;
import mpegDashSchemaMpd2011.AdaptationSetType;
import mpegDashSchemaMpd2011.PeriodType;
import mpegDashSchemaMpd2011.RepresentationType;
import mpegDashSchemaMpd2011.SegmentBaseType;
import org.apache.xmlbeans.GDuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.castlabs.dash.helpers.ManifestHelper.calculateIndexRange;
import static com.castlabs.dash.helpers.ManifestHelper.createRepresentation;


/**
 * Creates a single SIDX manifest.
 */
public class SegmentBaseSingleSidxManifestWriterImpl extends AbstractManifestWriter {

    private final Map<Track, Container> trackContainer;
    private final Map<String, List<Track>> adaptationSets;
    private final Map<Track, String> trackFilenames;
    private final Map<Track, Long> trackBitrates;
    private final boolean writeSegmentBase;
    private Map<String, String> adaptationSet2Role;

    public SegmentBaseSingleSidxManifestWriterImpl(
            DashFileSetSequence dashFileSetSequence,
            Map<String, List<Track>> adaptationSets,
            Map<Track, Container> trackContainer,
            Map<Track, Long> trackBitrates,
            Map<Track, String> trackFilenames,
            boolean writeSegmentBase,
            Map<String, String> adaptationSet2Role) {

        super(trackContainer, trackBitrates, dashFileSetSequence);
        this.adaptationSets = adaptationSets;
        this.trackFilenames = trackFilenames;
        this.trackContainer = trackContainer;
        this.trackBitrates = trackBitrates;
        this.writeSegmentBase = writeSegmentBase;
        this.adaptationSet2Role = adaptationSet2Role;
    }

    @Override
    public String getProfile() {
        return "urn:mpeg:dash:profile:isoff-on-demand:2011";
    }

    protected void createPeriod(PeriodType periodType) throws IOException {

        double maxDurationInSeconds = -1;

        for (String adaptationSetId : adaptationSets.keySet()) {
            List<Track> tracks = adaptationSets.get(adaptationSetId);

            AdaptationSetType adaptationSet = createAdaptationSet(periodType, tracks, adaptationSet2Role.get(adaptationSetId));


            for (Track track : tracks) {
                RepresentationType representation = createRepresentation(adaptationSet, track);

                if (writeSegmentBase) {
                    SegmentBaseType segBaseType = representation.addNewSegmentBase();
                    createInitialization(segBaseType.addNewInitialization(), track);
                    segBaseType.setTimescale(track.getTrackMetaData().getTimescale());
                    segBaseType.setIndexRangeExact(true);
                    segBaseType.setIndexRange(calculateIndexRange(trackContainer.get(track)));
                }

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
