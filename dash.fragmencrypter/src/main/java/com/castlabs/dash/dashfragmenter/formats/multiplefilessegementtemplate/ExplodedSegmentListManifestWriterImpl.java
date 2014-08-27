package com.castlabs.dash.dashfragmenter.formats.multiplefilessegementtemplate;

import com.castlabs.dash.helpers.AbstractManifestWriter;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.boxes.threegpp26244.SegmentIndexBox;
import com.googlecode.mp4parser.util.Path;
import mpegDashSchemaMpd2011.*;
import org.apache.xmlbeans.GDuration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.castlabs.dash.helpers.ManifestHelper.createRepresentation;

/**
 * Created by user on 26.08.2014.
 */
public class ExplodedSegmentListManifestWriterImpl extends AbstractManifestWriter {
    Map<Track, List<File>> trackToSegements;
    String initPattern;
    String mediaPattern;

    public ExplodedSegmentListManifestWriterImpl(Map<String, List<Track>> trackFamilies,
                                                 Map<Track, Container> trackContainer,
                                                 Map<Track, Long> trackBitrates,
                                                 Map<Track, String> trackFilenames,
                                                 Map<Track, UUID> trackKeyIds,
                                                 Map<Track, List<File>> trackToSegements,
                                                 String initPattern, String mediaPattern) {
        super(trackFamilies, trackContainer, trackBitrates, trackFilenames, trackKeyIds);
        this.trackToSegements = trackToSegements;
        this.initPattern = initPattern;
        this.mediaPattern = mediaPattern;
    }

    @Override
    protected void createPeriod(PeriodType periodType) throws IOException {

        double maxDurationInSeconds = -1;

        for (String trackFamily : trackFamilies.keySet()) {
            List<Track> tracks = trackFamilies.get(trackFamily);
            for (Track track : tracks) {
                double durationInSeconds = (double) track.getDuration() / track.getTrackMetaData().getTimescale();
                maxDurationInSeconds = Math.max(maxDurationInSeconds, durationInSeconds);
            }
            AdaptationSetType adaptationSet = createAdaptationSet(periodType, tracks);
            Track firstTrack = tracks.get(0);
            SegmentTemplateType segmentTemplate = adaptationSet.addNewSegmentTemplate();
            segmentTemplate.setMedia(mediaPattern);
            segmentTemplate.setInitialization2(initPattern);
            segmentTemplate.setTimescale(firstTrack.getTrackMetaData().getTimescale());
            SegmentTimelineType segmentTimeline = segmentTemplate.addNewSegmentTimeline();
            List<File> segments = trackToSegements.get(firstTrack).subList(1, trackToSegements.get(firstTrack).size());

            for (File segment : segments) {
                IsoFile segmentContainer = new IsoFile(segment.getAbsolutePath());
                SegmentIndexBox sidx = Path.getPath(segmentContainer, "sidx");
                SegmentTimelineType.S s = segmentTimeline.addNewS();
                long segmentDuration = 0;
                for (SegmentIndexBox.Entry entry : sidx.getEntries()) {
                    segmentDuration += entry.getSubsegmentDuration();
                }
                s.setD((BigInteger.valueOf(segmentDuration)));
                s.setT(BigInteger.valueOf(sidx.getEarliestPresentationTime()));

            }


            for (Track track : tracks) {
                RepresentationType representation = createRepresentation(adaptationSet, track);

                representation.setBandwidth(trackBitrates.get(track));
                representation.setId(trackFilenames.get(track));

            }
        }

        //adaptationSetVid.setPar();

        periodType.setDuration(new GDuration(
                1, 0, 0, 0, (int) (maxDurationInSeconds / 3600),
                (int) ((maxDurationInSeconds % 3600) / 60),
                (int) (maxDurationInSeconds % 60), BigDecimal.ZERO));


    }

    protected void createInitialization(URLType urlType, Track track) {
        File initFile = trackToSegements.get(track).get(0);
        String dirName = trackFilenames.get(track);
        urlType.setSourceURL(dirName + "/" + initFile.getName());
    }
}
