package com.castlabs.dash.dashfragmenter.mp4todash;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.googlecode.mp4parser.authoring.Track;
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
    Map<Track, String> trackToTemplate;

    public ExplodedSegmentListManifestWriterImpl(Map<String, List<Track>> trackFamilies,
                                                 Map<Track, Container> trackContainer,
                                                 Map<Track, Long> trackBitrates,
                                                 Map<Track, String> trackFilenames,
                                                 Map<Track, UUID> trackKeyIds,
                                                 Map<Track, List<File>> trackToSegements,
                                                 Map<Track, String> trackToTemplate) {
        super(trackFamilies, trackContainer, trackBitrates, trackFilenames, trackKeyIds);
        this.trackToSegements = trackToSegements;
        this.trackToTemplate = trackToTemplate;
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


            for (Track track : tracks) {
                RepresentationType representation = createRepresentation(adaptationSet, track);

                representation.setBandwidth(trackBitrates.get(track));

                SegmentTemplateType segmentTemplate = representation.addNewSegmentTemplate();
                segmentTemplate.setTimescale(track.getTrackMetaData().getTimescale());
                SegmentTimelineType segmentTimeline = segmentTemplate.addNewSegmentTimeline();
                createInitialization(segmentTemplate.addNewInitialization(), track);
                long time = 0;
                List<File> segments = trackToSegements.get(track).subList(1, trackToSegements.get(track).size());
                segmentTemplate.setMedia(trackToTemplate.get(track));
                for (File segment : segments) {
                    IsoFile segmentContainer = new IsoFile(segment.getAbsolutePath());
                    MovieFragmentBox moof = Path.getPath(segmentContainer, "moof");
                    SegmentTimelineType.S s = segmentTimeline.addNewS();
                    assert moof.getTrackRunBoxes().size() == 1 : "Ouch - doesn't with mutiple trun";
                    TrackRunBox trun = moof.getTrackRunBoxes().get(0);
                    long segmentDuration = 0;
                    for (TrackRunBox.Entry entry : trun.getEntries()) {
                        segmentDuration += entry.getSampleDuration();
                    }
                    s.setD((BigInteger.valueOf(segmentDuration)));
                    s.setT(BigInteger.valueOf(time));
                    time += segmentDuration;


                }

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
