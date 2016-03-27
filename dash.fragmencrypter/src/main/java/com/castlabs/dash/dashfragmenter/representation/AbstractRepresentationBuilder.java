package com.castlabs.dash.dashfragmenter.representation;

import com.castlabs.dash.helpers.DashHelper;
import com.castlabs.dash.helpers.SapHelper;
import com.castlabs.dash.helpers.Timing;
import com.coremedia.iso.BoxParser;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.*;
import com.coremedia.iso.boxes.fragment.*;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.authoring.Edit;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.tracks.CencEncryptedTrack;
import com.googlecode.mp4parser.boxes.dece.SampleEncryptionBox;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.CencSampleEncryptionInformationGroupEntry;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.GroupEntry;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.SampleGroupDescriptionBox;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.SampleToGroupBox;
import com.googlecode.mp4parser.boxes.threegpp26244.SegmentIndexBox;
import com.googlecode.mp4parser.util.Path;
import com.googlecode.mp4parser.util.UUIDConverter;
import com.mp4parser.iso14496.part12.SampleAuxiliaryInformationOffsetsBox;
import com.mp4parser.iso14496.part12.SampleAuxiliaryInformationSizesBox;
import com.mp4parser.iso23001.part7.CencSampleAuxiliaryDataFormat;
import com.mp4parser.iso23001.part7.ProtectionSystemSpecificHeaderBox;
import com.mp4parser.iso23001.part7.TrackEncryptionBox;
import mpegCenc2013.DefaultKIDAttribute;
import mpegDashSchemaMpd2011.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.*;

import static com.castlabs.dash.helpers.BoxHelper.boxToBytes;
import static com.castlabs.dash.helpers.ManifestHelper.convertFramerate;
import static com.castlabs.dash.helpers.ManifestHelper.getApproxTrackSize;
import static com.castlabs.dash.helpers.Timing.getDuration;
import static com.castlabs.dash.helpers.Timing.getPtss;
import static com.castlabs.dash.helpers.Timing.getTimeMappingEditTime;
import static com.googlecode.mp4parser.util.CastUtils.l2i;

public abstract class AbstractRepresentationBuilder extends AbstractList<Container> implements RepresentationBuilder {
    protected Track theTrack;
    protected final List<ProtectionSystemSpecificHeaderBox> psshs;
    protected Date date = new Date();
    protected String source;
    protected long[] segmentStartSamples;
    protected long[] fragmentStartSamples;

    public AbstractRepresentationBuilder(Track track, List<ProtectionSystemSpecificHeaderBox> psshs, String source, long[] segmentStartSamples, long[] fragmentStartSamples) {
        this.theTrack = track;
        this.psshs = psshs;
        this.source = source;
        this.fragmentStartSamples = fragmentStartSamples;
        this.segmentStartSamples = segmentStartSamples;
    }

    public String getSource() {
        return source;
    }

    public Track getTrack() {
        return theTrack;
    }

    public Container getInitSegment() {
        List<Box> initSegment = new ArrayList<Box>();
        List<String> minorBrands = new ArrayList<String>();
        minorBrands.add("isom");
        minorBrands.add("iso6");
        minorBrands.add("avc1");
        initSegment.add(new FileTypeBox("isom", 0, minorBrands));
        initSegment.add(createMoov());
        return new ListContainer(initSegment);
    }

    protected Box createMoov() {
        MovieBox movieBox = new MovieBox();

        movieBox.addBox(createMvhd());
        movieBox.addBox(createTrak(theTrack));
        movieBox.addBox(createMvex());
        if (psshs != null) {
            for (ProtectionSystemSpecificHeaderBox pssh : psshs) {
                movieBox.addBox(pssh);
            }
        }
        // metadata here
        return movieBox;
    }


    protected Box createTrex(Track track) {
        TrackExtendsBox trex = new TrackExtendsBox();
        trex.setTrackId(track.getTrackMetaData().getTrackId());
        trex.setDefaultSampleDescriptionIndex(1);
        trex.setDefaultSampleDuration(0);
        trex.setDefaultSampleSize(0);
        SampleFlags sf = new SampleFlags();
        if ("soun".equals(track.getHandler()) || "subt".equals(track.getHandler()) || "text".equals(track.getHandler())) {
            // as far as I know there is no audio encoding
            // where the sample are not self contained.
            // same seems to be true for subtitle tracks
            sf.setSampleDependsOn(2);
            sf.setSampleIsDependedOn(2);
        }
        trex.setDefaultSampleFlags(sf);
        return trex;
    }

