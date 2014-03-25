/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.helpers;

import com.coremedia.iso.Hex;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.OriginalFormatBox;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.h264.AvcConfigurationBox;
import com.coremedia.iso.boxes.sampleentry.AbstractSampleEntry;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.coremedia.iso.boxes.sampleentry.VisualSampleEntry;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.boxes.AC3SpecificBox;
import com.googlecode.mp4parser.boxes.DTSSpecificBox;
import com.googlecode.mp4parser.boxes.EC3SpecificBox;
import com.googlecode.mp4parser.boxes.mp4.ESDescriptorBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.AudioSpecificConfig;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.DecoderConfigDescriptor;
import com.googlecode.mp4parser.boxes.threegpp26244.SegmentIndexBox;
import com.googlecode.mp4parser.util.Path;
import mpegDASHSchemaMPD2011.AdaptationSetType;
import mpegDASHSchemaMPD2011.DescriptorType;
import mpegDASHSchemaMPD2011.RepresentationType;
import org.apache.commons.lang.math.Fraction;

import java.io.IOException;
import java.util.List;

/**
 * Some conversion from Track representation to Manifest specifics shared by DASH manifests of all kinds.
 */
public class ManifestHelper {

    public static String convertFramerate(double vrate) {
        String frameRate = null;
        if ((vrate > 14) && (vrate < 15)) {
            frameRate = "15000/1001";
        } else if ((vrate == 15)) {
            frameRate = "15000/1000";
        } else if ((vrate > 23) && (vrate < 24)) {
            frameRate = "24000/1001";
        } else if (vrate == 24) {
            frameRate = "24000/1000";
        } else if ((vrate > 24) && ((vrate < 25) || (vrate == 25))) {
            frameRate = "25000/1000";
        } else if ((vrate > 29) && (vrate < 30)) {
            frameRate = "30000/1001";
        } else if (vrate == 30) {
            frameRate = "30000/1000";
        } else if (vrate == 50) {
            frameRate = "50000/1000";
        } else if ((vrate > 59) && (vrate < 60)) {
            frameRate = "60000/1001";
        } else if (vrate == 60) {
            frameRate = "60000/1000";
        } else {
            System.out.println("Framerate " + vrate + " is not supported");
            System.exit(1);
        }
        return frameRate;
    }

    public static String calculateIndexRange(Container isoFile) throws IOException {
        SegmentIndexBox sidx = (SegmentIndexBox) Path.getPath(isoFile, "/sidx");
        long start = 0;
        for (Box box : isoFile.getBoxes()) {
            if (box == sidx) {
                break;
            } else {
                start += box.getSize();
            }
        }
        // long start = sidx.getOffset(); getOffset works for parsed files only
        long end = sidx.getSize() + start;
        return String.format("%s-%s", start, end);
    }


    /**
     * Creates a representation and adjusts the AdaptionSet's attributes maxFrameRate, maxWidth, maxHeight.
     * Also creates AudioChannelConfiguration.
     */
    public static RepresentationType createRepresentation(AdaptationSetType adaptationSet, Track track) {
        RepresentationType representation = adaptationSet.addNewRepresentation();
        if (track.getHandler().equals("vide")) {

            long videoHeight = (long) track.getTrackMetaData().getHeight();
            long videoWidth = (long) track.getTrackMetaData().getWidth();
            double framesPerSecond = (double) (track.getSamples().size() * track.getTrackMetaData().getTimescale()) / track.getDuration();

            adaptationSet.setMaxFrameRate(convertFramerate(
                    Math.max(adaptationSet.isSetMaxFrameRate() ? Fraction.getFraction(adaptationSet.getMaxFrameRate()).doubleValue() : 0,
                            framesPerSecond)
            ));

            adaptationSet.setMaxWidth(Math.max(adaptationSet.isSetMaxWidth() ? adaptationSet.getMaxWidth() : 0,
                    videoHeight));
            adaptationSet.setMaxHeight(Math.max(adaptationSet.isSetMaxHeight() ? adaptationSet.getMaxHeight() : 0,
                    videoWidth));

            adaptationSet.setPar("1:1");
            // too hard to find it out. Ignoring even though it should be set according to DASH-AVC-264-v2.00-hd-mca.pdf

            representation.setMimeType("video/mp4");
            representation.setCodecs(getVideoCodecs(track));
            representation.setWidth(videoWidth);
            representation.setHeight(videoHeight);
            representation.setFrameRate(convertFramerate(framesPerSecond));
            representation.setSar("1:1");
            // too hard to find it out. Ignoring even though it should be set according to DASH-AVC-264-v2.00-hd-mca.pdf
        }

        if (track.getHandler().equals("soun")) {

            AudioQuality audioQ = getAudioQuality(track);


            representation.setMimeType("audio/mp4");
            representation.setCodecs(audioQ.fourCC);
            representation.setAudioSamplingRate(String.valueOf(audioQ.samplingRate));

            DescriptorType audio_channel_conf = representation.addNewAudioChannelConfiguration();
            audio_channel_conf.setSchemeIdUri(audioQ.audioChannelScheme);
            audio_channel_conf.setValue(audioQ.audioChannelValue);

        }
        return representation;
    }

