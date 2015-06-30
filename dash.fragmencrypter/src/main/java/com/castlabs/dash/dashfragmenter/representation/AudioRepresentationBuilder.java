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
import com.mp4parser.iso23001.part7.CencSampleAuxiliaryDataFormat;
import com.mp4parser.iso23001.part7.ProtectionSystemSpecificHeaderBox;
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

public class AudioRepresentationBuilder extends AbstractRepresentationBuilder {

    long[] fragmentStartSamples;


    public AudioRepresentationBuilder(Track track, int framesPerSegment, List<ProtectionSystemSpecificHeaderBox> psshs) {
        super(track, psshs);
        fragmentStartSamples = track.getSyncSamples();
        if (fragmentStartSamples == null || fragmentStartSamples.length == 0) {
            int samples = track.getSamples().size();
            fragmentStartSamples = new long[samples / framesPerSegment];
            for (int i = 0; i < fragmentStartSamples.length; i++) {
                fragmentStartSamples[i] = i * framesPerSegment + 1;
            }
        }
    }

    public int size() {
        return fragmentStartSamples.length;
    }

    public Container get(int index) {
        List<Box> moofMdat = new ArrayList<Box>();
        long startSample = fragmentStartSamples[index];
        long endSample;
        if (index + 1 < fragmentStartSamples.length) {
            endSample = fragmentStartSamples[index + 1];
        } else {
            endSample = theTrack.getSamples().size() - 1;
        }


        moofMdat.add(createMoof(startSample, endSample, theTrack, index * 2 + 1)); // it's one bases
        moofMdat.add(createMdat(startSample, endSample));
        return new ListContainer(moofMdat);
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

    protected void createTfhd(TrackFragmentBox parent) {
        TrackFragmentHeaderBox tfhd = new TrackFragmentHeaderBox();
        SampleFlags sf = new SampleFlags();

        tfhd.setDefaultSampleFlags(sf);
        tfhd.setBaseDataOffset(-1);
        tfhd.setTrackId(theTrack.getTrackMetaData().getTrackId());
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
        createTfhd(traf);
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
            MovieFragmentBox moof = Path.getPath(c, "/moof[0]");
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


    public RepresentationType getSegmentTemplateRepresentation() {
        RepresentationType representation = RepresentationType.Factory.newInstance();
        representation.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");

        AudioSampleEntry ase = (AudioSampleEntry) theTrack.getSampleDescriptionBox().getSampleEntry();

        representation.setMimeType("audio/mp4");
        representation.setCodecs(DashHelper.getRfc6381Codec(ase));
        representation.setAudioSamplingRate("" + DashHelper.getAudioSamplingRate(ase));

        DescriptorType audio_channel_conf = representation.addNewAudioChannelConfiguration();
        DashHelper.ChannelConfiguration cc = DashHelper.getChannelConfiguration(ase);
        audio_channel_conf.setSchemeIdUri(cc.schemeIdUri);
        audio_channel_conf.setValue(cc.value);


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
}

