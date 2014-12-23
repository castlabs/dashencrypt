package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.Command;
import com.castlabs.dash.dashfragmenter.ExitCodeException;
import com.coremedia.iso.Hex;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeReaderVariable;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.mdat.SampleList;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Mp4TrackImpl;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.WrappingTrack;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.builder.Mp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.SampleToGroupBox;
import com.googlecode.mp4parser.util.Path;
import com.mp4parser.iso14496.part15.AvcConfigurationBox;
import javafx.embed.swing.SwingNode;
import javafx.scene.control.TitledPane;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.xmlbeans.impl.common.IOUtil;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * Created by sannies on 08.12.2014.
 */
public class SortTracks implements Command {

    @Option(required = true, handler = FileOptionHandler.class, name = "--outputdir", aliases = "-o")
    File outputDir;


    @Argument(required = true, multiValued = true, handler = FileOptionHandler.class, usage = "Bitstream input files", metaVar = "vid1.avc1, aud1.dtshd ...")
    protected List<File> inputFiles;


    @Option(required = false, name = "-ffprobe-executable")
    String ffprobe;

    public int run() throws IOException, ExitCodeException {
        Set<String> audiosDone = new HashSet<String>();
        outputDir.mkdirs();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        // collect videos
        Mp4Builder builder = new DefaultMp4Builder();
        for (File f : inputFiles) {
            if (f.getName().endsWith(".mp4")) {
                IsoFile isoFile = new IsoFile(f.getAbsolutePath());

                if (Path.getPath(isoFile, "/moov[0]/trak[0]/mdia[0]/minf[0]/stbl[0]/stsd[0]/avc1[0]") != null) {
                    AvcConfigurationBox avcC = Path.getPath(isoFile, "/moov[0]/trak[0]/mdia[0]/minf[0]/stbl[0]/stsd[0]/avc1[0]/avcC[0]");
                    Mp4TrackImpl mp4Track = new Mp4TrackImpl(f.getName(), (TrackBox) Path.getPath(isoFile, "/moov[0]/trak[0]"));
                    int fps = (int) ((double) mp4Track.getSamples().size() / ((double) mp4Track.getDuration() / (double) mp4Track.getTrackMetaData().getTimescale()));
                    if (fps > 15) {

                        long[] syncSamples = new long[0];

                        for (int i = 1; i <= mp4Track.getSamples().size(); i++) {
                            boolean idr = false;
                            ByteBuffer s = mp4Track.getSamples().get(i - 1).asByteBuffer();
                            while (s.remaining() > 0) {
                                int length = l2i(IsoTypeReaderVariable.read(s, avcC.getLengthSizeMinusOne() + 1));
                                ByteBuffer nal = ((ByteBuffer) s.slice().limit(length));
                                int type = nal.get();
                                int nal_ref_idc = (type >> 5) & 3;
                                int nal_unit_type = type & 0x1f;
                                idr |= nal_unit_type == 5;
                                s.position(s.position() + length);
                            }
                            if (idr) {

                                syncSamples = Arrays.copyOf(syncSamples, syncSamples.length + 1);
                                syncSamples[syncSamples.length - 1] = i;
                            }

                        }
                        Movie m = new Movie();
                        m.addTrack(new WrappingTrack(mp4Track));
                        Container correctedFile = builder.build(m);
                        File outFile = new File(outputDir, f.getName());
                        correctedFile.writeContainer(
                                new FileOutputStream(outFile).getChannel());
                        System.out.println("Added " + f.getName() + " with corrected sync samples.");

                        analyze(outFile, mp4Track.getDuration() / mp4Track.getTrackMetaData().getTimescale());
                    }
                }
                if (audiosDone.isEmpty() && Path.getPath(isoFile, "/moov[0]/trak[0]/mdia[0]/minf[0]/stbl[0]/stsd[0]/mp4a[0]") != null) {
                    Mp4TrackImpl mp4Track = new Mp4TrackImpl(f.getName(), (TrackBox) Path.getPath(isoFile, "/moov[0]/trak[0]"));

                    for (Sample sample : mp4Track.getSamples()) {
                        md.update(sample.asByteBuffer());
                    }
                    String audioFingerPrint = Hex.encodeHex(md.digest());
                    if (!audiosDone.contains(audioFingerPrint)) {
                        if (!audiosDone.isEmpty()) {
                            throw new RuntimeException("More than 1 audio. We need to pick up the languages somehow. Colliding file: " + f.getName());
                        }
                        audiosDone.add(audioFingerPrint);
                        Movie m = new Movie();
                        m.addTrack(mp4Track);
                        Container correctedFile = builder.build(m);
                        File outFile = new File(outputDir, f.getName());
                        correctedFile.writeContainer(
                                new FileOutputStream(outFile).getChannel());
                        System.out.println("Added " + f.getName() + " as new audio.");
                        analyze(outFile, mp4Track.getDuration() / mp4Track.getTrackMetaData().getTimescale());
                    }
                }
            }
            if (f.getName().endsWith(".xml")) {
                String xml = FileUtils.readFileToString(f);
                xml = xml.replace("http://www.w3.org/ns/ttml#style", "http://www.w3.org/ns/ttml#styling");
                xml = xml.replace(" id=\"", " xml:id=\"");
                xml = xml.replace("sansSerif", "sans-serif");
                FileUtils.write(new File(outputDir, f.getName()), xml);
                System.out.println("Added and corrected " + f.getName() + " as subtitle.");
            }
        }


        return 0;
    }

    public void analyze(File outFile, long duration) throws IOException, ExitCodeException {
        final Process p = Runtime.getRuntime().exec(ffprobe + " -count_frames " + outFile.getAbsolutePath());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();

        new Thread() {
            @Override
            public void run() {
                try {
                    IOUtils.copy(p.getErrorStream(), new TeeOutputStream(System.err, err));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        new Thread() {
            @Override
            public void run() {
                try {
                    IOUtils.copy(p.getInputStream(), new TeeOutputStream(System.out, out));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        double errorRatio = ((double)err.toByteArray().length /duration);
        System.err.println(String.format( "Error freeness ratio = %.5f",  errorRatio));
        if (errorRatio > 20) {
            throw new ExitCodeException(outFile.getName() + "'s error density is too high. Manual check required.", 12345);
        }
    }
}