    private static AudioQuality getAudioQuality(Track track) {
        String type = track.getSampleDescriptionBox().getSampleEntry().getType();
        if (type.equals("enca")) {
            OriginalFormatBox frma = track.getSampleDescriptionBox().getSampleEntry().getBoxes(OriginalFormatBox.class, true).get(0);
            type = frma.getDataFormat();
        }
        if (type.equals("ac-3")) {
            return getAc3AudioQuality(track);
        } else if (type.equals("ec-3")) {
            return getEc3AudioQuality(track);
        } else if (type.startsWith("dts")) {
            return getDtsAudioQuality(track);
        } else if (type.equals("mp4a")) {
            return getAacAudioQuality(track);
        } else {
            throw new RuntimeException("I don't know how to get AudioQuality for " + type);
        }
    }

    private static AudioQuality getDtsAudioQuality(Track track) {
        AudioQuality l = new AudioQuality();

        final AudioSampleEntry ase = getAudioSampleEntry(track, l);
        l.fourCC = getFormat(ase);

        final DTSSpecificBox dtsSpecificBox = ase.getBoxes(DTSSpecificBox.class).get(0);
        if (dtsSpecificBox == null) {
            throw new RuntimeException("DTS track misses DTSSpecificBox!");
        }
        l.bitrate = dtsSpecificBox.getAvgBitRate();
        l.samplingRate = dtsSpecificBox.getDTSSamplingFrequency();
        l.bitPerSample = dtsSpecificBox.getPcmSampleDepth();
        l.audioChannelValue = Integer.toString(getNumChannelsAndMask(dtsSpecificBox)[0]);
        l.audioChannelScheme = "urn:dts:dash:audio_channel_configuration:2012";

        return l;
    }

