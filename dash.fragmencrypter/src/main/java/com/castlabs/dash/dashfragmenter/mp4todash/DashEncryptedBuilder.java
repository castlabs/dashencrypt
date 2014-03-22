/*
 * Copyright 2012 Sebastian Annies, Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.castlabs.dash.dashfragmenter.mp4todash;

import com.coremedia.iso.IsoTypeReaderVariable;
import com.coremedia.iso.boxes.*;
import com.coremedia.iso.boxes.fragment.TrackFragmentBox;
import com.coremedia.iso.boxes.h264.AvcConfigurationBox;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;
import com.coremedia.iso.boxes.sampleentry.VisualSampleEntry;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.boxes.AbstractSampleEncryptionBox;
import com.googlecode.mp4parser.boxes.basemediaformat.TrackEncryptionBox;
import com.googlecode.mp4parser.boxes.cenc.CencSampleAuxiliaryDataFormat;
import com.googlecode.mp4parser.boxes.cenc.CommonEncryptionSampleList;
import com.googlecode.mp4parser.boxes.cenc.ProtectionSystemSpecificHeaderBox;
import com.googlecode.mp4parser.boxes.ultraviolet.SampleEncryptionBox;

import javax.crypto.SecretKey;
import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * Creates a fragmented Dash conforming MP4 file.
 */
public class DashEncryptedBuilder extends DashBuilder {
    boolean dummyIvs = false; // todo remove but at the moment needed to generate stable result

    private Map<Track, Map<Integer, SampleEncryptionBox>> sencCache = new WeakHashMap<Track, Map<Integer, SampleEncryptionBox>>();
    private Random random = new Random(5);
    private Map<Track, byte[]> keyIds = new HashMap<Track, byte[]>();
    private Map<Track, SecretKey> keys = new HashMap<Track, SecretKey>();
    private Map<byte[], byte[]> psshBoxes;
    private Map<Integer, SoftReference<List<Sample>>> sampleListCache = new WeakHashMap<Integer, SoftReference<List<Sample>>>();

    public Map<Track, byte[]> getKeyIds() {
        return keyIds;
    }

    public Map<Track, SecretKey> getKeys() {
        return keys;
    }

    public void setPsshBoxes(Map<byte[], byte[]> psshBoxes) {
        this.psshBoxes = psshBoxes;
    }


    protected ProtectionSystemSpecificHeaderBox createProtectionSystemSpecificHeaderBox(
            byte[] systemId,
            byte[] content) {

        final ProtectionSystemSpecificHeaderBox pssh = new ProtectionSystemSpecificHeaderBox();
        pssh.setSystemId(systemId);
        pssh.setContent(content);

        return pssh;
    }

    protected List<byte[]> createIvs(long startSample, long endSample, Track track, int sequenceNumber) {
        List<byte[]> ivs = new LinkedList<byte[]>();
        BigInteger one = new BigInteger("1");
        byte[] init = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};


        if (!dummyIvs) {
            random.nextBytes(init);
        }
        BigInteger ivInt = new BigInteger(1, init);
        for (long i = startSample; i < endSample; i++) {
            byte[] iv = ivInt.toByteArray();
            // iv must not always be 8 byte long. It could be shorter!!!
            // or longer
            byte[] eightByteIv = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
            System.arraycopy(
                    iv,
                    iv.length - 8 > 0 ? iv.length - 8 : 0,
                    eightByteIv,
                    (8 - iv.length) < 0 ? 0 : (8 - iv.length),
                    iv.length > 8 ? 8 : iv.length);
            ivs.add(eightByteIv);
            ivInt = ivInt.add(one);
        }


