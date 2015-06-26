package com.castlabs.dash.dashfragmenter.cmdlines;

import com.castlabs.dash.dashfragmenter.Command;
import org.apache.commons.io.FileUtils;
import org.apache.xmlbeans.XmlOptions;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.UuidOptionHandler;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Future super class of all commands.
 */
public abstract class AbstractEncryptOrNotCommand implements Command {
    protected SecretKey videoKey;
    protected SecretKey audioKey;


    @Option(name = "--encrypt",
            aliases = {"-1", "-e"},
            forbids = "--encrypt-separate-keys",
            usage = "If option is set a single key for audio and video will be generated."
    )
    protected boolean encryptSingleKey = false;


    @Option(name = "--encrypt-separate-keys",
            aliases = "-2",
            forbids = "--encrypt",
            usage = "If set different keys will be used for audio and video. When setting keys explicitly this option is not used."
    )
    protected boolean encryptSeparateKey = false;


    @Option(name = "--uuid:v",
            aliases = {"-u", "-u:v", "--uuid"},
            forbids = {"--encrypt", "--encrypt-separate-keys"},
            handler = UuidOptionHandler.class,
            usage = "Key ID for video tracks - in case no dedicated audio key ID is set it will also used for audio. Format is '01010101-0101-0101-0101-010101010101'"
    )
    protected UUID videoKeyId = null;

    @Option(name = "--secretKey:v",
            aliases = {"-k", "-k:v", "--secretKey"},
            depends = {"--uuid:v"},
            forbids = {"--secretKeyFile:v"},
            usage = "Content Encryption Key for video tracks - in case no dedicated audio key/key ID is given it will also used for audio tracks. Format is '01010101010101010101010101010101'"
    )
    protected String encKeySecretKeyVideo = null;

    @Option(name = "--secretKeyFile:v",
            aliases = {"-f", "-f:v", "--secretKeyFile"},
            depends = {"--setKeys", "--uuid:v"},
            forbids = {"--keyseed", "--secretKey:v"},
            usage = "Path to a file containing the Content Encryption Key for video tracks - in case no dedicated audio key/key ID is given it will also used for audio tracks. Format is '01010101010101010101010101010101'"
    )
    protected File encKeySecretKeyFileVideo = null;


    @Option(name = "--uuid:a",
            aliases = "-u:a",
            usage = "Key ID for audio tracks. The default value is random.",
            handler = UuidOptionHandler.class,
            depends = {"--uuid:v"}
    )
    protected UUID audioKeyId = null;

    @Option(name = "--secretKey:a",
            aliases = "-k:a",
            usage = "Content Encryption Key for audio tracks. Format is '01010101010101010101010101010101'",
            depends = {"--secretKey:v", "--uuid:a"},
            forbids = {"--keyseed", "--secretKeyFile:a"}
    )
    protected String encKeySecretKeyAudio = null;

    @Option(name = "--secretKeyFile:a",
            aliases = "-f:a",
            usage = "Path to a file containing the Content Encryption Key for audio tracks. Format is '01010101010101010101010101010101'",
            depends = {"--secretKeyFile:v", "--uuid:a"},
            forbids = {"--keyseed", "--secretKey:a"}

    )
    protected File encKeySecretKeyFileAudio = null;


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


    public void postProcessCmdLineArgs(CmdLineParser cmdLineParser) throws CmdLineException {

        try {

            if (encryptSingleKey || encryptSeparateKey) {
                videoKey = audioKey = KeyGenerator.getInstance("AES").generateKey();
                videoKeyId = audioKeyId = UUID.randomUUID();
            }
            if (encryptSeparateKey) {
                audioKey = KeyGenerator.getInstance("AES").generateKey();
                audioKeyId = UUID.randomUUID();
            }
        } catch (NoSuchAlgorithmException e) {
            throw new CmdLineException(cmdLineParser, "The AES encryption algorithm is not supported by your VM");
        }

        if (audioKeyId != null) {
            if (encKeySecretKeyAudio == null && encKeySecretKeyFileAudio == null && audioKey == null) {
                throw new CmdLineException(cmdLineParser, "--uuid\\:a requires either --secretKeyFile\\:a, --secretKey\\:a or --keyseed");
            }
        }
        if (videoKeyId != null) {
            if (encKeySecretKeyVideo == null && encKeySecretKeyFileVideo == null && videoKey == null) {
                throw new CmdLineException(cmdLineParser, "--uuid:v requires either --secretKeyFile:v, --secretKey:v or --keyseed");
            }
            if (audioKeyId == null) {
                audioKeyId = videoKeyId;
            }
        }

        if (this.encKeySecretKeyVideo != null) {
            audioKey = videoKey = new SecretKeySpec(com.coremedia.iso.Hex.decodeHex(this.encKeySecretKeyVideo), "AES");
        } else if (this.encKeySecretKeyFileVideo != null) {
            try {
                audioKey = videoKey = new SecretKeySpec(com.coremedia.iso.Hex.decodeHex(FileUtils.readFileToString(this.encKeySecretKeyFileVideo)), "AES");
            } catch (IOException e) {
                throw new CmdLineException(cmdLineParser, "Content Encryption Key file " + this.encKeySecretKeyFileAudio.getAbsolutePath() + " could not be read");
            }
        }

        if (videoKeyId != null) {
            if (this.encKeySecretKeyAudio != null) {
                audioKey = new SecretKeySpec(com.coremedia.iso.Hex.decodeHex(this.encKeySecretKeyAudio), "AES");
            } else if (this.encKeySecretKeyFileAudio != null) {
                try {
                    audioKey = new SecretKeySpec(com.coremedia.iso.Hex.decodeHex(FileUtils.readFileToString((this.encKeySecretKeyFileAudio))), "AES");
                } catch (IOException e) {
                    throw new CmdLineException(cmdLineParser, "Content Encryption Key file " + this.encKeySecretKeyFileAudio.getAbsolutePath() + " could not be read");
                }
            }
        }
    }
}