    protected Box createMvex() {
        MovieExtendsBox mvex = new MovieExtendsBox();
        final MovieExtendsHeaderBox mved = new MovieExtendsHeaderBox();
        mved.setVersion(1);
        mved.setFragmentDuration(theTrack.getDuration());
        mvex.addBox(mved);

        mvex.addBox(createTrex(theTrack));
        return mvex;
    }

    protected Box createTkhd(Track track) {
        TrackHeaderBox tkhd = new TrackHeaderBox();
        tkhd.setVersion(1);
        tkhd.setFlags(7); // enabled, in movie, in previe, in poster

        tkhd.setAlternateGroup(track.getTrackMetaData().getGroup());
        tkhd.setCreationTime(track.getTrackMetaData().getCreationTime());
        // We need to take edit list box into account in trackheader duration
        // but as long as I don't support edit list boxes it is sufficient to
        // just translate media duration to movie timescale
        if (track.getEdits().isEmpty()) {
            tkhd.setDuration(track.getDuration());
        } else {
            long dur = 0;
            for (Edit edit : track.getEdits()) {
                dur += edit.getMediaTime() != -1 ?
                        edit.getSegmentDuration() * track.getTrackMetaData().getTimescale() : 0;
            }
            tkhd.setDuration(dur);
        }


        tkhd.setHeight(track.getTrackMetaData().getHeight());
        tkhd.setWidth(track.getTrackMetaData().getWidth());
        tkhd.setLayer(track.getTrackMetaData().getLayer());
        tkhd.setModificationTime(getDate());
        tkhd.setTrackId(track.getTrackMetaData().getTrackId());
        tkhd.setVolume(track.getTrackMetaData().getVolume());
        return tkhd;
    }


    protected Box createMvhd() {
        MovieHeaderBox mvhd = new MovieHeaderBox();
        mvhd.setVersion(1);
        mvhd.setCreationTime(getDate());
        mvhd.setModificationTime(getDate());
        mvhd.setDuration(0);//no duration in moov for fragmented movies
        long movieTimeScale = theTrack.getTrackMetaData().getTimescale();
        mvhd.setTimescale(movieTimeScale);
        mvhd.setNextTrackId(theTrack.getTrackMetaData().getTrackId() + 1);
        return mvhd;
    }


    protected Box createStbl(Track track) {
        SampleTableBox stbl = new SampleTableBox();

        createStsd(track, stbl);
        stbl.addBox(new TimeToSampleBox());
        stbl.addBox(new SampleToChunkBox());
        stbl.addBox(new SampleSizeBox());
        stbl.addBox(new StaticChunkOffsetBox());
        return stbl;
    }

    protected void createStsd(Track track, SampleTableBox stbl) {
        stbl.addBox(track.getSampleDescriptionBox());
    }

    protected Box createMinf(Track track) {
        MediaInformationBox minf = new MediaInformationBox();
        if (track.getHandler().equals("vide")) {
            minf.addBox(new VideoMediaHeaderBox());
        } else if (track.getHandler().equals("soun")) {
            minf.addBox(new SoundMediaHeaderBox());
        } else if (track.getHandler().equals("text")) {
            minf.addBox(new NullMediaHeaderBox());
        } else if (track.getHandler().equals("subt")) {
            minf.addBox(new SubtitleMediaHeaderBox());
        } else if (track.getHandler().equals("hint")) {
            minf.addBox(new HintMediaHeaderBox());
        } else if (track.getHandler().equals("sbtl")) {
            minf.addBox(new NullMediaHeaderBox());
        }
        minf.addBox(createDinf());
        minf.addBox(createStbl(track));
        return minf;
    }

    protected Box createMdiaHdlr(Track track) {
        HandlerBox hdlr = new HandlerBox();
        hdlr.setHandlerType(track.getHandler());
        return hdlr;
    }

    protected Box createMdhd(Track track) {
        MediaHeaderBox mdhd = new MediaHeaderBox();
        mdhd.setCreationTime(track.getTrackMetaData().getCreationTime());
        mdhd.setModificationTime(getDate());
        mdhd.setDuration(0);//no duration in moov for fragmented movies
        mdhd.setTimescale(track.getTrackMetaData().getTimescale());
        mdhd.setLanguage(track.getTrackMetaData().getLanguage());
        return mdhd;
    }

    public Date getDate() {
        return date;
    }