    //todo: reuse smoothstreaming code, it's copied here
    private static int[] getNumChannelsAndMask(DTSSpecificBox dtsSpecificBox) {
        final int channelLayout = dtsSpecificBox.getChannelLayout();
        int numChannels = 0;
        int dwChannelMask = 0;
        if ((channelLayout & 0x0001) == 0x0001) {
            //0001h Center in front of listener 1
            numChannels += 1;
            dwChannelMask |= 0x00000004; //SPEAKER_FRONT_CENTER
        }
        if ((channelLayout & 0x0002) == 0x0002) {
            //0002h Left/Right in front 2
            numChannels += 2;
            dwChannelMask |= 0x00000001; //SPEAKER_FRONT_LEFT
            dwChannelMask |= 0x00000002; //SPEAKER_FRONT_RIGHT
        }
        if ((channelLayout & 0x0004) == 0x0004) {
            //0004h Left/Right surround on side in rear 2
            numChannels += 2;
            //* if Lss, Rss exist, then this position is equivalent to Lsr, Rsr respectively
            dwChannelMask |= 0x00000010; //SPEAKER_BACK_LEFT
            dwChannelMask |= 0x00000020; //SPEAKER_BACK_RIGHT
        }
        if ((channelLayout & 0x0008) == 0x0008) {
            //0008h Low frequency effects subwoofer 1
            numChannels += 1;
            dwChannelMask |= 0x00000008; //SPEAKER_LOW_FREQUENCY
        }
        if ((channelLayout & 0x0010) == 0x0010) {
            //0010h Center surround in rear 1
            numChannels += 1;
            dwChannelMask |= 0x00000100; //SPEAKER_BACK_CENTER
        }
        if ((channelLayout & 0x0020) == 0x0020) {
            //0020h Left/Right height in front 2
            numChannels += 2;
            dwChannelMask |= 0x00001000; //SPEAKER_TOP_FRONT_LEFT
            dwChannelMask |= 0x00004000; //SPEAKER_TOP_FRONT_RIGHT
        }
        if ((channelLayout & 0x0040) == 0x0040) {
            //0040h Left/Right surround in rear 2
            numChannels += 2;
            dwChannelMask |= 0x00000010; //SPEAKER_BACK_LEFT
            dwChannelMask |= 0x00000020; //SPEAKER_BACK_RIGHT
        }
        if ((channelLayout & 0x0080) == 0x0080) {
            //0080h Center Height in front 1
            numChannels += 1;
            dwChannelMask |= 0x00002000; //SPEAKER_TOP_FRONT_CENTER
        }
        if ((channelLayout & 0x0100) == 0x0100) {
            //0100h Over the listener’s head 1
            numChannels += 1;
            dwChannelMask |= 0x00000800; //SPEAKER_TOP_CENTER
        }
        if ((channelLayout & 0x0200) == 0x0200) {
            //0200h Between left/right and center in front 2
            numChannels += 2;
            dwChannelMask |= 0x00000040; //SPEAKER_FRONT_LEFT_OF_CENTER
            dwChannelMask |= 0x00000080; //SPEAKER_FRONT_RIGHT_OF_CENTER
        }
        if ((channelLayout & 0x0400) == 0x0400) {
            //0400h Left/Right on side in front 2
            numChannels += 2;
            dwChannelMask |= 0x00000200; //SPEAKER_SIDE_LEFT
            dwChannelMask |= 0x00000400; //SPEAKER_SIDE_RIGHT
        }
        if ((channelLayout & 0x0800) == 0x0800) {
            //0800h Left/Right surround on side 2
            numChannels += 2;
            //* if Lss, Rss exist, then this position is equivalent to Lsr, Rsr respectively
            dwChannelMask |= 0x00000010; //SPEAKER_BACK_LEFT
            dwChannelMask |= 0x00000020; //SPEAKER_BACK_RIGHT
        }
        if ((channelLayout & 0x1000) == 0x1000) {
            //1000h Second low frequency effects subwoofer 1
            numChannels += 1;
            dwChannelMask |= 0x00000008; //SPEAKER_LOW_FREQUENCY
        }
        if ((channelLayout & 0x2000) == 0x2000) {
            //2000h Left/Right height on side 2
            numChannels += 2;
            dwChannelMask |= 0x00000010; //SPEAKER_BACK_LEFT
            dwChannelMask |= 0x00000020; //SPEAKER_BACK_RIGHT
        }
        if ((channelLayout & 0x4000) == 0x4000) {
            //4000h Center height in rear 1
            numChannels += 1;
            dwChannelMask |= 0x00010000; //SPEAKER_TOP_BACK_CENTER
        }
        if ((channelLayout & 0x8000) == 0x8000) {
            //8000h Left/Right height in rear 2
            numChannels += 2;
            dwChannelMask |= 0x00008000; //SPEAKER_TOP_BACK_LEFT
            dwChannelMask |= 0x00020000; //SPEAKER_TOP_BACK_RIGHT
        }
        if ((channelLayout & 0x10000) == 0x10000) {
            //10000h Center below in front
            numChannels += 1;
        }
        if ((channelLayout & 0x20000) == 0x20000) {
            //20000h Left/Right below in front
            numChannels += 2;
        }
        return new int[]{numChannels, dwChannelMask};
    }

