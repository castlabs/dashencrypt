package com.castlabs.dash.dashfragmenter.mp4todash;

import com.coremedia.iso.Hex;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.FragmentIntersectionFinder;
import com.googlecode.mp4parser.authoring.builder.Mp4Builder;
import com.googlecode.mp4parser.util.UUIDConverter;
import mpegDASHSchemaMPD2011.MPDDocument;
import org.apache.commons.io.FileUtils;
import org.apache.xmlbeans.XmlOptions;
import org.kohsuke.args4j.Option;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class DashFileSetEncrypt extends DashFileSet {

    UUID keyid;
    SecretKey key;

    @Option(name = "--uuid",
            aliases = "-u",
            usage = "UUID (KeyID)"
    )
    String encKid = null;

    @Option(name = "--secretKey",
            aliases = "-k",
            usage = "Secret Key (Key)",
            depends = {"--uuid"}

    )
    String encKeySecretKey = null;

    @Option(name = "--secretKeyFile",
            aliases = "-f",
            usage = "Path to file",
            depends = {"--uuid"}
    )
    String encKeySecretKeyFile = null;

    @Override
    public int run() throws IOException {
        if (((this.encKeySecretKey == null) && (this.encKeySecretKeyFile == null))) {
            System.out.println("Please specify --secretKey or --secretKeyFile ");
            // --uuid requirements is done via cmd line parser
            return 1236;
        } else {
            this.keyid = UUID.fromString(this.encKid);
            if (this.encKeySecretKey != null) {
                this.key = new SecretKeySpec(Hex.decodeHex(this.encKeySecretKey), "AES");
            } else {
                this.key = new SecretKeySpec(Hex.decodeHex(FileUtils.readFileToString(new File(this.encKeySecretKeyFile))), "AES");
            }
        }
        super.run();

        return 0;
    }

    @Override
    protected void writeManifest(Map<String, List<Track>> trackFamilies, Map<Track, Long> trackBitrate, Map<Track, String> trackFilename, Map<Track, Container> dashedFiles) throws IOException {

        Map<Track, UUID> trackKeyIds = new HashMap<Track, UUID>();
        for (List<Track> tracks : trackFamilies.values()) {
            for (Track track : tracks) {
                trackKeyIds.put(track, this.keyid);
            }
        }
        SegmentBaseSingleSidxManifestWriterImpl dashManifestWriter = new SegmentBaseSingleSidxManifestWriterImpl(
                trackFamilies, dashedFiles,
                trackBitrate, trackFilename,
                trackKeyIds);
        MPDDocument mpdDocument = dashManifestWriter.getManifest();

        XmlOptions xmlOptions = new XmlOptions();
        //xmlOptions.setUseDefaultNamespace();
        HashMap<String, String> ns = new HashMap<String, String>();
        //ns.put("urn:mpeg:DASH:schema:MPD:2011", "");
        ns.put("urn:mpeg:cenc:2013", "cenc");
        xmlOptions.setSaveSuggestedPrefixes(ns);
        xmlOptions.setSaveAggressiveNamespaces();
        xmlOptions.setUseDefaultNamespace();
        xmlOptions.setSavePrettyPrint();

        mpdDocument.save(new File(this.outputDirectory, "Manifest.mpd"), xmlOptions);
    }

    @Override
    protected Mp4Builder getFileBuilder(FragmentIntersectionFinder fragmentIntersectionFinder, Movie m) {
        DashEncryptedBuilder dashBuilder = new DashEncryptedBuilder();
        for (Track track : m.getTracks()) {
            dashBuilder.getKeyIds().put(track, UUIDConverter.convert(this.keyid));
            dashBuilder.getKeys().put(track, this.key);
        }
        dashBuilder.setIntersectionFinder(fragmentIntersectionFinder);
        // dashBuilder.setPsshBoxes(Map<byte[], byte[])); ( a map from systemId to drm specific pssh content )
        return dashBuilder;
    }

}


