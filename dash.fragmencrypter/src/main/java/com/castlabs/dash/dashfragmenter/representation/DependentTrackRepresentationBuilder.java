package com.castlabs.dash.dashfragmenter.representation;

import com.castlabs.dash.helpers.DashHelper;
import com.castlabs.dash.helpers.SapHelper;
import com.coremedia.iso.BoxParser;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.*;
import com.coremedia.iso.boxes.fragment.*;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;
import com.googlecode.mp4parser.BasicContainer;
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.authoring.Edit;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.tracks.CencEncryptedTrack;
import com.googlecode.mp4parser.boxes.dece.SampleEncryptionBox;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.GroupEntry;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.SampleGroupDescriptionBox;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.SampleToGroupBox;
import com.googlecode.mp4parser.boxes.threegpp26244.SegmentIndexBox;
import com.googlecode.mp4parser.util.Path;
import com.mp4parser.iso14496.part12.SampleAuxiliaryInformationOffsetsBox;
import com.mp4parser.iso14496.part12.SampleAuxiliaryInformationSizesBox;
import com.mp4parser.iso14496.part12.TrackReferenceTypeBox;
import com.mp4parser.iso14496.part15.HevcConfigurationBox;
import com.mp4parser.iso23001.part7.CencSampleAuxiliaryDataFormat;
import com.mp4parser.iso23001.part7.TrackEncryptionBox;
import mpegCenc2013.DefaultKIDAttribute;
import mpegDashSchemaMpd2011.DescriptorType;
import mpegDashSchemaMpd2011.RepresentationType;
import mpegDashSchemaMpd2011.SegmentBaseType;
import mpegDashSchemaMpd2011.URLType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.*;

import static com.castlabs.dash.helpers.ManifestHelper.convertFramerate;
import static com.castlabs.dash.helpers.Timing.getPtss;
import static com.castlabs.dash.helpers.Timing.getTimeMappingEditTime;
import static com.googlecode.mp4parser.util.CastUtils.l2i;

public class DependentTrackRepresentationBuilder extends AbstractList<Container> implements RepresentationBuilder {
    private Track main;
    private Track dependent;
    private String dependencyType;
    long[] fragmentStartSamples = new long[0];

    public Track getTrack() {
        return main;
    }

    public DependentTrackRepresentationBuilder(Track main, Track dependent, String dependencyType, int minFramesPerFragment) {
        this.main = main;
        this.dependent = dependent;
        this.dependencyType = dependencyType;
        assert main.getSamples().size() == dependent.getSamples().size();

        fragmentStartSamples = main.getSyncSamples();
        if (fragmentStartSamples == null) {
            int samples = main.getSamples().size();
            fragmentStartSamples = new long[samples / minFramesPerFragment];
            for (int i = 0; i < fragmentStartSamples.length; i++) {
                fragmentStartSamples[i] = i * minFramesPerFragment + 1;
            }
        }

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


        for (Container c : this) {
            int size = 0;
            for (Box box : c.getBoxes()) {
                size += l2i(box.getSize());
            }
            MovieFragmentBox moof = Path.getPath(c, "/moof[0]");
            SegmentIndexBox.Entry entry = new SegmentIndexBox.Entry();
            entries.add(entry);
            entry.setReferencedSize(size);
            TrackRunBox trun = Path.<TrackRunBox>getPath(moof, "traf[0]/trun[0]");
            ptss = getPtss(trun);
            entry.setSapType(SapHelper.getFirstFrameSapType(ptss, SapHelper.getSampleFlags(0,trun, trex)));
            entry.setSubsegmentDuration(getTrunDuration(Path.<TrackRunBox>getPath(moof, "traf[0]/trun[0]")));
            entry.setStartsWithSap((byte) 1); // we know it - no need to lookup
        }

        sidx.setFirstOffset(0);
        return new ListContainer(Collections.<Box>singletonList(sidx));
    }

    protected long getTrunDuration(TrackRunBox trun) {
        final List<TrackRunBox.Entry> trunEntries = trun.getEntries();
        long duration = 0;
        for (TrackRunBox.Entry trunEntry : trunEntries) {
            duration += trunEntry.getSampleDuration();
        }
        return duration;
    }




    public Container getInitSegment() {
        BasicContainer bv = new BasicContainer();
        List<Box> initSegment = new ArrayList<Box>();
        List<String> minorBrands = new ArrayList<String>();
        minorBrands.add("isom");
        minorBrands.add("iso6");
        minorBrands.add("avc1");
        initSegment.add(new FileTypeBox("isom", 0, minorBrands));
        initSegment.add(createMoov());
        bv.setBoxes(initSegment);
        return bv;
    }

    @Override
    public int size() {
        return fragmentStartSamples.length;
    }