    private static AudioQuality getAacAudioQuality(Track track) {
        AudioQuality l = new AudioQuality();
        AudioSampleEntry ase = (AudioSampleEntry) track.getSampleDescriptionBox().getSampleEntry();
        l.samplingRate = ase.getSampleRate();
        final ESDescriptorBox esDescriptorBox = ase.getBoxes(ESDescriptorBox.class).get(0);
        final DecoderConfigDescriptor decoderConfigDescriptor = esDescriptorBox.getEsDescriptor().getDecoderConfigDescriptor();
        final AudioSpecificConfig audioSpecificConfig = decoderConfigDescriptor.getAudioSpecificInfo();
        if (audioSpecificConfig.getSbrPresentFlag() == 1) {
            l.fourCC = "mp4a.40.5";
        } else if (audioSpecificConfig.getPsPresentFlag() == 1) {
            l.fourCC = "mp4a.40.29";
        } else {
            l.fourCC = "mp4a.40.2";
        }
        l.bitrate = decoderConfigDescriptor.getAvgBitRate();
        l.samplingRate = ase.getSampleRate();
        l.audioChannelScheme = "urn:mpeg:dash:23003:3:audio_channel_configuration:2011";
        l.audioChannelValue = String.valueOf(audioSpecificConfig.getChannelConfiguration());
        l.bitPerSample = ase.getSampleSize();
        return l;
    }

    private static AudioQuality getAc3AudioQuality(Track track) {
        AudioQuality l = new AudioQuality();
        l.fourCC = "AC-3";

        AudioSampleEntry ase = getAudioSampleEntry(track, l);
        final AC3SpecificBox ac3SpecificBox = ase.getBoxes(AC3SpecificBox.class).get(0);
        if (ac3SpecificBox == null) {
            throw new RuntimeException("AC-3 track misses AC3SpecificBox!");
        }

        int bitRateCode = ac3SpecificBox.getBitRateCode();
        /*
        Table A.2: Bit_rate_code table
bit_rate_code Exact bit rate (kbit/s)       bit_rate_code Bit rate upper limit (kbit/s)
"000000" (0) 32                             "100000" (32) 32
"000001" (1) 40                             "100001" (33) 40
"000010" (2) 48                             "100010" (34) 48
"000011" (3) 56                             "100011" (35) 56
"000100" (4) 64                             "100100" (36) 64
"000101" (5) 80                             "100101" (37) 80
"000110" (6) 96                             "100110" (38) 96
"000111" (7) 112                            "100111" (39) 112
"001000" (8) 128                            "101000" (40) 128
"001001" (9) 160                            "101001" (41) 160
"001010" (10) 192                           "101010" (42) 192
"001011" (11) 224                           "101011" (43) 224
"001100" (12) 256                           "101100" (44) 256
"001101" (13) 320                           "101101" (45) 320
"001110" (14) 384                           "101110" (46) 384
"001111" (15) 448                           "101111" (47) 448
"010000" (16) 512                           "110000" (48) 512
"010001" (17) 576                           "110001" (49) 576
"010010" (18) 640                           "110010" (50) 640
         */
        //remove upper limit indicator
        bitRateCode = bitRateCode << 1;
        switch (bitRateCode) {
            case 0:
                l.bitrate = 32;
                break;
            case 1:
                l.bitrate = 40;
                break;
            case 2:
                l.bitrate = 48;
                break;
            case 3:
                l.bitrate = 56;
                break;
            case 4:
                l.bitrate = 64;
                break;
            case 5:
                l.bitrate = 80;
                break;
            case 6:
                l.bitrate = 96;
                break;
            case 7:
                l.bitrate = 112;
                break;
            case 8:
                l.bitrate = 128;
                break;
            case 9:
                l.bitrate = 160;
                break;
            case 10:
                l.bitrate = 192;
                break;
            case 11:
                l.bitrate = 224;
                break;
            case 12:
                l.bitrate = 256;
                break;
            case 13:
                l.bitrate = 320;
                break;
            case 14:
                l.bitrate = 384;
                break;
            case 15:
                l.bitrate = 448;
                break;
            case 16:
                l.bitrate = 512;
                break;
            case 17:
                l.bitrate = 576;
                break;
            case 18:
                l.bitrate = 640;
                break;
        }
        l.bitrate *= 1024; //bit per sec

        int audioChannelValue = getDolbyAudioChannelValue(ac3SpecificBox.getAcmod(), ac3SpecificBox.getLfeon());
        l.audioChannelValue = Hex.encodeHex(new byte[]{(byte) ((audioChannelValue >> 8) & 0xFF), (byte) (audioChannelValue & 0xFF)});
        l.audioChannelScheme = "urn:dolby:dash:audio_channel_configuration:2011";

        return l;
    }

