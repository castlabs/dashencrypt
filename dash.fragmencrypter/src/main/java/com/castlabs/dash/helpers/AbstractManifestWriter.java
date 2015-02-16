/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.helpers;

import com.castlabs.dash.dashfragmenter.sequences.DashFileSetSequence;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackExtendsBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentHeaderBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.tracks.CencEncryptedTrack;
import com.googlecode.mp4parser.util.Iso639;
import com.googlecode.mp4parser.util.Path;
import mpegCenc2013.DefaultKIDAttribute;
import mpegDashSchemaMpd2011.*;
import org.apache.xmlbeans.GDuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public abstract class AbstractManifestWriter {

    private final Map<? extends  Track, Container> trackContainer;
    private final Map<? extends  Track, Long> trackBitrates;
    DashFileSetSequence dashFileSetSequence;


    public AbstractManifestWriter(Map<? extends  Track, Container> trackContainer,
                                  Map<? extends  Track, Long> trackBitrates,
                                  DashFileSetSequence dashFileSetSequence) {

        this.trackContainer = trackContainer;
        this.trackBitrates = trackBitrates;
        this.dashFileSetSequence = dashFileSetSequence;

    }

    public GDuration getMinBufferTime() {
        int requiredTimeInS = 0;
        for (Map.Entry<? extends  Track, Container> trackContainerEntry : trackContainer.entrySet()) {
            long bitrate = trackBitrates.get(trackContainerEntry.getKey());
            long timescale = ((MediaHeaderBox) Path.getPath(trackContainerEntry.getValue(), "/moov[0]/trak[0]/mdia[0]/mdhd[0]")).getTimescale();
            long requiredBuffer = 0;
            Iterator<Box> iterator = trackContainerEntry.getValue().getBoxes().iterator();
            while (iterator.hasNext()) {

                Box moofCand = iterator.next();
                if (!moofCand.getType().equals("moof")) {
                    continue;
                }
                MovieFragmentBox moof = (MovieFragmentBox) moofCand;
                iterator.next(); // skip mdat

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
            requiredTimeInS = (int) Math.max(requiredTimeInS, Math.ceil((double) requiredBuffer / (bitrate / 8)));
        }
        return new GDuration(1, 0, 0, 0, 0, 0, requiredTimeInS, BigDecimal.ZERO);
    }

    public abstract String getProfile();

    public MPDDocument getManifest() throws IOException {

        MPDDocument mdd = MPDDocument.Factory.newInstance();
        MPDtype mpd = mdd.addNewMPD();
        PeriodType periodType = mpd.addNewPeriod();
        periodType.setId("0");
        periodType.setStart(new GDuration(1, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO));

        ProgramInformationType programInformationType = mpd.addNewProgramInformation();
        programInformationType.setMoreInformationURL("www.castLabs.com");


        createPeriod(periodType);


        mpd.setProfiles(getProfile());
        mpd.setType(PresentationType.STATIC); // no mpd update strategy implemented yet, could be dynamic


        mpd.setMinBufferTime(getMinBufferTime());
        mpd.setMediaPresentationDuration(periodType.getDuration());

        return mdd;
    }

    UUID getKeyId(Track track) {
        if (track instanceof CencEncryptedTrack) {
            return ((CencEncryptedTrack) track).getDefaultKeyId();
        } else {
            return null;
        }

    }

    protected AdaptationSetType createAdaptationSet(PeriodType periodType, List<Track> tracks, String role) {
        UUID keyId = null;
        String language = null;
        for (Track track : tracks) {

            if (keyId != null && !keyId.equals(getKeyId(track))) {
                throw new RuntimeException("The ManifestWriter cannot deal with more than ONE key per adaptation set.");
            }
            keyId = getKeyId(track);

            if (language != null && !language.endsWith(track.getTrackMetaData().getLanguage())) {
                throw new RuntimeException("The ManifestWriter cannot deal with more than ONE language per adaptation set.");
            }
            language = track.getTrackMetaData().getLanguage();
        }


        AdaptationSetType adaptationSet = periodType.addNewAdaptationSet();

        if (role != null) {
            DescriptorType roleDescriptorType = adaptationSet.addNewRole();
            String scheme = role.split("\\|")[0];
            String id = role.split("\\|")[1];
            roleDescriptorType.setSchemeIdUri(scheme);
            roleDescriptorType.setId(id);
        }

        if (!tracks.get(0).getHandler().equals("subt")) {
            adaptationSet.setSegmentAlignment(true);
            adaptationSet.setSubsegmentAlignment(true);
            adaptationSet.setSubsegmentStartsWithSAP(1);
            adaptationSet.setStartWithSAP(1);
            adaptationSet.setBitstreamSwitching(true);
        }
        if (!"und".equals(language)) {
            adaptationSet.setLang(Iso639.convert3to2(language));
        }

        if (tracks.get(0).getHandler().equals("soun")) {
            adaptationSet.setMimeType("audio/mp4");
        } else if (tracks.get(0).getHandler().equals("vide")) {
            adaptationSet.setMimeType("video/mp4");
        } else if (tracks.get(0).getHandler().equals("subt")) {
            adaptationSet.setMimeType("video/mp4");
        } else {
            throw new RuntimeException("Don't know what to do with handler type = " + tracks.get(0).getHandler());
        }

        if (keyId != null) {
            addContentProtection(adaptationSet, keyId);
        }
        return adaptationSet;
    }

    protected void addContentProtection(AdaptationSetType adaptationSet, UUID keyId) {
        dashFileSetSequence.addContentProtection(adaptationSet, keyId);
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

    class Buffer {
        final long bandwidth;
        final long timescale;
        long currentBufferFullness = 0;
        long minBufferFullness = 0;

        public Buffer(long bandwidth, long timescale) {
            this.bandwidth = bandwidth;
            this.timescale = timescale;
        }

        void simPlayback(long size, long videoTime) {
            currentBufferFullness -= size;
            currentBufferFullness += ((double) videoTime / timescale) * bandwidth / 8;
            if (currentBufferFullness < minBufferFullness) {
                minBufferFullness = currentBufferFullness;
            }
        }
    }

}