        return ivs;
    }


    @Override
    protected Box createTraf(long startSample, long endSample, Track track, int sequenceNumber) {
        TrackFragmentBox traf = new TrackFragmentBox();
        traf.addBox(createTfhd(startSample, endSample, track, sequenceNumber));
        traf.addBox(createTfdt(startSample, track));
        traf.addBox(createTrun(startSample, endSample, track, sequenceNumber));

        SampleEncryptionBox senc = (SampleEncryptionBox) createSenc(startSample, endSample, track, sequenceNumber);
        List<Short> sizes = senc.getEntrySizes();

        // SAIZ
        int samplecount = (int) (endSample - startSample);
        int flags = 1;
        String auxInfoType = "cenc";
        String auxInfoTypeParameter = "\0\0\0\0";

        //todo - if one value in sizes varies, then default is to be set to 0 and the list accordingly
        traf.addBox(createSaiz(samplecount, flags, 0, sizes, auxInfoType, auxInfoTypeParameter));
        traf.addBox(senc);


        //SAIO


        // moof.getSize() + mfhd.getSize()
        long offset = 40;  // moof hdr + mfhd hdr + traf hdr

        for (Box box : traf.getBoxes()) {
            if (box instanceof SampleEncryptionBox) {
                offset += ((SampleEncryptionBox) box).getOffsetToFirstIV();
                break;
            }
            offset += box.getSize();
        }

        traf.addBox(createSaio(flags, auxInfoType, auxInfoTypeParameter, Collections.singletonList(offset)));
        return traf;
    }


    protected AbstractSampleEncryptionBox createSenc(long startSample, long endSample, Track track, int sequenceNumber) {
        Map<Integer, SampleEncryptionBox> trackSpecificCache = sencCache.get(track);
        if (trackSpecificCache == null) {
            trackSpecificCache = new HashMap<Integer, SampleEncryptionBox>();
            sencCache.put(track, trackSpecificCache);
        }
        SampleEncryptionBox senc = trackSpecificCache.get(sequenceNumber);
        if (senc != null) {
            return senc;
        }
        senc = new SampleEncryptionBox();

        List<CencSampleAuxiliaryDataFormat> entries = new LinkedList<CencSampleAuxiliaryDataFormat>();
        LinkedList<byte[]> iVStack = new LinkedList<byte[]>(createIvs(startSample, endSample, track, sequenceNumber));

        LinkedList<Sample> samples = new LinkedList<Sample>(track.getSamples().subList(l2i(startSample) - 1, l2i(endSample) - 1));
        AvcConfigurationBox avcC = null;
        List<Box> boxes = track.getSampleDescriptionBox().getSampleEntry().getBoxes();
        for (Box box : boxes) {
            if (box instanceof AvcConfigurationBox) {
                avcC = (AvcConfigurationBox) box;
                senc.setSubSampleEncryption(true);
            }
        }

        while (iVStack.size() > 0 && samples.size() > 0) {

            CencSampleAuxiliaryDataFormat e = new CencSampleAuxiliaryDataFormat();
            entries.add(e);
            e.iv = iVStack.removeFirst();
            ByteBuffer sample = (ByteBuffer) samples.remove().asByteBuffer().rewind();


            if (avcC != null) {
                int nalLengthSize = avcC.getLengthSizeMinusOne() + 1;
                while (sample.remaining() > 0) {
                    int nalLength = l2i(IsoTypeReaderVariable.read(sample, nalLengthSize));
                    int clearBytes;
                    int nalGrossSize = nalLength + nalLengthSize;
                    if (nalGrossSize >= 112) {
                        clearBytes = 96 + nalGrossSize % 16;
                    } else {
                        clearBytes = nalGrossSize;
                    }
                    e.pairs.add(e.createPair(clearBytes, nalGrossSize - clearBytes));
                    sample.position(sample.position() + nalLength);
                }
            }
        }
        senc.setEntries(entries);
        trackSpecificCache.put(sequenceNumber, senc);
        return senc;
    }

    private Box createSaiz(int samplecount, int flags, int defaultSampleInfoSize, List<Short> sampleInforSizes, String auxInfoType, String auxInfoTypeParameter) {
        SampleAuxiliaryInformationSizesBox saiz = new SampleAuxiliaryInformationSizesBox();
        saiz.setFlags(flags);
        saiz.setAuxInfoType(auxInfoType);
        saiz.setAuxInfoTypeParameter(auxInfoTypeParameter);
        saiz.setDefaultSampleInfoSize(defaultSampleInfoSize);
        saiz.setSampleInfoSizes(sampleInforSizes);
        saiz.setSampleCount(samplecount);
        return saiz;
    }

    private Box createSaio(int flags, String auxInfoType, String auxInfoTypeParameter, List<Long> offsets) {
        SampleAuxiliaryInformationOffsetsBox saio = new SampleAuxiliaryInformationOffsetsBox();
        saio.setFlags(flags);
        saio.setAuxInfoType(auxInfoType);
        saio.setAuxInfoTypeParameter(auxInfoTypeParameter);
        saio.setOffsets(offsets);

        return saio;
    }

    /**
     * Creates a fully populated 'moov' box with all child boxes. Child boxes are:
     * <ul>
     * <li>{@link #createMvhd(com.googlecode.mp4parser.authoring.Movie) mvhd}</li>
     * <li>{@link #createMvex(com.googlecode.mp4parser.authoring.Movie)  mvex}</li>
     * <li>a {@link #createTrak(com.googlecode.mp4parser.authoring.Track, com.googlecode.mp4parser.authoring.Movie)  trak} for every track</li>
     * </ul>
     *
     * @param movie the concerned movie
     * @return fully populated 'moov'
     */
    @Override
    protected Box createMoov(Movie movie) {
        MovieBox movieBox = new MovieBox();

        movieBox.addBox(createMvhd(movie));

        for (byte[] systemIdBytes : psshBoxes.keySet()) {
            movieBox.addBox(createProtectionSystemSpecificHeaderBox(systemIdBytes, psshBoxes.get(systemIdBytes)));
        }

        for (Track track : movie.getTracks()) {
            movieBox.addBox(createTrak(track, movie));
        }

        movieBox.addBox(createMvex(movie));

        // metadata here
        return movieBox;

    }

    /**
     * Gets all samples starting with <code>startSample</code> (one based -&gt; one is the first) and
     * ending with <code>endSample</code> (exclusive).
     *
     * @param startSample    low endpoint (inclusive) of the sample sequence
     * @param endSample      high endpoint (exclusive) of the sample sequence
     * @param track          source of the samples
     * @param sequenceNumber the fragment index of the requested list of samples
     * @return a <code>List&lt;ByteBuffer&gt;</code> of raw samples
     */
    @Override
    protected List<Sample> getSamples(long startSample, long endSample, Track track, int sequenceNumber) {
        final SoftReference<List<Sample>> listSoftReference = sampleListCache.get(sequenceNumber);
        if (listSoftReference != null && listSoftReference.get() != null) {
            return listSoftReference.get();
        }
        // since startSample and endSample are one-based substract 1 before addressing list elements
        final List<Sample> samples = track.getSamples().subList(l2i(startSample) - 1, l2i(endSample) - 1);

        SecretKey secretKey = keys.get(track);
        if (secretKey == null) {
            System.err.println("Couldn't find key for track " + track + " with handler " + track.getHandler());
            return samples;
        }

        final List<CencSampleAuxiliaryDataFormat> cencEntries = createSenc(startSample, endSample, track, sequenceNumber).getEntries();
        final CommonEncryptionSampleList sampleList = new CommonEncryptionSampleList(secretKey,
                samples,
                cencEntries);

        sampleListCache.put(sequenceNumber, new SoftReference<List<Sample>>(sampleList));
        return sampleList;
    }

    protected SchemeTypeBox createSchm() {
        SchemeTypeBox schemeTypeBox = new SchemeTypeBox();
        schemeTypeBox.setSchemeVersion(0x00010000);
        schemeTypeBox.setSchemeType("cenc");
        return schemeTypeBox;
    }

    protected Box createSchi(Track track) {
        SchemeInformationBox schemeInformationBox = new SchemeInformationBox();
        TrackEncryptionBox trackEncryptionBox = new TrackEncryptionBox();
        trackEncryptionBox.setDefaultIvSize(8);
        trackEncryptionBox.setDefaultAlgorithmId(0x01);
        trackEncryptionBox.setDefault_KID(keyIds.get(track));
        schemeInformationBox.addBox(trackEncryptionBox);
        return schemeInformationBox;
    }

    @Override
    protected Box createStbl(Movie movie, Track track) {
        SampleTableBox stbl = new SampleTableBox();
        SampleDescriptionBox stsd = track.getSampleDescriptionBox();

        SampleEntry sampleEntry = stsd.getSampleEntry();
        // Apple often has extra boxes next to avcc
        // remvoe them to become spec compliant

        if (keys.get(track) != null) {
            OriginalFormatBox originalFormatBox = new OriginalFormatBox();
            originalFormatBox.setDataFormat(sampleEntry.getType());
            ProtectionSchemeInformationBox protectionSchemeInformationBox = new ProtectionSchemeInformationBox();
            protectionSchemeInformationBox.addBox(originalFormatBox);
            protectionSchemeInformationBox.addBox(createSchm());

            if (sampleEntry instanceof AudioSampleEntry) {
                ((AudioSampleEntry) sampleEntry).setType(AudioSampleEntry.TYPE_ENCRYPTED);
                ((AudioSampleEntry) sampleEntry).addBox(protectionSchemeInformationBox);
            }
            if (sampleEntry instanceof VisualSampleEntry && sampleEntry.getType().equals("avc1")) {
                ((VisualSampleEntry) sampleEntry).setCompressorname("AVC Coding");
                ((VisualSampleEntry) sampleEntry).setType(VisualSampleEntry.TYPE_ENCRYPTED);
                ((VisualSampleEntry) sampleEntry).addBox(protectionSchemeInformationBox);
            }

            protectionSchemeInformationBox.addBox(createSchi(track));

            stbl.addBox(stsd);
        } else {
            System.err.println("Couldn't find key for track " + track + " with handler " + track.getHandler());
            stbl.addBox(track.getSampleDescriptionBox());
        }
        stbl.addBox(new TimeToSampleBox());
        stbl.addBox(new SampleToChunkBox());
        stbl.addBox(new SampleSizeBox());
        stbl.addBox(new StaticChunkOffsetBox());
        return stbl;
    }

}
