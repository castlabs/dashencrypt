/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.AbstractCommand;
import com.castlabs.dash.dashfragmenter.ExitCodeException;
import com.castlabs.dash.dashfragmenter.sequences.DashFileSetEncryptSequence;
import com.coremedia.iso.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;

import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;


public class DashFileSetEncrypt extends AbstractCommand {

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

    @Argument(required = true, multiValued = true, handler = FileOptionHandler.class, usage = "MP4 and bitstream input files", metaVar = "vid1.mp4, vid2.mp4, aud1.mp4, aud2.ec3 ...")
    protected List<File> inputFiles;

    @Option(name = "--outputdir", aliases = "-o",
            usage = "output directory - if no output directory is given the " +
                    "current working directory is used.",
            metaVar = "PATH")
    protected File outputDirectory = new File("");


    @Option(name = "--explode", aliases = "-x", usage = "If this option is set each segement will be written in a single file")
    protected boolean explode = false;


    @Override
    public int run() throws IOException, ExitCodeException {
        DashFileSetEncryptSequence d = new DashFileSetEncryptSequence();
        d.setExplode(explode);
        d.setLogger(setupLogger());
        d.setOutputDirectory(outputDirectory);
        d.setInputFiles(inputFiles);

        List<X509Certificate> certObjects = new LinkedList<X509Certificate>();
        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X509");
            for (File certificate : certificates) {
                FileInputStream fis = new FileInputStream(certificate);
                try {
                    certObjects.add((X509Certificate) cf.generateCertificate(fis));
                } catch (CertificateException e) {
                    IOUtils.closeQuietly(fis);
                    throw e;
                }
            }
        } catch (CertificateException e) {
            throw new ExitCodeException(e.getMessage(), 1);
        }
        d.setCertificates(certObjects);


        if (((this.encKeySecretKey == null) && (this.encKeySecretKeyFile == null))) {
            byte[] k = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(k);
            d.setKey(new SecretKeySpec(k, "AES"));

            if (encKid == null) {
                d.setKeyid(UUID.randomUUID());
            } else {
                d.setKeyid(UUID.fromString(this.encKid));
            }
        } else {
            d.setKeyid(UUID.fromString(this.encKid));
            if (this.encKeySecretKey != null) {
                d.setKey(new SecretKeySpec(Hex.decodeHex(this.encKeySecretKey), "AES"));
            } else {
                d.setKey(new SecretKeySpec(Hex.decodeHex(FileUtils.readFileToString(new File(this.encKeySecretKeyFile))), "AES"));
            }
        }

        return d.run();
    }


}