    @Override
    public Container get(int index) {
        BasicContainer moofMdat = new BasicContainer();
        long startSample1 = fragmentStartSamples[index];
        long endSample1;

        if (index + 1 < fragmentStartSamples.length) {
            endSample1 = fragmentStartSamples[index + 1];
        } else {
            endSample1 = main.getSamples().size() - 1;
        }

        moofMdat.addBox(createMoof(startSample1, endSample1, main, index * 2 + 1)); // it's one bases
        moofMdat.addBox(createMdat(startSample1, endSample1, main));

        moofMdat.addBox(createMoof(startSample1, endSample1, dependent, index * 2 + 2)); // it's one bases
        moofMdat.addBox(createMdat(startSample1, endSample1, dependent));
        return moofMdat;
    }

    public Date getDate() {
        return new Date();
    }

    protected Box createMdat(final long startSample, final long endSample, Track t) {
        final List<Sample> samples = t.getSamples().subList(l2i(startSample) - 1, l2i(endSample) - 1);
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
                for (Sample sample : samples) {
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

                for (Sample sample : samples) {
                    sample.writeTo(writableByteChannel);
                }


            }

            public void parse(DataSource fileChannel, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {

            }
        }

        return new Mdat();
    }

    protected void createTfhd(TrackFragmentBox parent, Track t) {
        TrackFragmentHeaderBox tfhd = new TrackFragmentHeaderBox();
        SampleFlags sf = new SampleFlags();

        tfhd.setDefaultSampleFlags(sf);
        tfhd.setBaseDataOffset(-1);
        tfhd.setTrackId(t.getTrackMetaData().getTrackId());
        tfhd.setDefaultBaseIsMoof(true);
        parent.addBox(tfhd);
    }

    protected void createMfhd(int sequenceNumber, MovieFragmentBox parent) {
        MovieFragmentHeaderBox mfhd = new MovieFragmentHeaderBox();
        mfhd.setSequenceNumber(sequenceNumber);
        parent.addBox(mfhd);
    }

