package com.castlabs.dash.dashfragmenter.representation;

import com.castlabs.dash.dashfragmenter.ExitCodeException;
import com.castlabs.dash.dashfragmenter.cmdlines.AbstractEncryptOrNotCommand;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CencEncryptingTrackImpl;
import mpegDashSchemaMpd2011.*;
import org.apache.xmlbeans.GDuration;
import org.apache.xmlbeans.XmlOptions;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class DependentTrackVariantA extends AbstractEncryptOrNotCommand {

    @Argument(required = true, multiValued = true, handler = FileOptionHandler.class, usage = "Bitstream input files", metaVar = "vid1.avc1, aud1.dtshd ...")
    protected List<File> inputFiles;

    @Option(name = "--outputdir", aliases = "-o",
            usage = "output directory - if no output directory is given the " +
                    "current working directory is used.",
            metaVar = "PATH")
    protected File outputDirectory = new File(System.getProperty("user.dir"));


    public static XmlOptions getXmlOptions() {
        XmlOptions xmlOptions = new XmlOptions();
        //xmlOptions.setUseDefaultNamespace();
        HashMap<String, String> ns = new HashMap<String, String>();
        //ns.put("urn:mpeg:DASH:schema:MPD:2011", "");
        ns.put("urn:mpeg:cenc:2013", "cenc");
        xmlOptions.setSaveSuggestedPrefixes(ns);
        xmlOptions.setSaveAggressiveNamespaces();
        xmlOptions.setUseDefaultNamespace();
        xmlOptions.setSavePrettyPrint();
        return xmlOptions;
    }

    private MPDDocument getManifest(List<AdaptationSetType> adaptationSets, double maxDuration) throws IOException {

        MPDDocument mdd = MPDDocument.Factory.newInstance();
        MPDtype mpd = mdd.addNewMPD();
        PeriodType periodType = mpd.addNewPeriod();
        periodType.setId("0");
        periodType.setStart(new GDuration(1, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO));

        ProgramInformationType programInformationType = mpd.addNewProgramInformation();
        programInformationType.setMoreInformationURL("www.castLabs.com");


        periodType.setAdaptationSetArray(adaptationSets.toArray(new AdaptationSetType[adaptationSets.size()]));

        periodType.setDuration(new GDuration(
                1, 0, 0, 0, (int) (maxDuration / 3600),
                (int) ((maxDuration % 3600) / 60),
                (int) (maxDuration % 60), BigDecimal.ZERO));

        mpd.setProfiles("urn:mpeg:dash:profile:isoff-on-demand:2011");
        mpd.setType(PresentationType.STATIC); // no mpd update strategy implemented yet, could be dynamic


        mpd.setMinBufferTime(new GDuration(1, 0, 0, 0, 0, 0, 4, BigDecimal.ZERO));
        mpd.setMediaPresentationDuration(periodType.getDuration());

        return mdd;
    }


    public int run() throws IOException, ExitCodeException {
        if (!(outputDirectory.getAbsoluteFile().exists() ^ outputDirectory.getAbsoluteFile().mkdirs())) {
            System.err.println("Output directory does not exist and cannot be created.");
        }
        double maxDurationInSeconds = 0;
        List<DashTrackBuilder> dashTrackBuilders = new ArrayList<DashTrackBuilder>();
        for (File inputFile : inputFiles) {
            if (!inputFile.getName().endsWith(".mp4")) {
                throw new ExitCodeException("Only MP4 files are supported as input.", 87263);
            }
            Movie m = MovieCreator.build(inputFile.getAbsolutePath());

            List<Track> tracks = m.getTracks();

            for (Track track : tracks) {
                double durationInSeconds = (double) track.getDuration() / track.getTrackMetaData().getTimescale();
                maxDurationInSeconds = Math.max(maxDurationInSeconds, durationInSeconds);
            }

            int dvhe = -1;
            int hvc1 = -1;
            int audio = -1;
            for (int i = 0; i < tracks.size(); i++) {
                String codec = tracks.get(i).getSampleDescriptionBox().getSampleEntry().getType();
                if (codec.equals("dvhe")) {
                    dvhe = i;
                }
                if (codec.equals("hvc1")) {
                    hvc1 = i;
                }
                if (codec.equals("mp4a") || codec.equals("ac-3") || codec.equals("ec-3") || codec.equals("ec-3") || codec.equals("ec-3")) {
                    audio = i;
                }
            }
            int aaa = dashTrackBuilders.size();
            if (dvhe >= 0 && hvc1 >= 0) {
                if (videoKeyId != null) {
                    dashTrackBuilders.add(new DependentTrackDashBuilder(
                            new CencEncryptingTrackImpl(tracks.get(hvc1), videoKeyId, videoKey, false),
                            tracks.get(dvhe), "vdep", 48));
                } else {
                    dashTrackBuilders.add(new DependentTrackDashBuilder(tracks.get(hvc1), tracks.get(dvhe), "vdep", 48));
                }
            } else if (dvhe == 0 && hvc1 >= 0) {
                throw new ExitCodeException("Only combined tracks are supported momentarily.", 811573);
            } else if (dvhe >= 0 && hvc1 == 0) {
                throw new ExitCodeException("Only combined tracks are supported momentarily.", 811573);
            }
            if (audio >= 0) {
                throw new ExitCodeException("Audio tracks are not yet supported.", 8573);
            }
            if (aaa == dashTrackBuilders.size()) {
                throw new ExitCodeException("No Representation has been created", 9873);
            }
        }
        int v = 1;
        int a = 1;

        Map<String, AdaptationSetType> adaptationSets = new HashMap<String, AdaptationSetType>();

        for (DashTrackBuilder dashTrackBuilder : dashTrackBuilders) {

            if (dashTrackBuilder instanceof SegmentTemplateRepresentation) {
                String id;
                if (dashTrackBuilder.getPrimaryTrack().getHandler().equals("soun")) {
                    id = "a" + (a++);
                } else if (dashTrackBuilder.getPrimaryTrack().getHandler().equals("vide")) {
                    id = "v" + (v++);
                } else {
                    throw new ExitCodeException("I don't support " + dashTrackBuilder.getPrimaryTrack().getHandler(), 28763);
                }

                DashTrackWriter.write(dashTrackBuilder, new File(outputDirectory.getAbsolutePath(), id + ".mp4").getAbsolutePath());

                RepresentationType representation = ((SegmentTemplateRepresentation) dashTrackBuilder).getSegmentTemplateRepresentation();
                representation.addNewBaseURL().setStringValue(id + ".mp4");
                representation.setId(id);
                String type = dashTrackBuilder.getPrimaryTrack().getSampleDescriptionBox().getSampleEntry().getType();
                type += dashTrackBuilder.getPrimaryTrack().getTrackMetaData().getLanguage();

                AdaptationSetType adaptationSet = adaptationSets.get(type);
                if (adaptationSet == null) {
                    adaptationSet = AdaptationSetType.Factory.newInstance();
                    adaptationSets.put(type, adaptationSet);
                }
                RepresentationType[] representationsInThisSet = adaptationSet.getRepresentationArray();
                representationsInThisSet = Arrays.copyOf(representationsInThisSet, representationsInThisSet.length + 1);
                representationsInThisSet[representationsInThisSet.length - 1] = representation;
                adaptationSet.setRepresentationArray(representationsInThisSet);

            } else {
                throw new ExitCodeException("Only SegmentTemplateRepresentation are working by now", 9183);
            }
        }

        MPDDocument mpd = getManifest(new ArrayList<AdaptationSetType>(adaptationSets.values()), maxDurationInSeconds);
        ManifestOptimizer manifestOptimizer = new ManifestOptimizer();
        manifestOptimizer.optimize(mpd);
        File manifest1 = new File(outputDirectory, "Manifest.mpd");
        mpd.save(manifest1, getXmlOptions());


        return 0;
    }
}