    protected DataInformationBox createDinf() {
        DataInformationBox dinf = new DataInformationBox();
        DataReferenceBox dref = new DataReferenceBox();
        dinf.addBox(dref);
        DataEntryUrlBox url = new DataEntryUrlBox();
        url.setFlags(1);
        dref.addBox(url);
        return dinf;
    }

    protected Box createMdia(Track track) {
        MediaBox mdia = new MediaBox();
        mdia.addBox(createMdhd(track));
        mdia.addBox(createMdiaHdlr(track));
        mdia.addBox(createMinf(track));
        return mdia;
    }

    protected Box createTrak(Track track) {
        TrackBox trackBox = new TrackBox();
        trackBox.addBox(createTkhd(track));
        Box edts = createEdts(track);
        if (edts != null) {
            trackBox.addBox(edts);
        }
        trackBox.addBox(createMdia(track));
        return trackBox;
    }

    protected Box createEdts(Track track) {
        if (track.getEdits() != null && track.getEdits().size() > 0) {
            EditListBox elst = new EditListBox();
            elst.setVersion(1);
            List<EditListBox.Entry> entries = new ArrayList<EditListBox.Entry>();

            for (Edit edit : track.getEdits()) {
                entries.add(new EditListBox.Entry(elst,
                        Math.round(edit.getSegmentDuration() * track.getTrackMetaData().getTimescale()),
                        edit.getMediaTime() * track.getTrackMetaData().getTimescale() / edit.getTimeScale(),
                        edit.getMediaRate()));
            }

            elst.setEntries(entries);
            EditBox edts = new EditBox();
            edts.addBox(elst);
            return edts;
        } else {
            return null;
        }
    }


    protected void addContentProtection(RepresentationType representation) {
        List<String> keyIds = new ArrayList<String>();
        if (theTrack instanceof CencEncryptedTrack) {
            keyIds.add(((CencEncryptedTrack) theTrack).getDefaultKeyId().toString());
            for (GroupEntry ge : theTrack.getSampleGroups().keySet()) {
                if (ge instanceof CencSampleEncryptionInformationGroupEntry) {
                    if (((CencSampleEncryptionInformationGroupEntry) ge).getKid() != null) {
                        keyIds.add(((CencSampleEncryptionInformationGroupEntry) ge).getKid().toString());
                    }
                }
            }

        }

        if (!keyIds.isEmpty()) {
            DescriptorType contentProtection = representation.addNewContentProtection();
            final DefaultKIDAttribute defaultKIDAttribute = DefaultKIDAttribute.Factory.newInstance();

            defaultKIDAttribute.setDefaultKID(keyIds);
            contentProtection.set(defaultKIDAttribute);
            contentProtection.setSchemeIdUri("urn:mpeg:dash:mp4protection:2011");
            contentProtection.setValue("cenc");
        }

        if (psshs != null) {
            for (ProtectionSystemSpecificHeaderBox pssh : psshs) {
                DescriptorType dt = representation.addNewContentProtection();
                byte[] psshContent = pssh.getContent();
                dt.setSchemeIdUri("urn:uuid:" + UUIDConverter.convert(pssh.getSystemId()).toString());
                if (Arrays.equals(ProtectionSystemSpecificHeaderBox.PLAYREADY_SYSTEM_ID, pssh.getSystemId())) {
                    dt.setValue("MSPR 2.0");
                    Node playReadyCPN = dt.getDomNode();
                    Document d = playReadyCPN.getOwnerDocument();
                    Element pro = d.createElementNS("urn:microsoft:playready", "pro");
                    Element prPssh = d.createElementNS("urn:mpeg:cenc:2013", "pssh");

                    pro.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(psshContent)));
                    prPssh.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(boxToBytes(pssh))));

