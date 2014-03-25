package com.castlabs.dash.dashfragmenter.mp4todash;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Track;
import mpegCenc2013.DefaultKIDAttribute;
import mpegDASHSchemaMPD2011.*;
import org.apache.xmlbeans.GDuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractManifestWriter {

    protected final Map<String, List<Track>> trackFamilies;
    protected final Map<Track, Container> trackContainer;
    protected final Map<Track, Long> trackBitrates;
    protected final Map<Track, String> trackFilenames;
    protected final Map<Track, UUID> trackKeyIds;

    public AbstractManifestWriter(Map<String, List<Track>> trackFamilies,
                                  Map<Track, Container> trackContainer,
                                  Map<Track, Long> trackBitrates,
                                  Map<Track, String> trackFilenames,
                                  Map<Track, UUID> trackKeyIds) {

        this.trackFamilies = trackFamilies;
        this.trackContainer = trackContainer;
        this.trackBitrates = trackBitrates;
        this.trackFilenames = trackFilenames;
        this.trackKeyIds = trackKeyIds;
    }


    public MPDDocument getManifest() throws IOException {

        MPDDocument mdd = MPDDocument.Factory.newInstance();
        MPDtype mpd = mdd.addNewMPD();
        PeriodType periodType = mpd.addNewPeriod();
        periodType.setId("0");
        periodType.setStart(new GDuration(1, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO));

        ProgramInformationType programInformationType = mpd.addNewProgramInformation();
        programInformationType.setMoreInformationURL("www.castLabs.com");


        createPeriod(periodType);


        mpd.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");
        mpd.setType(PresentationType.STATIC); // no mpd update strategy implemented yet, could be dynamic
        mpd.setMinBufferTime(new GDuration(1, 0, 0, 0, 0, 0, 2, BigDecimal.ZERO));
        mpd.setMediaPresentationDuration(periodType.getDuration());

        return mdd;
    }

    protected AdaptationSetType createAdaptationSet(PeriodType periodType, List<Track> tracks) {
        UUID keyId = null;
        String language = null;
        for (Track track : tracks) {
            if (keyId != null && !keyId.equals(trackKeyIds.get(track))) {
                throw new RuntimeException("The ManifestWriter cannot deal with more than ONE key per adaptation set.");
            }
            keyId = trackKeyIds.get(track);

            if (language != null && !language.endsWith(track.getTrackMetaData().getLanguage())) {
                throw new RuntimeException("The ManifestWriter cannot deal with more than ONE language per adaptation set.");
            }
            language = track.getTrackMetaData().getLanguage();
        }


        AdaptationSetType adaptationSet = periodType.addNewAdaptationSet();
        adaptationSet.setSegmentAlignment(true);
        adaptationSet.setStartWithSAP(1);
        adaptationSet.setLang(language);
        adaptationSet.setBitstreamSwitching(true);

        if (keyId != null) {
            DescriptorType contentProtection = adaptationSet.addNewContentProtection();
            final DefaultKIDAttribute defaultKIDAttribute = DefaultKIDAttribute.Factory.newInstance();
            defaultKIDAttribute.setDefaultKID(Collections.singletonList(keyId.toString()));
            contentProtection.set(defaultKIDAttribute);
            contentProtection.setSchemeIdUri("urn:mpeg:dash:mp4protection:2011");
            contentProtection.setValue("cenc");
        }
        return adaptationSet;
    }

    protected void createInitialization(URLType urlType, Track track) {
        long offset = 0;
        for (Box box : trackContainer.get(track).getBoxes()) {
            if ("moov".equals(box.getType())) {
                urlType.setRange(String.format("%s-%s", offset, offset + box.getSize() - 1));
                break;
            }
            offset += box.getSize();
        }
    }

    abstract protected void createPeriod(PeriodType periodType) throws IOException;

}
