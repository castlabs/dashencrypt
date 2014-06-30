/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.dashfragmenter.mp4todash;

import com.coremedia.iso.Hex;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.FragmentIntersectionFinder;
import com.googlecode.mp4parser.authoring.builder.Mp4Builder;
import com.googlecode.mp4parser.util.UUIDConverter;
import ietfParamsXmlNsKeyprovPskc.*;
import mpegDashSchemaMpd2011.MPDDocument;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlBase64Binary;
import org.apache.xmlbeans.XmlOptions;
import org.kohsuke.args4j.Option;
import org.w3.x2000.x09.xmldsig.KeyInfoType;
import org.w3.x2000.x09.xmldsig.X509DataType;
import org.w3.x2001.x04.xmlenc.CipherDataType;
import org.w3.x2001.x04.xmlenc.EncryptedDataType;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;


public class DashFileSetEncrypt extends DashFileSet {

    protected UUID keyid;
    protected SecretKey key;

    @Option(name = "--uuid",
            aliases = "-u",
            usage = "UUID (KeyID)"
    )
    protected String encKid = null;

    @Option(name = "--secretKey",
            aliases = "-k",
            usage = "Secret Key (Key)",
            depends = {"--uuid"}

    )
    protected String encKeySecretKey = null;

    @Option(name = "--secretKeyFile",
            aliases = "-f",
            usage = "Path to file",
            depends = {"--uuid"}
    )
    protected String encKeySecretKeyFile = null;

    @Option(name = "--certificate", aliases = "-c", usage = "X509 certificate for generation of KDF documents")
    protected List<File> certificates = new LinkedList<File>();

    @Override
    public int run() throws IOException {
        if (((this.encKeySecretKey == null) && (this.encKeySecretKeyFile == null))) {
            byte[] k = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(k);
            this.key = new SecretKeySpec(k, "AES");
            if (keyid == null) {
                this.keyid = UUID.randomUUID();
            }
        } else {
            this.keyid = UUID.fromString(this.encKid);
            if (this.encKeySecretKey != null) {
                this.key = new SecretKeySpec(Hex.decodeHex(this.encKeySecretKey), "AES");
            } else {
                this.key = new SecretKeySpec(Hex.decodeHex(FileUtils.readFileToString(new File(this.encKeySecretKeyFile))), "AES");
            }
        }
        super.run();
        createKdf();

        return 0;
    }

    protected void createKdf() {
        // this.outputDirectory, "kdf.pskcxml"
        try {
            List<X509Certificate> certObjects = new LinkedList<X509Certificate>();
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            for (File certificate : certificates) {
                FileInputStream fis = new FileInputStream(certificate);
                try {
                    certObjects.add((X509Certificate) cf.generateCertificate(fis));
                } catch (CertificateException e) {
                    IOUtils.closeQuietly(fis);
                    throw e;
                }
            }

            for (X509Certificate certificate : certObjects) {
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
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

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


