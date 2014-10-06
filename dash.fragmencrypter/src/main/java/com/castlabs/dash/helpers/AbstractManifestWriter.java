/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.helpers;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.MovieHeaderBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackExtendsBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentHeaderBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.coremedia.iso.boxes.mdat.MediaDataBox;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.util.Iso639;
import com.googlecode.mp4parser.util.Path;
import mpegCenc2013.DefaultKIDAttribute;
import mpegDashSchemaMpd2011.*;
import org.apache.xmlbeans.GDuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

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



    class Buffer {
        final long bandwidth;
        final long timescale;
        public Buffer(long bandwidth, long timescale) {
            this.bandwidth = bandwidth;
            this.timescale = timescale;
        }

        long currentBufferFullness = 0;
        long minBufferFullness = 0;

        void simPlayback(long size, long videoTime) {
            currentBufferFullness -= size;
            currentBufferFullness += ((double) videoTime / timescale) * bandwidth / 8;
            if (currentBufferFullness<minBufferFullness) {
                minBufferFullness = currentBufferFullness;
            }



        }

    }



    public GDuration getMinBufferTime() {
        int requiredTimeInS = 0;
        for (Map.Entry<Track, Container> trackContainerEntry : trackContainer.entrySet()) {
            long bitrate = trackBitrates.get(trackContainerEntry.getKey());
            long timescale = ((MediaHeaderBox)Path.getPath(trackContainerEntry.getValue(), "/moov[0]/trak[0]/mdia[0]/mdhd[0]")).getTimescale();
            long requiredBuffer = 0;
            Iterator<Box> iterator = trackContainerEntry.getValue().getBoxes().iterator();
            while (iterator.hasNext()) {

                Box moofCand = iterator.next();
                if (!moofCand.getType().equals("moof")) {
                    continue;
                }
                MovieFragmentBox moof = (MovieFragmentBox) moofCand;
                Box mdat = iterator.next();

                Buffer currentBuffer = new Buffer(bitrate, timescale);
                currentBuffer.simPlayback(moof.getSize(), 0);
                for (TrackRunBox trun : moof.getTrackRunBoxes()) {
                    for (TrackRunBox.Entry entry : trun.getEntries()) {


                        long sampleDuration;
                        if (trun.isSampleDurationPresent()) {
                            sampleDuration = trun.getEntries().get(0).getSampleDuration();
                        } else {
                            TrackFragmentHeaderBox tfhd = Path.getPath(moof, "traf[0]/tfhd[0]");
                            if (tfhd.hasDefaultSampleDuration()) {
                                sampleDuration = tfhd.getDefaultSampleDuration();
                            } else {
                                TrackExtendsBox trex = Path.getPath(trackContainerEntry.getValue(), "/moov[0]/mvex[0]/trex[0]");
                                sampleDuration = trex.getDefaultSampleDuration();
                            }
                        }


                        long size;
                        if (trun.isSampleSizePresent()) {
                            size = entry.getSampleSize();
                        } else {
                            TrackFragmentHeaderBox tfhd = Path.getPath(moof, "traf[0]/tfhd[0]");
                            if (tfhd.hasDefaultSampleSize()) {
                                size = tfhd.getDefaultSampleSize();
                            } else {
                                TrackExtendsBox trex = Path.getPath(trackContainerEntry.getValue(), "/moov[0]/mvex[0]/trex[0]");
                                size = trex.getDefaultSampleSize();
                            }
                        }
                        currentBuffer.simPlayback(size, sampleDuration);
                    }

                }
                requiredBuffer = Math.max(requiredBuffer, -(currentBuffer.minBufferFullness + Math.min(0, currentBuffer.currentBufferFullness)));

            }
            requiredTimeInS = (int) Math.max(requiredTimeInS, Math.ceil((double)requiredBuffer  / (bitrate / 8)));
        }
        return new GDuration(1, 0, 0, 0, 0, 0, requiredTimeInS, BigDecimal.ZERO);
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


        mpd.setMinBufferTime(getMinBufferTime());
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
        adaptationSet.setLang(Iso639.convert3to2(language));
        adaptationSet.setBitstreamSwitching(true);
        if (tracks.get(0).getHandler().equals("soun")) {
            adaptationSet.setMimeType("audio/mp4");
        } else if (tracks.get(0).getHandler().equals("vide")) {
            adaptationSet.setMimeType("video/mp4");
        } else {
            throw new RuntimeException("Don't know what to do with handler type = " + tracks.get(0).getHandler());
        }

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