                    playReadyCPN.appendChild(pro);
                    playReadyCPN.appendChild(prPssh);
                }
                if (Arrays.equals(ProtectionSystemSpecificHeaderBox.WIDEVINE, pssh.getSystemId())) {
                    // Widevvine
                    Node widevineCPN = dt.getDomNode();
                    Document d = widevineCPN.getOwnerDocument();
                    Element wvPssh = d.createElementNS("urn:mpeg:cenc:2013", "pssh");
                    wvPssh.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(boxToBytes(pssh))));

                    widevineCPN.appendChild(wvPssh);
                }
            }
        }
    }

    public int size() {
        return segmentStartSamples.length;
    }

    public Container get(int index) {

        List<Box> moofMdat = new ArrayList<Box>();
        long startSample = segmentStartSamples[index];
        long endSample;
        if (index + 1 < segmentStartSamples.length) {
            endSample = segmentStartSamples[index + 1];
        } else {
            endSample = theTrack.getSamples().size() - 1;
        }

        long fragmentStartSample;
        long fragmentEndSample;
        do {
            fragmentStartSample = startSample;
            int fIndex = Arrays.binarySearch(fragmentStartSamples, startSample);
            if (fIndex + 1 < fragmentStartSamples.length) {
                fragmentEndSample = fragmentStartSamples[index + 1];
            } else {
                fragmentEndSample = theTrack.getSamples().size() + 1;
            }
            moofMdat.add(createMoof(fragmentStartSample, fragmentEndSample, theTrack, fIndex + 1)); // it's one bases
            moofMdat.add(createMdat(fragmentStartSample, fragmentEndSample));

        } while (fragmentEndSample < endSample);


        return new ListContainer(moofMdat);
    }


    /**
     * Creates a 'moof' box for a given sequence of samples.
     *
     * @param startSample    low endpoint (inclusive) of the sample sequence
     * @param endSample      high endpoint (exclusive) of the sample sequence
     * @param track          source of the samples
     * @param sequenceNumber the fragment index of the requested list of samples
     * @return the list of TrackRun boxes.
     */
    protected Box createMoof(long startSample, long endSample, Track track, int sequenceNumber) {
        MovieFragmentBox moof = new MovieFragmentBox();
        createMfhd(sequenceNumber, moof);
        createTraf(startSample, endSample, track, moof);

        TrackRunBox firstTrun = moof.getTrackRunBoxes().get(0);
        firstTrun.setDataOffset(1); // dummy to make size correct
        firstTrun.setDataOffset((int) (8 + moof.getSize())); // mdat header + moof size

        return moof;
    }

    protected void createMfhd(int sequenceNumber, MovieFragmentBox parent) {
        MovieFragmentHeaderBox mfhd = new MovieFragmentHeaderBox();
        mfhd.setSequenceNumber(sequenceNumber);
        parent.addBox(mfhd);
    }


    public Container getIndexSegment() {
        SegmentIndexBox sidx = new SegmentIndexBox();
        sidx.setVersion(0);
        sidx.setFlags(0);
        sidx.setReserved(0);

        Container initSegment = getInitSegment();
        TrackHeaderBox tkhd = Path.getPath(initSegment, "/moov[0]/trak[0]/tkhd[0]");
        MediaHeaderBox mdhd = Path.getPath(initSegment, "/moov[0]/trak[0]/mdia[0]/mdhd[0]");
        sidx.setReferenceId(tkhd.getTrackId());
        sidx.setTimeScale(mdhd.getTimescale());
        // we only have one
        long[] ptss = getPtss(Path.<TrackRunBox>getPath(get(0), "/moof[0]/traf[0]/trun[0]"));
        Arrays.sort(ptss); // index 0 has now the earliest presentation time stamp!
        long timeMappingEdit = getTimeMappingEditTime(initSegment);
        sidx.setEarliestPresentationTime(ptss[0] - timeMappingEdit);
        List<SegmentIndexBox.Entry> entries = sidx.getEntries();

        TrackExtendsBox trex = Path.getPath(initSegment, "/moov[0]/mvex[0]/trex[0]");

        // ugly code ...

        for (Container c : this) {
            int size = 0;
            for (Box box : c.getBoxes()) {
                size += l2i(box.getSize());
            }
            MovieFragmentBox moof = Path.getPath(c, "moof[0]");
            SegmentIndexBox.Entry entry = new SegmentIndexBox.Entry();
            entries.add(entry);
            entry.setReferencedSize(size);
            TrackRunBox trun = Path.getPath(moof, "traf[0]/trun[0]");
            ptss = getPtss(trun);
            entry.setSapType(SapHelper.getFirstFrameSapType(ptss, SapHelper.getSampleFlags(0, trun, trex)));
            entry.setSubsegmentDuration(Timing.getDuration(Path.<TrackRunBox>getPath(moof, "traf[0]/trun[0]")));
            entry.setStartsWithSap((byte) 1); // we know it - no need to lookup
        }

        sidx.setFirstOffset(0);
        return new ListContainer(Collections.<Box>singletonList(sidx));
    }

    protected void createTfdt(long startSample, Track track, TrackFragmentBox parent) {
        TrackFragmentBaseMediaDecodeTimeBox tfdt = new TrackFragmentBaseMediaDecodeTimeBox();
        tfdt.setVersion(1);
        long startTime = 0;
        long[] times = track.getSampleDurations();
        for (int i = 1; i < startSample; i++) {
            startTime += times[i - 1];
        }
        tfdt.setBaseMediaDecodeTime(startTime);
        parent.addBox(tfdt);
    }

    /**
     * Gets the sizes of a sequence of samples.
     *
     * @param startSample low endpoint (inclusive) of the sample sequence
     * @param endSample   high endpoint (exclusive) of the sample sequence
     * @return the sample sizes in the given interval
     */
    protected long[] getSampleSizes(long startSample, long endSample) {
        List<Sample> samples = getSamples(startSample, endSample);

        long[] sampleSizes = new long[samples.size()];
        for (int i = 0; i < sampleSizes.length; i++) {
            sampleSizes[i] = samples.get(i).getSize();
        }
        return sampleSizes;
    }


    /**
     * Creates one or more theTrack run boxes for a given sequence.
     *
     * @param startSample low endpoint (inclusive) of the sample sequence
     * @param endSample   high endpoint (exclusive) of the sample sequence
     * @param track       source of the samples
     * @param parent      the created box must be added to this box
     */
    protected void createTrun(long startSample, long endSample, Track track, TrackFragmentBox parent) {
        TrackRunBox trun = new TrackRunBox();
        trun.setVersion(1);
        long[] sampleSizes = getSampleSizes(startSample, endSample);

        trun.setSampleDurationPresent(true);
        trun.setSampleSizePresent(true);
        List<TrackRunBox.Entry> entries = new ArrayList<TrackRunBox.Entry>(l2i(endSample - startSample));


        List<CompositionTimeToSample.Entry> compositionTimeEntries = track.getCompositionTimeEntries();
        int compositionTimeQueueIndex = 0;
        CompositionTimeToSample.Entry[] compositionTimeQueue =
                compositionTimeEntries != null && compositionTimeEntries.size() > 0 ?
                        compositionTimeEntries.toArray(new CompositionTimeToSample.Entry[compositionTimeEntries.size()]) : null;
        long compositionTimeEntriesLeft = compositionTimeQueue != null ? compositionTimeQueue[compositionTimeQueueIndex].getCount() : -1;


        trun.setSampleCompositionTimeOffsetPresent(compositionTimeEntriesLeft > 0);

        // fast forward composition stuff
        for (long i = 1; i < startSample; i++) {
            if (compositionTimeQueue != null) {
                //trun.setSampleCompositionTimeOffsetPresent(true);
                if (--compositionTimeEntriesLeft == 0 && (compositionTimeQueue.length - compositionTimeQueueIndex) > 1) {
                    compositionTimeQueueIndex++;
                    compositionTimeEntriesLeft = compositionTimeQueue[compositionTimeQueueIndex].getCount();
                }
            }
        }

        boolean sampleFlagsRequired = (track.getSampleDependencies() != null && !track.getSampleDependencies().isEmpty() ||
                track.getSyncSamples() != null && track.getSyncSamples().length != 0);

        trun.setSampleFlagsPresent(sampleFlagsRequired);

        for (int i = 0; i < sampleSizes.length; i++) {
            TrackRunBox.Entry entry = new TrackRunBox.Entry();
            entry.setSampleSize(sampleSizes[i]);
            if (sampleFlagsRequired) {
                //if (false) {
                SampleFlags sflags = new SampleFlags();

                if (track.getSampleDependencies() != null && !track.getSampleDependencies().isEmpty()) {
                    SampleDependencyTypeBox.Entry e = track.getSampleDependencies().get(i);
                    sflags.setSampleDependsOn(e.getSampleDependsOn());
                    sflags.setSampleIsDependedOn(e.getSampleIsDependentOn());
                    sflags.setSampleHasRedundancy(e.getSampleHasRedundancy());
                }
                if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                    // we have to mark non-sync samples!
                    if (Arrays.binarySearch(track.getSyncSamples(), startSample + i) >= 0) {
                        sflags.setSampleIsDifferenceSample(false);
                        sflags.setSampleDependsOn(2);
                    } else {
                        sflags.setSampleIsDifferenceSample(true);
                        sflags.setSampleDependsOn(1);
                    }
                }
                // i don't have sample degradation
                entry.setSampleFlags(sflags);

            }

            entry.setSampleDuration(track.getSampleDurations()[l2i(startSample + i - 1)]);

            if (compositionTimeQueue != null) {
                entry.setSampleCompositionTimeOffset(compositionTimeQueue[compositionTimeQueueIndex].getOffset());
                if (--compositionTimeEntriesLeft == 0 && (compositionTimeQueue.length - compositionTimeQueueIndex) > 1) {
                    compositionTimeQueueIndex++;
                    compositionTimeEntriesLeft = compositionTimeQueue[compositionTimeQueueIndex].getCount();
                }
            }
            entries.add(entry);
        }

        trun.setEntries(entries);

        parent.addBox(trun);
    }

    protected void createTraf(long startSample, long endSample, Track track, MovieFragmentBox parent) {
        TrackFragmentBox traf = new TrackFragmentBox();
        parent.addBox(traf);
        createTfhd(traf);
        TrackFragmentHeaderBox tfhd = (TrackFragmentHeaderBox) traf.getBoxes().get(traf.getBoxes().size()-1);
        createTfdt(startSample, track, traf);
        createTrun(startSample, endSample, track, traf);
        TrackRunBox trun = (TrackRunBox) traf.getBoxes().get(traf.getBoxes().size()-1);
        SampleFlags first = null;
        SampleFlags second = null;
        boolean allFllowingSame = true;

        for (TrackRunBox.Entry entry : trun.getEntries()) {
            if (first == null) {
                first = entry.getSampleFlags();
            } else if (second == null) {
                second = entry.getSampleFlags();
            } else {
                allFllowingSame &= second.equals(entry.getSampleFlags());
            }
        }
        if (allFllowingSame && second != null) {
            trun.setSampleFlagsPresent(false);
            trun.setFirstSampleFlags(first);
            tfhd.setDefaultSampleFlags(second);
        }


        createSubs(startSample, endSample, track, traf);

        if (track instanceof CencEncryptedTrack) {
            createSaiz(startSample, endSample, (CencEncryptedTrack) track, traf);
            createSenc(startSample, endSample, (CencEncryptedTrack) track, traf);
            createSaio(traf);
        }


        Map<String, List<GroupEntry>> groupEntryFamilies = new HashMap<String, List<GroupEntry>>();
        for (Map.Entry<GroupEntry, long[]> sg : track.getSampleGroups().entrySet()) {
            String type = sg.getKey().getType();
            List<GroupEntry> groupEntries = groupEntryFamilies.get(type);
            if (groupEntries == null) {
                groupEntries = new ArrayList<GroupEntry>();
                groupEntryFamilies.put(type, groupEntries);
            }
            groupEntries.add(sg.getKey());
        }


        for (Map.Entry<String, List<GroupEntry>> sg : groupEntryFamilies.entrySet()) {
            SampleGroupDescriptionBox sgpd = new SampleGroupDescriptionBox();
            String type = sg.getKey();
            sgpd.setGroupEntries(sg.getValue());
            SampleToGroupBox sbgp = new SampleToGroupBox();
            sbgp.setGroupingType(type);
            SampleToGroupBox.Entry last = null;
            for (int i = l2i(startSample - 1); i < l2i(endSample - 1); i++) {
                int index = 0;
                for (int j = 0; j < sg.getValue().size(); j++) {
                    GroupEntry groupEntry = sg.getValue().get(j);
                    long[] sampleNums = track.getSampleGroups().get(groupEntry);
                    if (Arrays.binarySearch(sampleNums, i) >= 0) {
                        index = j + 1;
                    }
                }
                if (last == null || last.getGroupDescriptionIndex() != index) {
                    last = new SampleToGroupBox.Entry(1, index);
                    sbgp.getEntries().add(last);
                } else {
                    last.setSampleCount(last.getSampleCount() + 1);
                }
            }
            traf.addBox(sgpd);
            traf.addBox(sbgp);
        }


    }

    protected void createSubs(long startSample, long endSample, Track track, TrackFragmentBox traf) {
        SubSampleInformationBox subs = track.getSubsampleInformationBox();
        if (subs != null) {
            SubSampleInformationBox fragmentSubs = new SubSampleInformationBox();
            fragmentSubs.setEntries(subs.getEntries().subList(l2i(startSample - 1), l2i(endSample - 1)));
            traf.addBox(fragmentSubs);
        }

    }

    protected void createTfhd(TrackFragmentBox parent) {
        TrackFragmentHeaderBox tfhd = new TrackFragmentHeaderBox();
        SampleFlags sf = new SampleFlags();

        tfhd.setDefaultSampleFlags(sf);
        tfhd.setBaseDataOffset(-1);

        tfhd.setTrackId(theTrack.getTrackMetaData().getTrackId());
        tfhd.setDefaultBaseIsMoof(true);
        parent.addBox(tfhd);
    }


    protected void createSenc(long startSample, long endSample, CencEncryptedTrack track, TrackFragmentBox parent) {
        SampleEncryptionBox senc = new SampleEncryptionBox();
        senc.setSubSampleEncryption(track.hasSubSampleEncryption());
        senc.setEntries(track.getSampleEncryptionEntries().subList(l2i(startSample - 1), l2i(endSample - 1)));
        parent.addBox(senc);
    }

    protected void createSaio(TrackFragmentBox parent) {
        SampleAuxiliaryInformationOffsetsBox saio = new SampleAuxiliaryInformationOffsetsBox();
        parent.addBox(saio);
        assert parent.getBoxes(TrackRunBox.class).size() == 1 : "Don't know how to deal with multiple Track Run Boxes when encrypting";
        saio.setAuxInfoType("cenc");
        saio.setFlags(1);
        long offset = 0;
        offset += 8; // traf header till 1st child box
        for (Box box : parent.getBoxes()) {
            if (box instanceof SampleEncryptionBox) {
                offset += ((SampleEncryptionBox) box).getOffsetToFirstIV();
                break;
            } else {
                offset += box.getSize();
            }
        }
        MovieFragmentBox moof = (MovieFragmentBox) parent.getParent();
        offset += 16; // traf header till 1st child box
        for (Box box : moof.getBoxes()) {
            if (box == parent) {
                break;
            } else {
                offset += box.getSize();
            }

        }

        saio.setOffsets(new long[]{offset});

    }

    protected void createSaiz(long startSample, long endSample, CencEncryptedTrack track, TrackFragmentBox parent) {
        SampleDescriptionBox sampleDescriptionBox = track.getSampleDescriptionBox();

        TrackEncryptionBox tenc = Path.getPath(sampleDescriptionBox, "enc.[0]/sinf[0]/schi[0]/tenc[0]");

        SampleAuxiliaryInformationSizesBox saiz = new SampleAuxiliaryInformationSizesBox();
        saiz.setAuxInfoType("cenc");
        saiz.setFlags(1);
        if (track.hasSubSampleEncryption()) {
            short[] sizes = new short[l2i(endSample - startSample)];
            List<CencSampleAuxiliaryDataFormat> auxs =
                    track.getSampleEncryptionEntries().subList(l2i(startSample - 1), l2i(endSample - 1));
            for (int i = 0; i < sizes.length; i++) {
                sizes[i] = (short) auxs.get(i).getSize();
            }
            saiz.setSampleInfoSizes(sizes);
        } else {
            saiz.setDefaultSampleInfoSize(tenc.getDefaultIvSize());
            saiz.setSampleCount(l2i(endSample - startSample));
        }
        parent.addBox(saiz);
    }

    protected Box createMdat(final long startSample, final long endSample) {

        class Mdat implements Box {
            Container parent;
            long size_ = -1;

            public Container getParent() {
                return parent;
            }

            public void setParent(Container parent) {
                this.parent = parent;
            }

            public long getOffset() {
                throw new RuntimeException("Doesn't have any meaning for programmatically created boxes");
            }

            public long getSize() {
                if (size_ != -1) return size_;
                long size = 8; // I don't expect 2gig fragments
                for (Sample sample : getSamples(startSample, endSample)) {
                    size += sample.getSize();
                }
                size_ = size;
                return size;
            }

            public String getType() {
                return "mdat";
            }

            public void getBox(WritableByteChannel writableByteChannel) throws IOException {
                ByteBuffer header = ByteBuffer.allocate(8);
                IsoTypeWriter.writeUInt32(header, l2i(getSize()));
                header.put(IsoFile.fourCCtoBytes(getType()));
                header.rewind();
                writableByteChannel.write(header);

                List<Sample> samples = getSamples(startSample, endSample);
                for (Sample sample : samples) {
                    sample.writeTo(writableByteChannel);
                }


            }

            public void parse(DataSource fileChannel, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {

            }
        }

        return new Mdat();
    }

    /**
     * Gets all samples starting with <code>startSample</code> (one based -&gt; one is the first) and
     * ending with <code>endSample</code> (exclusive).
     *
     * @param startSample low endpoint (inclusive) of the sample sequence
     * @param endSample   high endpoint (exclusive) of the sample sequence
     * @return a <code>List&lt;ByteBuffer&gt;</code> of raw samples
     */
    protected List<Sample> getSamples(long startSample, long endSample) {
        // since startSample and endSample are one-based substract 1 before addressing list elements
        return theTrack.getSamples().subList(l2i(startSample) - 1, l2i(endSample) - 1);
    }

    String getCodec() {
        return DashHelper.getRfc6381Codec(theTrack.getSampleDescriptionBox().getSampleEntry());
    }

    public RepresentationType getLiveRepresentation() {
        RepresentationType representation = getBaseRepresentation();
        SegmentTemplateType segmentTemplate = representation.addNewSegmentTemplate();
        SegmentTimelineType segmentTimeline = segmentTemplate.addNewSegmentTimeline();


        TrackRunBox firstTrun = Path.getPath(this.get(0), "moof/traf/trun");

        long[] ptss = getPtss(firstTrun);
        Arrays.sort(ptss); // index 0 has now the earliest presentation time stamp!
        long timeMappingEdit = getTimeMappingEditTime(getInitSegment());
        long startTime = ptss[0] - timeMappingEdit;

        SegmentTimelineType.S lastSegmentTimelineS = null;
        for (Container container : this) {
            long duration = 0;
            List<TrackRunBox> truns = Path.getPaths(container, "moof/traf/trun");
            for (TrackRunBox trun : truns) {
                duration += getDuration(trun);
            }

            if (lastSegmentTimelineS != null && lastSegmentTimelineS.getD().equals(BigInteger.valueOf(duration))) {
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


        return representation;
    }

    public RepresentationType getBaseRepresentation() {
        RepresentationType representation = RepresentationType.Factory.newInstance();
        representation.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");
        if (theTrack.getHandler().equals("vide")) {

            long videoHeight = (long) theTrack.getTrackMetaData().getHeight();
            long videoWidth = (long) theTrack.getTrackMetaData().getWidth();
            double framesPerSecond = (double) (theTrack.getSamples().size() * theTrack.getTrackMetaData().getTimescale()) / theTrack.getDuration();


            representation.setMimeType("video/mp4");
            representation.setCodecs(getCodec());
            representation.setWidth(videoWidth);
            representation.setHeight(videoHeight);
            representation.setFrameRate(convertFramerate(framesPerSecond));
            representation.setSar("1:1");
            // too hard to find it out. Ignoring even though it should be set according to DASH-AVC-264-v2.00-hd-mca.pdf
        } else if (theTrack.getHandler().equals("soun")) {

            AudioSampleEntry ase = (AudioSampleEntry) theTrack.getSampleDescriptionBox().getSampleEntry();

            representation.setMimeType("audio/mp4");
            representation.setCodecs(DashHelper.getRfc6381Codec(ase));
            representation.setAudioSamplingRate("" + DashHelper.getAudioSamplingRate(ase));

            DescriptorType audio_channel_conf = representation.addNewAudioChannelConfiguration();
            DashHelper.ChannelConfiguration cc = DashHelper.getChannelConfiguration(ase);
            audio_channel_conf.setSchemeIdUri(cc.schemeIdUri);
            audio_channel_conf.setValue(cc.value);


        } else if (theTrack.getHandler().equals("subt") || theTrack.getHandler().equals("text")) {
            representation.setMimeType("application/mp4");
            representation.setCodecs(getCodec());

            representation.setStartWithSAP(1);

        }

        long size = 0;
        List<Sample> samples = theTrack.getSamples();
        for (int i = 0; i < Math.min(samples.size(), 10000); i++) {
            size += samples.get(i).getSize();
        }
        size = (size / Math.min(theTrack.getSamples().size(), 10000)) * theTrack.getSamples().size();

        double duration = (double) theTrack.getDuration() / theTrack.getTrackMetaData().getTimescale();

        representation.setBandwidth((long) ((size * 8 / duration / 100)) * 100);

        addContentProtection(representation);

        return representation;
    }

    public RepresentationType getOnDemandRepresentation() {
        RepresentationType representation = getBaseRepresentation();

        SegmentBaseType segBaseType = representation.addNewSegmentBase();

        segBaseType.setTimescale(theTrack.getTrackMetaData().getTimescale());
        segBaseType.setIndexRangeExact(true);

        long initSize = 0;
        for (Box b : getInitSegment().getBoxes()) {
            initSize += b.getSize();
        }
        URLType initialization = segBaseType.addNewInitialization();
        long indexSize = 0;
        for (Box b : getIndexSegment().getBoxes()) {
            indexSize += b.getSize();
        }

        segBaseType.setIndexRange(String.format("%s-%s", initSize, initSize + indexSize));
        initialization.setRange(String.format("0-%s", initSize - 1));

        return representation;


    }

}
