package com.castlabs.dash.dashfragmenter.formats.multiplefilessegmenttemplate;

import com.castlabs.dash.dashfragmenter.sequences.DashFileSetSequence;
import com.castlabs.dash.helpers.AbstractManifestWriter;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.util.Path;
import mpegDashSchemaMpd2011.*;
import org.apache.xmlbeans.GDuration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.castlabs.dash.helpers.ManifestHelper.createRepresentation;
import static com.castlabs.dash.helpers.Timing.*;

/**
 * Creates a manifest fr
 */
public class ExplodedSegmentListManifestWriterImpl extends AbstractManifestWriter {
    private final Map<Track, List<File>> trackToSegements;
    private final Map<String, List<Track>> adaptationSets;
    private final Map<Track, Long> trackBitrates;
    private final Map<Track, String> trackFilenames;
    private final String initPattern;
    private final String mediaPattern;
    private Map<String, String> adaptationSet2Role;
    private final boolean compressTimeline;

    public ExplodedSegmentListManifestWriterImpl(
            DashFileSetSequence dashFileSetSequence,
            Map<String, List<Track>> adaptationSets,
            Map<Track, Container> trackContainer,
            Map<Track, Long> representationBitrates,
            Map<Track, String> representationIds,
            Map<Track, List<File>> trackToSegements,
            String initPattern, String mediaPattern,
            boolean compressTimeline,
            Map<String, String> adaptationSet2Role) {
        super(trackContainer, representationBitrates, dashFileSetSequence);
        this.trackBitrates = representationBitrates;
        this.adaptationSets = adaptationSets;
        this.trackToSegements = trackToSegements;
        this.trackFilenames = representationIds;
        this.initPattern = initPattern;
        this.mediaPattern = mediaPattern;
        this.compressTimeline = compressTimeline;
        this.adaptationSet2Role = adaptationSet2Role;
    }

    @Override
    public String getProfile() {
        return "urn:mpeg:dash:profile:isoff-live:2011";
    }


    @Override
    protected void createPeriod(PeriodType periodType) throws IOException {

        double maxDurationInSeconds = -1;
        // %lang%
        for (Map.Entry<String, List<Track>> e : adaptationSets.entrySet()) {
            String adaptationSetId = e.getKey();
            List<Track> tracks = adaptationSets.get(adaptationSetId);

            for (Track track : tracks) {
                double durationInSeconds = (double) track.getDuration() / track.getTrackMetaData().getTimescale();
                maxDurationInSeconds = Math.max(maxDurationInSeconds, durationInSeconds);
            }
            AdaptationSetType adaptationSet = createAdaptationSet(periodType, tracks, adaptationSet2Role.get(adaptationSetId));
            Track firstTrack = tracks.get(0);
            SegmentTemplateType segmentTemplate = adaptationSet.addNewSegmentTemplate();
            segmentTemplate.setMedia(mediaPattern.replace("%lang%", tracks.get(0).getTrackMetaData().getLanguage()));
            segmentTemplate.setInitialization2(initPattern.replace("%lang%", tracks.get(0).getTrackMetaData().getLanguage()));
            segmentTemplate.setTimescale(firstTrack.getTrackMetaData().getTimescale());
            SegmentTimelineType segmentTimeline = segmentTemplate.addNewSegmentTimeline();
            List<File> segments = trackToSegements.get(firstTrack).subList(1, trackToSegements.get(firstTrack).size());
            File init = trackToSegements.get(firstTrack).get(0);

            TrackRunBox firstTrun = Path.getPath(new IsoFile(segments.get(0).getAbsolutePath()), "moof/traf/trun");

            long[] ptss = getPtss(firstTrun);
            Arrays.sort(ptss); // index 0 has now the earliest presentation time stamp!
            long timeMappingEdit = getTimeMappingEditTime(new IsoFile(init.getAbsolutePath()));
            long startTime = ptss[0] - timeMappingEdit;

            SegmentTimelineType.S lastSegmentTimelineS = null;
            for (File segment : segments) {
                long duration = 0;
                List<TrackRunBox> truns = Path.getPaths(new IsoFile(segment.getAbsolutePath()), "moof/traf/trun");
                for (TrackRunBox trun : truns) {
                    duration += getDuration(trun);
                }

                if (compressTimeline && lastSegmentTimelineS != null && lastSegmentTimelineS.getD().equals(BigInteger.valueOf(duration))) {
                    if (lastSegmentTimelineS.isSetR()) {
                        lastSegmentTimelineS.setR(lastSegmentTimelineS.getR().add(BigInteger.ONE));
                    } else {
                        lastSegmentTimelineS.setR(BigInteger.ONE);
                    }

                } else {
                    SegmentTimelineType.S s = segmentTimeline.addNewS();
                    s.setD((BigInteger.valueOf(duration)));
                    s.setT(BigInteger.valueOf(startTime));
                    lastSegmentTimelineS = s;
                }

                startTime += duration;
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
