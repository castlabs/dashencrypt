package com.castlabs.dash.dashfragmenter.encrypt2;

import com.castlabs.dash.dashfragmenter.Main;
import com.castlabs.dash.dashfragmenter.representation.RepresentationBuilderImpl;
import com.castlabs.dash.helpers.DashHelper2;
import mpeg.dash.schema.mpd._2011.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.mp4parser.Box;
import org.mp4parser.Container;
import org.mp4parser.boxes.iso14496.part12.MovieHeaderBox;
import org.mp4parser.boxes.iso14496.part12.OriginalFormatBox;
import org.mp4parser.boxes.iso14496.part15.AvcConfigurationBox;
import org.mp4parser.boxes.iso14496.part15.HevcConfigurationBox;
import org.mp4parser.boxes.iso14496.part15.HevcDecoderConfigurationRecord;
import org.mp4parser.boxes.iso23001.part7.ProtectionSystemSpecificHeaderBox;
import org.mp4parser.boxes.iso23001.part7.TrackEncryptionBox;
import org.mp4parser.boxes.sampleentry.AudioSampleEntry;
import org.mp4parser.boxes.sampleentry.SampleEntry;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.tracks.CleanInputStream;
import org.mp4parser.muxer.tracks.h264.parsing.model.SeqParameterSet;
import org.mp4parser.muxer.tracks.h265.SequenceParameterSetRbsp;
import org.mp4parser.tools.Path;
import org.mp4parser.tools.UUIDConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.castlabs.dash.helpers.BoxHelper.boxToBytes;
import static com.castlabs.dash.helpers.ManifestHelper.convertFramerate;

public class ManifestCreation {
    private static Logger LOG = java.util.logging.Logger.getLogger(ManifestCreation.class.getName());
    static DatatypeFactory datatypeFactory = null;

    static {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }


    public static void addTextTrack(PeriodType periodType, List<File> inFiles, File outputDirectory, Map<String, String> outOptions) throws IOException {
        String role = outOptions.getOrDefault("role", "subtitle");
        String lang = outOptions.get("lang");


        AdaptationSetType adaptationSet = new AdaptationSetType();
        periodType.getAdaptationSet().add(adaptationSet);

        adaptationSet.setLang(lang);

        RepresentationType representation = new RepresentationType();
        adaptationSet.getRepresentation().add(representation);

        DescriptorType textTrackRole = new DescriptorType();
        adaptationSet.getRole().add(textTrackRole);

        textTrackRole.setSchemeIdUri("urn:mpeg:dash:role:2011");
        textTrackRole.setValue(role);
        representation.setId(role + "-" + lang);
        representation.setBandwidth(0); // pointless - just invent a small number
        if (inFiles.get(0).getName().endsWith(".dfxp")) {
            representation.setMimeType("application/ttaf+xml");
        } else if (inFiles.get(0).getName().endsWith(".xml") || inFiles.get(0).getName().endsWith(".ttml")) {
            representation.setMimeType("application/ttml+xml");
        } else if (inFiles.get(0).getName().endsWith(".vtt")) {
            representation.setMimeType("text/vtt");

        } else {
            throw new RuntimeException("Not sure what kind of textTrack " + inFiles.get(0).getName() + " is.");
        }
        for (File inFile : inFiles) {
            FileUtils.copyFile(inFile, new File(outputDirectory, inFile.getName()));
        }
        BaseURLType baseURL = new BaseURLType();
        representation.getBaseURL().add(baseURL);
        baseURL.setValue(inFiles.get(0).getName());
    }

    public static void addContentProtection(RepresentationType representation, Track theTrack, Map<UUID, List<ProtectionSystemSpecificHeaderBox>> psshMap) {
        List<String> keyIds = new ArrayList<>();
        List<ProtectionSystemSpecificHeaderBox> psshs = new ArrayList<>();
        for (SampleEntry sampleEntry : theTrack.getSampleEntries()) {
            TrackEncryptionBox tenc = Path.getPath((Container) sampleEntry, "sinf[0]/schi[0]/tenc[0]");
            if (tenc != null) {
                keyIds.add(tenc.getDefault_KID().toString());
                psshs.addAll(psshMap.getOrDefault(tenc.getDefault_KID(), Collections.emptyList()));

            }
        }

        if (!keyIds.isEmpty()) {
            DescriptorType contentProtection = new DescriptorType();
            representation.getContentProtection().add(contentProtection);
            contentProtection.getOtherAttributes().put(QName.valueOf("cenc:default_KID"), String.join(",", keyIds));
            contentProtection.setSchemeIdUri("urn:mpeg:dash:mp4protection:2011");
            contentProtection.setValue("cenc");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = null;
            try {
                builder = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }

            Document d = builder.newDocument();

            for (ProtectionSystemSpecificHeaderBox pssh : psshs) {

                DescriptorType dt = new DescriptorType();
                representation.getContentProtection().add(dt);
                byte[] psshContent = pssh.getContent();
                dt.setSchemeIdUri("urn:uuid:" + UUIDConverter.convert(pssh.getSystemId()).toString());
                if (Arrays.equals(ProtectionSystemSpecificHeaderBox.PLAYREADY_SYSTEM_ID, pssh.getSystemId())) {
                    dt.setValue("MSPR 2.0");


                    Element pro = d.createElementNS("urn:microsoft:playready", "pro");
                    Element prPssh = d.createElementNS("urn:mpeg:cenc:2013", "pssh");

                    pro.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(psshContent)));
                    prPssh.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(boxToBytes(pssh))));