    protected void createTraf(long startSample, long endSample, Track track, MovieFragmentBox parent) {
        TrackFragmentBox traf = new TrackFragmentBox();
        parent.addBox(traf);
        createTfhd(traf, track);
        createTfdt(startSample, track, traf);
        createTrun(startSample, endSample, track, traf);

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


    /**
     * Gets the sizes of a sequence of samples.
     *
     * @param startSample low endpoint (inclusive) of the sample sequence
     * @param endSample   high endpoint (exclusive) of the sample sequence
     * @return the sample sizes in the given interval
     */
    protected long[] getSampleSizes(long startSample, long endSample, Track t) {
        List<Sample> samples = t.getSamples().subList(l2i(startSample - 1), l2i(endSample - 1));

        long[] sampleSizes = new long[samples.size()];
        for (int i = 0; i < sampleSizes.length; i++) {
            sampleSizes[i] = samples.get(i).getSize();
        }
        return sampleSizes;
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
     * Creates one or more track run boxes for a given sequence.
     *
     * @param startSample low endpoint (inclusive) of the sample sequence
     * @param endSample   high endpoint (exclusive) of the sample sequence
     * @param track       source of the samples
     * @param parent      the created box must be added to this box
     */
    protected void createTrun(long startSample, long endSample, Track track, TrackFragmentBox parent) {
        TrackRunBox trun = new TrackRunBox();
        trun.setVersion(1);
        long[] sampleSizes = getSampleSizes(startSample, endSample, track);

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

    protected Box createMvhd(Track t) {
        MovieHeaderBox mvhd = new MovieHeaderBox();
        mvhd.setVersion(1);
        mvhd.setCreationTime(getDate());
        mvhd.setModificationTime(getDate());
        mvhd.setDuration(0);//no duration in moov for fragmented movies
        long movieTimeScale = t.getTrackMetaData().getTimescale();
        mvhd.setTimescale(movieTimeScale);
        mvhd.setNextTrackId(t.getTrackMetaData().getTrackId() + 1);
        return mvhd;
    }


    protected Box createMoov() {
        MovieBox movieBox = new MovieBox();

        movieBox.addBox(createMvhd(main));
        movieBox.addBox(createTrak(main));
        movieBox.addBox(createTrak(dependent));
        movieBox.addBox(createMvex());

        // metadata here
        return movieBox;
    }


    protected Box createTrex(Track t) {
        TrackExtendsBox trex = new TrackExtendsBox();
        trex.setTrackId(t.getTrackMetaData().getTrackId());
        trex.setDefaultSampleDescriptionIndex(1);
        trex.setDefaultSampleDuration(0);
        trex.setDefaultSampleSize(0);
        SampleFlags sf = new SampleFlags();
        if ("soun".equals(t.getHandler()) || "subt".equals(t.getHandler())) {
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
        mved.setFragmentDuration(main.getDuration());
        mvex.addBox(mved);

        mvex.addBox(createTrex(main));
        mvex.addBox(createTrex(dependent));
        return mvex;
    }

    protected Box createTkhd(Track t) {
        TrackHeaderBox tkhd = new TrackHeaderBox();
        tkhd.setVersion(1);
        tkhd.setFlags(7); // enabled, in movie, in previe, in poster

        tkhd.setAlternateGroup(t.getTrackMetaData().getGroup());
        tkhd.setCreationTime(t.getTrackMetaData().getCreationTime());
        // We need to take edit list box into account in trackheader duration
        // but as long as I don't support edit list boxes it is sufficient to
        // just translate media duration to movie timescale
        tkhd.setDuration(0);//no duration in moov for fragmented movies
        tkhd.setHeight(t.getTrackMetaData().getHeight());
        tkhd.setWidth(t.getTrackMetaData().getWidth());
        tkhd.setLayer(t.getTrackMetaData().getLayer());
        tkhd.setModificationTime(getDate());
        tkhd.setTrackId(t.getTrackMetaData().getTrackId());
        tkhd.setVolume(t.getTrackMetaData().getVolume());
        return tkhd;
    }

    protected Box createMdhd(Track t) {
        MediaHeaderBox mdhd = new MediaHeaderBox();
        mdhd.setCreationTime(t.getTrackMetaData().getCreationTime());
        mdhd.setModificationTime(getDate());
        mdhd.setDuration(0);//no duration in moov for fragmented movies
        mdhd.setTimescale(t.getTrackMetaData().getTimescale());
        mdhd.setLanguage(t.getTrackMetaData().getLanguage());
        return mdhd;
    }

    protected Box createStbl(Track t) {
        SampleTableBox stbl = new SampleTableBox();

        createStsd(t, stbl);
        stbl.addBox(new TimeToSampleBox());
        stbl.addBox(new SampleToChunkBox());
        stbl.addBox(new SampleSizeBox());
        stbl.addBox(new StaticChunkOffsetBox());
        return stbl;
    }

    protected void createStsd(Track track, SampleTableBox stbl) {
        stbl.addBox(track.getSampleDescriptionBox());
    }

    protected Box createMinf(Track t) {
        MediaInformationBox minf = new MediaInformationBox();
        if (t.getHandler().equals("vide")) {
            minf.addBox(new VideoMediaHeaderBox());
        } else if (t.getHandler().equals("soun")) {
            minf.addBox(new SoundMediaHeaderBox());
        } else if (t.getHandler().equals("text")) {
            minf.addBox(new NullMediaHeaderBox());
        } else if (t.getHandler().equals("subt")) {
            minf.addBox(new SubtitleMediaHeaderBox());
        } else if (t.getHandler().equals("hint")) {
            minf.addBox(new HintMediaHeaderBox());
        } else if (t.getHandler().equals("sbtl")) {
            minf.addBox(new NullMediaHeaderBox());
        }
        minf.addBox(createDinf());
        minf.addBox(createStbl(t));
        return minf;
    }

    protected Box createMdiaHdlr(Track t) {
        HandlerBox hdlr = new HandlerBox();
        hdlr.setHandlerType(t.getHandler());
        return hdlr;
    }

    protected Box createMdia(Track t) {
        MediaBox mdia = new MediaBox();
        mdia.addBox(createMdhd(t));


        mdia.addBox(createMdiaHdlr(t));


        mdia.addBox(createMinf(t));
        return mdia;
    }

    protected Box createTrak(Track t) {
        TrackBox trackBox = new TrackBox();
        trackBox.addBox(createTkhd(t));
        if (t == dependent && dependencyType != null) {
            TrackReferenceBox tref = new TrackReferenceBox();
            TrackReferenceTypeBox vdep = new TrackReferenceTypeBox(dependencyType);
            tref.addBox(vdep);
            trackBox.addBox(tref);
        }
        Box edts = createEdts(t);
        if (edts != null) {
            trackBox.addBox(edts);
        }
        trackBox.addBox(createMdia(t));
        return trackBox;
    }

    protected Box createEdts(Track t) {
        if (t.getEdits() != null && t.getEdits().size() > 0) {
            EditListBox elst = new EditListBox();
            elst.setVersion(1);
            List<EditListBox.Entry> entries = new ArrayList<EditListBox.Entry>();

            for (Edit edit : t.getEdits()) {
                entries.add(new EditListBox.Entry(elst,
                        Math.round(edit.getSegmentDuration() * t.getTrackMetaData().getTimescale()),
                        edit.getMediaTime() * t.getTrackMetaData().getTimescale() / edit.getTimeScale(),
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

    protected DataInformationBox createDinf() {
        DataInformationBox dinf = new DataInformationBox();
        DataReferenceBox dref = new DataReferenceBox();
        dinf.addBox(dref);
        DataEntryUrlBox url = new DataEntryUrlBox();
        url.setFlags(1);
        dref.addBox(url);
        return dinf;
    }


    String getCodec() {
        String codec1 = DashHelper.getRfc6381Codec(main.getSampleDescriptionBox().getSampleEntry());
        String codec2 = DashHelper.getRfc6381Codec(dependent.getSampleDescriptionBox().getSampleEntry());
        if (codec2 == null) {
            SampleEntry se = dependent.getSampleDescriptionBox().getSampleEntry();
            OriginalFormatBox frma = Path.getPath((Box) se, "sinf/frma");
            String type;
            if (frma != null) {
                type = frma.getDataFormat();
            } else {
                type = se.getType();
            }
            if (type.equals("dvhe")) {
                HevcConfigurationBox hevC = Path.getPath((Box) se, "hvcC");
                type += ".s";
                if (hevC.getBitDepthChromaMinus8() == 0) {
                    type += "e";
                }
                if (hevC.getBitDepthChromaMinus8() == 2) {
                    type += "t";
                }
                if (hevC.getBitDepthChromaMinus8() == 4) {
                    type += "w";
                }
                type += "n";
            }
            codec2 = type;
        }
        return codec1 + "," + codec2;
    }

    public RepresentationType getSegmentTemplateRepresentation() {
        RepresentationType representation = RepresentationType.Factory.newInstance();
        representation.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");
        if (main.getHandler().equals("vide")) {

            long videoHeight = (long) main.getTrackMetaData().getHeight();
            long videoWidth = (long) main.getTrackMetaData().getWidth();
            double framesPerSecond = (double) (main.getSamples().size() * main.getTrackMetaData().getTimescale()) / main.getDuration();


            representation.setMimeType("video/mp4");
            representation.setCodecs(getCodec());
            representation.setWidth(videoWidth);
            representation.setHeight(videoHeight);
            representation.setFrameRate(convertFramerate(framesPerSecond));
            representation.setSar("1:1");
            // too hard to find it out. Ignoring even though it should be set according to DASH-AVC-264-v2.00-hd-mca.pdf
        }

        if (main.getHandler().equals("soun")) {


            AudioSampleEntry ase = (AudioSampleEntry) main.getSampleDescriptionBox().getSampleEntry();

            representation.setMimeType("audio/mp4");
            representation.setCodecs(DashHelper.getRfc6381Codec(ase));
            representation.setAudioSamplingRate("" + DashHelper.getAudioSamplingRate(ase));

            DescriptorType audio_channel_conf = representation.addNewAudioChannelConfiguration();
            DashHelper.ChannelConfiguration cc = DashHelper.getChannelConfiguration(ase);
            audio_channel_conf.setSchemeIdUri(cc.schemeIdUri);
            audio_channel_conf.setValue(cc.value);

        }

        if (main.getHandler().equals("subt")) {
            representation.setMimeType("audio/mp4");
            representation.setCodecs(getCodec());

            representation.setStartWithSAP(1);

        }

        SegmentBaseType segBaseType = representation.addNewSegmentBase();

        segBaseType.setTimescale(main.getTrackMetaData().getTimescale());
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

        long size = 0;
        List<Sample> samples = main.getSamples();
        for (int i = 0; i < Math.min(samples.size(), 10000); i++) {
            size += samples.get(i).getSize();
        }
        samples = dependent.getSamples();
        for (int i = 0; i < Math.min(samples.size(), 10000); i++) {
            size += samples.get(i).getSize();
        }
        size = (size / Math.min(main.getSamples().size(), 10000)) * main.getSamples().size();

        double duration = (double) main.getDuration() / main.getTrackMetaData().getTimescale();

        representation.setBandwidth((long) ((size * 8 / duration / 100)) * 100);


        List<String> keyIds = new ArrayList<String>();
        if (main instanceof CencEncryptedTrack) {
            keyIds.add(((CencEncryptedTrack) main).getDefaultKeyId().toString());
        }
        if (dependent instanceof CencEncryptedTrack) {
            keyIds.add(((CencEncryptedTrack) dependent).getDefaultKeyId().toString());
        }

        if (!keyIds.isEmpty()) {
            DescriptorType contentProtection = representation.addNewContentProtection();
            final DefaultKIDAttribute defaultKIDAttribute = DefaultKIDAttribute.Factory.newInstance();

            defaultKIDAttribute.setDefaultKID(keyIds);
            contentProtection.set(defaultKIDAttribute);
            contentProtection.setSchemeIdUri("urn:mpeg:dash:mp4protection:2011");
            contentProtection.setValue("cenc");
        }
        return representation;
    }
}

