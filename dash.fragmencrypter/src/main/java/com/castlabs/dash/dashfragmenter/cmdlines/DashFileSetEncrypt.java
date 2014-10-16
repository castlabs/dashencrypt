/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.AbstractCommand;
import com.castlabs.dash.dashfragmenter.ExitCodeException;
import com.castlabs.dash.dashfragmenter.sequences.DashFileSetSequence;
import com.coremedia.iso.Hex;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;


public class DashFileSetEncrypt extends AbstractCommand {

    @Option(name = "--uuid:a",
            aliases = "-u:a",
            usage = "UUID (KeyID) for audio streams",
            depends = {"--secretKey", "--uuid"}
    )
    protected String encKidAudio = null;

    @Option(name = "--secretKey:a",
            aliases = "-k:a",
            usage = "Secret Key (CEK) for audio streams",
            depends = {"--uuid:a"}

    )
    protected String encKeySecretKeyAudio = null;


    @Option(name = "--uuid",
            aliases = {"-u", "-u:v", "-uuid:v"},
            usage = "UUID (KeyID) for video streams (will also be used for audio streams if no separate audio key is given)"
    )
    protected String encKid = null;

    @Option(name = "--secretKey",
            aliases = {"-k", "-k:v", "-secretKey:v"},
            usage = "Secret Key (CEK) for video streams (will also be used for audio streams if no separate audio key is given",
            depends = {"--uuid"}

    )
    protected String encKeySecretKey = null;

    @Option(name = "--sparse",
            aliases = "-s",
            usage = "0=encrypt everything, 1=encrypted if default, some sample clear, 2=clear is default, important samples are encrypted"
    )
    protected int sparse = 0;


    @Option(name = "--clearlead",
            aliases = "-C",
            usage = "seconds of unencrypted content after start"
    )
    protected int clearLead = 0;


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


    public int run() throws IOException, ExitCodeException {
        DashFileSetSequence d = new DashFileSetSequence();
        d.setExplode(explode);
        d.setSparse(sparse);
        d.setClearlead(clearLead);
        d.setLogger(setupLogger());
        d.setOutputDirectory(outputDirectory);
        d.setInputFiles(inputFiles);
        d.setEncryptionAlgo("cenc");

        SecretKey audioKey;
        SecretKey videoKey;
        UUID audioKeyId;
        UUID videoKeyId;


        if (this.encKeySecretKey == null) {
            byte[] k = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(k);
            audioKey = videoKey = (new SecretKeySpec(k, "AES"));

            if (encKid == null) {
                audioKeyId = videoKeyId = (UUID.randomUUID());
            } else {
                audioKeyId = videoKeyId = (UUID.fromString(this.encKid));
            }
            System.out.println(audioKeyId.toString() + ":" + Hex.encodeHex(audioKey.getEncoded()));
            System.out.println(videoKeyId.toString() + ":" + Hex.encodeHex(videoKey.getEncoded()));
        } else {
            videoKeyId = UUID.fromString(this.encKid);
            if (encKidAudio != null) {
                audioKeyId = UUID.fromString(this.encKidAudio);
            } else {
                audioKeyId = videoKeyId;
            }
            videoKey = new SecretKeySpec(Hex.decodeHex(this.encKeySecretKey), "AES");
            if (encKeySecretKeyAudio != null) {
                audioKey = new SecretKeySpec(Hex.decodeHex(this.encKeySecretKeyAudio), "AES");
            } else {
                audioKey = videoKey;
            }
        }
        d.setAudioKeyid(audioKeyId);
        d.setVideoKeyid(videoKeyId);
        d.setAudioKey(audioKey);
        d.setVideoKey(videoKey);


        return d.run();
    }


}


