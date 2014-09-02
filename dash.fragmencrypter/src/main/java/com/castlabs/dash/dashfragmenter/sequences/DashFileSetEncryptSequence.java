package com.castlabs.dash.dashfragmenter.sequences;

import com.castlabs.dash.dashfragmenter.ExitCodeException;
import com.castlabs.dash.dashfragmenter.formats.csf.SegmentBaseSingleSidxManifestWriterImpl;
import com.castlabs.dash.dashfragmenter.formats.multiplefilessegementtemplate.ExplodedSegmentListManifestWriterImpl;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.tracks.CencEncryptingTrackImpl;
import ietfParamsXmlNsKeyprovPskc.*;
import mpegDashSchemaMpd2011.MPDDocument;
import org.apache.xmlbeans.XmlBase64Binary;
import org.apache.xmlbeans.XmlOptions;
import org.w3.x2000.x09.xmldsig.KeyInfoType;
import org.w3.x2000.x09.xmldsig.X509DataType;
import org.w3.x2001.x04.xmlenc.CipherDataType;
import org.w3.x2001.x04.xmlenc.EncryptedDataType;


import javax.crypto.*;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Enhances the DashFileSetSequence with encryption.
 */
public class DashFileSetEncryptSequence extends DashFileSetSequence {
    SecretKey key;
    UUID keyid;
    List<X509Certificate> certificates;

    public void setKey(SecretKey key) {
        this.key = key;
    }

    public void setKeyid(UUID keyid) {
        this.keyid = keyid;
    }

    public void setCertificates(List<X509Certificate> certificates) {
        this.certificates = certificates;
    }

    @Override
    public int run() throws IOException, ExitCodeException {

        super.run();
        createKdf();

        return 0;
    }

    @Override
    protected Map<Track, String> createTracks() throws IOException, ExitCodeException {
        Map<Track, String> tracks = super.createTracks();
        Map<Track, String> encTracks = new HashMap<Track, String>();
        for (Map.Entry<Track, String> trackStringEntry : tracks.entrySet()) {
            String hdlr = trackStringEntry.getKey().getHandler();
            if ("vide".equals(hdlr) || "soun".equals(hdlr)) {
                CencEncryptingTrackImpl cencTrack = new CencEncryptingTrackImpl(trackStringEntry.getKey(), keyid, key );
                encTracks.put(cencTrack, trackStringEntry.getValue());
            } else {
                encTracks.put(trackStringEntry.getKey(), trackStringEntry.getValue());
            }
        }
        return encTracks;
    }

    protected void createKdf() throws ExitCodeException {
        // this.outputDirectory, "kdf.pskcxml"
        try {

            for (X509Certificate certificate : certificates) {
                KeyContainerDocument keyContainerDocument = KeyContainerDocument.Factory.newInstance();

                KeyContainerType keyContainer = keyContainerDocument.addNewKeyContainer();
                keyContainer.setVersion("1.0");


                KeyInfoType keyInfoType = keyContainer.addNewEncryptionKey();
                X509DataType x509DataType = keyInfoType.addNewX509Data();
                XmlBase64Binary xmlBase64BinaryCertificate = x509DataType.addNewX509Certificate();
                xmlBase64BinaryCertificate.setByteArrayValue(certificate.getEncoded());

                KeyPackageType keyPackage = keyContainer.addNewKeyPackage();
                KeyType keyType = keyPackage.addNewKey();
                keyType.setId(keyid.toString().replace("-", ""));
                keyType.setAlgorithm("urn:dece:pskc:contentkey");
                KeyDataType keyDataType = keyType.addNewData();
                BinaryDataType binaryDataTypeSecret = keyDataType.addNewSecret();
                EncryptedDataType encryptedDataType = binaryDataTypeSecret.addNewEncryptedValue();
                encryptedDataType.addNewEncryptionMethod().setAlgorithm("http://www.w3.org/2001/04/xmlenc#rsa_1_5");
                Cipher cipher = Cipher.getInstance("RSA");

                cipher.init(Cipher.ENCRYPT_MODE, certificate);
                CipherDataType cipherDataType = encryptedDataType.addNewCipherData();
                cipherDataType.setCipherValue(cipher.doFinal(key.getEncoded()));

                keyContainerDocument.save(new File(this.outputDirectory, certificate.getSubjectDN().getName() + ".pskcxml"));
            }



        } catch (InvalidKeyException e) {
            l.log(Level.FINE, e.getMessage(), e);
            e.printStackTrace();
            throw new ExitCodeException(e.getMessage(), 1);
        } catch (CertificateEncodingException e) {
            l.log(Level.FINE, e.getMessage(), e);
            throw new ExitCodeException(e.getMessage(), 1);
        } catch (NoSuchAlgorithmException e) {
            l.log(Level.FINE, e.getMessage(), e);
            throw new ExitCodeException(e.getMessage(), 1);
        } catch (NoSuchPaddingException e) {
            l.log(Level.FINE, e.getMessage(), e);
            throw new ExitCodeException(e.getMessage(), 1);
        } catch (BadPaddingException e) {
            l.log(Level.FINE, e.getMessage(), e);
            throw new ExitCodeException(e.getMessage(), 1);
        } catch (IllegalBlockSizeException e) {
            l.log(Level.FINE, e.getMessage(), e);
            throw new ExitCodeException(e.getMessage(), 1);
        } catch (IOException e) {
            l.log(Level.FINE, e.getMessage(), e);
            throw new ExitCodeException(e.getMessage(), 1);
        }

    }

    protected void writeManifestExploded(Map<String, List<Track>> trackFamilies,
                                         Map<Track, Long> trackBitrate,
                                         Map<Track, String> trackFilename,
                                         Map<Track, Container> dashedFiles,
                                         Map<Track, List<File>> trackToSegments,
                                         File outputDirectory, String initPattern, String mediaPattern) throws IOException {
        Map<Track, UUID> trackKeyIds = new HashMap<Track, UUID>();
        for (List<Track> tracks : trackFamilies.values()) {
            for (Track track : tracks) {
                trackKeyIds.put(track, this.keyid);
            }
        }
        MPDDocument mpdDocument =
                new ExplodedSegmentListManifestWriterImpl(
                        trackFamilies, dashedFiles, trackBitrate, trackFilename,
                        trackKeyIds, trackToSegments, initPattern, mediaPattern).getManifest();

        XmlOptions xmlOptions = new XmlOptions();
        //xmlOptions.setUseDefaultNamespace();
        HashMap<String, String> ns = new HashMap<String, String>();
        //ns.put("urn:mpeg:DASH:schema:MPD:2011", "");
        ns.put("urn:mpeg:cenc:2013", "cenc");
        xmlOptions.setSaveSuggestedPrefixes(ns);
        xmlOptions.setSaveAggressiveNamespaces();
        xmlOptions.setUseDefaultNamespace();
        xmlOptions.setSavePrettyPrint();
        File manifest1 = new File(outputDirectory, "Manifest.mpd");
        l.info("Writing " + manifest1 + "... ");
        mpdDocument.save(manifest1, xmlOptions);
        l.info("Done.");

    }

    @Override
    protected void writeManifestSingleSidx(Map<String, List<Track>> trackFamilies, Map<Track, Long> trackBitrate, Map<Track, String> trackFilename, Map<Track, Container> dashedFiles) throws IOException {

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
}