                    dt.getAny().add(pro);
                    dt.getAny().add(prPssh);
                }
                if (Arrays.equals(ProtectionSystemSpecificHeaderBox.WIDEVINE, pssh.getSystemId())) {
                    // Widevvine
                    Element wvPssh = d.createElementNS("urn:mpeg:cenc:2013", "pssh");
                    wvPssh.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(boxToBytes(pssh))));
                    dt.getAny().add(wvPssh);
                }
            }
        }
    }

    private static Dimension getImageDimension(File imgFile) throws IOException {
        int pos = imgFile.getName().lastIndexOf(".");
        if (pos == -1)
            throw new IOException("No extension for file: " + imgFile.getAbsolutePath());
        String suffix = imgFile.getName().substring(pos + 1);
        Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
        while (iter.hasNext()) {
            ImageReader reader = iter.next();
            try {
                ImageInputStream stream = new FileImageInputStream(imgFile);
                reader.setInput(stream);
                int width = reader.getWidth(reader.getMinIndex());
                int height = reader.getHeight(reader.getMinIndex());
                return new Dimension(width, height);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Error reading: " + imgFile.getAbsolutePath(), e);
            } finally {
                reader.dispose();
            }
        }

        throw new IOException("Not a known image file: " + imgFile.getAbsolutePath());
    }

    public static void addSegmentBase(RepresentationBuilderImpl rb, RepresentationType representationType) {
        SegmentBaseType segBaseType = new SegmentBaseType();
        representationType.setSegmentBase(segBaseType);
        segBaseType.setTimescale(((MovieHeaderBox) Path.getPath(rb.getInitSegment(), "moov[0]/mvhd[0]")).getTimescale());
        segBaseType.setIndexRangeExact(true);
        long initSize = 0;
        for (Box b : rb.getInitSegment().getBoxes()) {
            initSize += b.getSize();
        }
        long indexSize = 0;
        for (Box b : rb.getIndexSegment().getBoxes()) {
            indexSize += b.getSize();
        }
        segBaseType.setIndexRange(String.format("%s-%s", initSize, initSize + indexSize - 1));
        URLType initialization = new URLType();
        segBaseType.setInitElement(initialization);
        initialization.setRange(String.format("0-%s", initSize - 1));
    }

    public static String findRepresentationId(Track track, MPDtype mpd) {
        HashSet<String> ids = new HashSet<>();

        for (PeriodType periodType : mpd.getPeriod()) {
            for (AdaptationSetType adaptationSetType : periodType.getAdaptationSet()) {
                for (RepresentationType representationType : adaptationSetType.getRepresentation()) {
                    ids.add(representationType.getId());
                }
            }
        }
        if (ids.contains(FilenameUtils.getBaseName(track.getName()))) {
            return FilenameUtils.getBaseName(track.getName()) + "-" + track.getTrackMetaData().getTrackId();
        } else {
            return FilenameUtils.getBaseName(track.getName());
        }

    }

    public static void updateDuration(PeriodType periodType, double duration) {
        Duration d = datatypeFactory.newDuration(true, 0, 0, 0, (int) (duration / 3600),
                (int) ((duration % 3600) / 60),
                (int) (duration % 60));

        if (periodType.getDuration() == null || periodType.getDuration().compare(d) > 0) {
            periodType.setDuration(d);
        }
    }

    private static String greatestCommonPrefix(String a, String b) {
        int minLength = Math.min(a.length(), b.length());
        for (int i = 0; i < minLength; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.substring(0, i);
            }
        }
        return a.substring(0, minLength);
    }

    public static void addThumbnailTrack(PeriodType periodType, List<File> files, File outputDirectory, Map<String, String> outOptions) throws IOException {
        LOG.info("Using thumbnail files in following order: " + files.stream().map(File::getName).collect(Collectors.joining(", ")));
        String prefix = files.get(0).getAbsolutePath();

        Dimension dRef = null;
        for (File file : files) {
            prefix = greatestCommonPrefix(prefix, file.getAbsolutePath());
            Dimension d = getImageDimension(file);
            if (dRef == null) {
                dRef = d;
            } else {
                if (!d.equals(dRef)) {
                    LOG.warning("Thumbnail tile dimension is not the same for all thumbnail files");
                }
            }
        }
        prefix = prefix.split(Pattern.quote(File.separator))[prefix.split(Pattern.quote(File.separator)).length - 1];
        assert dRef != null;
        File targetDir = new File(outputDirectory, prefix);
        long thumbsize = 0;
        for (int i = 1; i <= files.size(); i++) {
            FileUtils.copyFile(files.get(i - 1), new File(targetDir, prefix + i + "." + FilenameUtils.getExtension(files.get(0).getName())));
            thumbsize += files.get(i - 1).length();
        }
        int htiles = Integer.parseInt(outOptions.get("htiles"));
        int vtiles = Integer.parseInt(outOptions.get("vtiles"));
        double thduration = Integer.parseInt(outOptions.get("thduration"));


        AdaptationSetType adaptationSet = new AdaptationSetType();
        periodType.getAdaptationSet().add(adaptationSet);
        adaptationSet.setMimeType("image/jpeg");
        adaptationSet.setContentType("image");
        SegmentTemplateType segmentTemplateType = new SegmentTemplateType();
        adaptationSet.setSegmentTemplate(segmentTemplateType);
        segmentTemplateType.setStartNumber(1L);
        segmentTemplateType.setMedia("$RepresentationID$/" + prefix + "$Number$." + FilenameUtils.getExtension(files.get(0).getName()));
        segmentTemplateType.setDuration((long) (vtiles * htiles * thduration));
        RepresentationType representation = new RepresentationType();
        adaptationSet.getRepresentation().add(representation);
        representation.setId(prefix);
        representation.setBandwidth((long) ((thumbsize * 8) / (files.size() * vtiles * htiles * thduration)));
        representation.setWidth((long) (dRef.width / htiles));
        representation.setHeight((long) (dRef.height / vtiles));

        DescriptorType essentialPropery = new DescriptorType();
        representation.getEssentialProperty().add(essentialPropery);
        essentialPropery.setSchemeIdUri("http://dashif.org/guidelines/thumbnail_tile");
        essentialPropery.setValue("" + htiles + "x" + vtiles);
    }

    public static PeriodType getPeriod(MPDtype mpd, String periodId) {
        return mpd.getPeriod().stream()
                .filter(x -> x.getId().equals(periodId))
                .findFirst()
                .orElseGet(() -> {
                    PeriodType p = new PeriodType();
                    mpd.getPeriod().add(p);
                    p.setId(periodId);
                    if (mpd.getPeriod().size() == 1) {
                        p.setStart(datatypeFactory.newDuration(true, 0, 0, 0, 0, 0, 0));
                    }
                    return p;
                });
    }

    public static RepresentationType addAudioRepresentation(AdaptationSetType adaptationSetType, Track t, String representationId, Map<String, String> outOptions) {
        RepresentationType representationType = new RepresentationType();
        adaptationSetType.getRepresentation().add(representationType);
        representationType.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");
        adaptationSetType.setMimeType("audio/mp4");
        if (outOptions.containsKey("role")) {
            DescriptorType role =  new DescriptorType();
            adaptationSetType.getRole().add(role);
            role.setSchemeIdUri("urn:mpeg:dash:role:2011");
            role.setValue(outOptions.get("role"));
        }
        LinkedHashSet<String> codecs = new LinkedHashSet<>();
        AudioSampleEntry ase = null;
        for (SampleEntry sampleEntry : t.getSampleEntries()) {
            if (sampleEntry instanceof AudioSampleEntry) {
                ase = (AudioSampleEntry) sampleEntry;
            }
            codecs.add(DashHelper2.getRfc6381Codec(sampleEntry));
        }
        assert ase != null;
        representationType.setCodecs(String.join(",", codecs));
        representationType.setAudioSamplingRate("" + DashHelper2.getAudioSamplingRate(ase));
        representationType.setBandwidth(RepresentationBuilderImpl.getBandwidth(t));

        representationType.setId(representationId);
        BaseURLType baseURLType = new BaseURLType();
        representationType.getBaseURL().add(baseURLType);
        baseURLType.setValue(representationType.getId() + ".mp4");

        DashHelper2.ChannelConfiguration cc = DashHelper2.getChannelConfiguration(ase);
        if (cc != null) {
            DescriptorType audioChannelConf = new DescriptorType();
            representationType.getAudioChannelConfiguration().add(audioChannelConf);
            audioChannelConf.setSchemeIdUri(cc.schemeIdUri);
            audioChannelConf.setValue(cc.value);
        }
        return representationType;
    }

    public static RepresentationType addVideoRepresentation(AdaptationSetType adaptationSetType, String representationId, Track t) {
        RepresentationType representationType = new RepresentationType();
        adaptationSetType.getRepresentation().add(representationType);
        representationType.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");
        long videoHeight = (long) t.getTrackMetaData().getHeight();
        long videoWidth = (long) t.getTrackMetaData().getWidth();
        double framesPerSecond = (double) (t.getSamples().size() * t.getTrackMetaData().getTimescale()) / t.getDuration();

        LinkedHashSet<String> codecs = new LinkedHashSet<>();
        for (SampleEntry sampleEntry : t.getSampleEntries()) {
            codecs.add(DashHelper2.getRfc6381Codec(sampleEntry));
        }
        representationType.setCodecs(StringUtils.join(codecs.toArray(), ","));
        representationType.setWidth(videoWidth);
        representationType.setHeight(videoHeight);
        representationType.setFrameRate(convertFramerate(framesPerSecond));
        for (SampleEntry se : t.getSampleEntries()) {
            AvcConfigurationBox avcC = Path.getPath((Box) se, "avcC");
            if (avcC != null) {
                SeqParameterSet sps = null;
                try {
                    sps = SeqParameterSet.read(avcC.getSequenceParameterSets().get(0).array());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            HevcConfigurationBox hvcC = Path.getPath((Box) se, "hvcC");
            if (hvcC != null) {
                for (HevcDecoderConfigurationRecord.Array array : hvcC.getArrays()) {
                    if (array.nal_unit_type == 33) {
                        for (byte[] nalUnit : array.nalUnits) {
                            InputStream bais = new CleanInputStream(new ByteArrayInputStream(nalUnit));
                            try {
                                bais.read(); // nal unit header
                                bais.read(); // nal unit header
                                SequenceParameterSetRbsp sps = new SequenceParameterSetRbsp(bais);

                                if (sps.vuiParameters.colour_description_present_flag) {
                                    DescriptorType cp = new DescriptorType();

                                    representationType.getSupplementalProperty().add(cp);
                                    cp.setSchemeIdUri("urn:mpeg:mpegB:cicp:ColourPrimaries");
                                    cp.setValue("" + sps.vuiParameters.colour_primaries);
/*                                    try (final InputStream stream =
                                                 ManifestCreation.class.getResourceAsStream("/com/castlabs/dash/dashfragmenter/encrypt2/color-primaries.properties")) {
                                        Properties properties = new Properties();
                                        properties.load(stream);
                                        if (properties.containsKey("" + sps.vuiParameters.colour_primaries)) {
                                            cp.newCursor().insertComment(properties.getProperty("" + sps.vuiParameters.colour_primaries));
                                        }
                                    }*/

                                    DescriptorType tc = new DescriptorType();
                                    representationType.getSupplementalProperty().add(tc);
                                    tc.setSchemeIdUri("urn:mpeg:mpegB:cicp:TransferCharacteristics");
                                    tc.setValue("" + sps.vuiParameters.transfer_characteristics);
/*                                    try (final InputStream stream =
                                                 ManifestCreation.class.getResourceAsStream("/com/castlabs/dash/dashfragmenter/encrypt2/transfer-characteristics.properties")) {
                                        Properties properties = new Properties();
                                        properties.load(stream);
                                        if (properties.containsKey("" + sps.vuiParameters.transfer_characteristics)) {
                                            tc.newCursor().insertComment(properties.getProperty("" + sps.vuiParameters.transfer_characteristics));
                                        }
                                    }*/

                                    DescriptorType mc = new DescriptorType();
                                    representationType.getSupplementalProperty().add(mc);
                                    mc.setSchemeIdUri("urn:mpeg:mpegB:cicp:MatrixCoefficients");
                                    mc.setValue("" + sps.vuiParameters.matrix_coeffs);
/*                                    try (final InputStream stream =
                                                 ManifestCreation.class.getResourceAsStream("/com/castlabs/dash/dashfragmenter/encrypt2/matrix-coefficient.properties")) {
                                        Properties properties = new Properties();
                                        properties.load(stream);
                                        if (properties.containsKey("" + sps.vuiParameters.matrix_coeffs)) {
                                            mc.newCursor().insertComment(properties.getProperty("" + sps.vuiParameters.matrix_coeffs));
                                        }
                                    }*/
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

            }
        }
        representationType.setSar("1:1");
        representationType.setBandwidth(RepresentationBuilderImpl.getBandwidth(t));
        representationType.setId(representationId);
        BaseURLType baseURLType = new BaseURLType();
        representationType.getBaseURL().add(baseURLType);
        baseURLType.setValue(representationType.getId() + ".mp4");
        return representationType;
    }
}