    private static AudioQuality getEc3AudioQuality(Track track) {
        AudioQuality l = new AudioQuality();
        l.fourCC = "EC-3";


        AudioSampleEntry ase = getAudioSampleEntry(track, l);
        final EC3SpecificBox ec3SpecificBox = ase.getBoxes(EC3SpecificBox.class).get(0);
        if (ec3SpecificBox == null) {
            throw new RuntimeException("EC-3 track misses EC3SpecificBox!");
        }
        l.bitrate = ec3SpecificBox.getDataRate() * 1024;

        final List<EC3SpecificBox.Entry> ec3SpecificBoxEntries = ec3SpecificBox.getEntries();
        int audioChannelValue = 0;
        for (EC3SpecificBox.Entry ec3SpecificBoxEntry : ec3SpecificBoxEntries) {
            audioChannelValue |= getDolbyAudioChannelValue(ec3SpecificBoxEntry.acmod, ec3SpecificBoxEntry.lfeon);
        }
        l.audioChannelValue = Hex.encodeHex(new byte[]{(byte) ((audioChannelValue >> 8) & 0xFF), (byte) (audioChannelValue & 0xFF)});
        l.audioChannelScheme = "urn:dolby:dash:audio_channel_configuration:2011";
        return l;
    }

    private static AudioSampleEntry getAudioSampleEntry(Track track, AudioQuality l) {
        final AudioSampleEntry ase = (AudioSampleEntry) track.getSampleDescriptionBox().getSampleEntry();
        l.samplingRate = ase.getSampleRate();

        return ase;
    }

    private static int getDolbyAudioChannelValue(int acmod, int lfeon) {
        int audioChannelValue;
        switch (acmod) {
            case 0:
                audioChannelValue = 0xA000;
                break;
            case 1:
                audioChannelValue = 0x4000;
                break;
            case 2:
                audioChannelValue = 0xA000;
                break;
            case 3:
                audioChannelValue = 0xE000;
                break;
            case 4:
                audioChannelValue = 0xA100;
                break;
            case 5:
                audioChannelValue = 0xE100;
                break;
            case 6:
                audioChannelValue = 0xB800;
                break;
            case 7:
                audioChannelValue = 0xF800;
                break;
            default:
                throw new RuntimeException("Unexpected acmod " + acmod);
        }
        if (lfeon == 1) {
            audioChannelValue += 1;
        }
        return audioChannelValue;
    }

    private static String getVideoCodecs(Track track) {

        SampleDescriptionBox stsd = track.getSampleDescriptionBox();
        VisualSampleEntry vse = (VisualSampleEntry) stsd.getSampleEntry();
        String type = vse.getType();
        if (type.equals("encv")) {
            OriginalFormatBox frma = vse.getBoxes(OriginalFormatBox.class, true).get(0);
            type = frma.getDataFormat();
        }

        if ("avc1".equals(type)) {
            AvcConfigurationBox avcConfigurationBox = vse.getBoxes(AvcConfigurationBox.class).get(0);
            List<byte[]> spsbytes = avcConfigurationBox.getSequenceParameterSets();
            byte[] CodecInit = new byte[3];
            CodecInit[0] = spsbytes.get(0)[1];
            CodecInit[1] = spsbytes.get(0)[2];
            CodecInit[2] = spsbytes.get(0)[3];
            return (type + "." + Hex.encodeHex(CodecInit)).toLowerCase();
        } else {
            throw new InternalError("I don't know how to handle video of type " + vse.getType());
        }

    }

    protected static String getFormat(AbstractSampleEntry se) {
        String type = se.getType();
        if (type.equals("encv") || type.equals("enca") || type.equals("encv")) {
            OriginalFormatBox frma = se.getBoxes(OriginalFormatBox.class, true).get(0);
            type = frma.getDataFormat();
        }
        return type;
    }

    private static class AudioQuality {
        public String fourCC;
        public long bitrate;
        public long samplingRate;
        public String audioChannelScheme;
        public String audioChannelValue;

        public int bitPerSample;
    }
}
