package com.castlabs.dash.dashfragmenter.formats.kdf;

import com.castlabs.dash.dashfragmenter.ExitCodeException;
import ietfParamsXmlNsKeyprovPskc.*;
import org.apache.xmlbeans.XmlBase64Binary;
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
import java.util.UUID;

public class KdfCreator {
    public static void createKdf(File outFile, X509Certificate certificate, SecretKey key, UUID keyId) throws ExitCodeException {
        // this.outputDirectory, "kdf.pskcxml"
        try {

                KeyContainerDocument keyContainerDocument = KeyContainerDocument.Factory.newInstance();

                KeyContainerType keyContainer = keyContainerDocument.addNewKeyContainer();
                keyContainer.setVersion("1.0");


                KeyInfoType keyInfoType = keyContainer.addNewEncryptionKey();
                X509DataType x509DataType = keyInfoType.addNewX509Data();
                XmlBase64Binary xmlBase64BinaryCertificate = x509DataType.addNewX509Certificate();
                xmlBase64BinaryCertificate.setByteArrayValue(certificate.getEncoded());

                KeyPackageType keyPackage = keyContainer.addNewKeyPackage();
                KeyType keyType = keyPackage.addNewKey();
                keyType.setId(keyId.toString().replace("-", ""));
                keyType.setAlgorithm("urn:dece:pskc:contentkey");
                KeyDataType keyDataType = keyType.addNewData();
                BinaryDataType binaryDataTypeSecret = keyDataType.addNewSecret();
                EncryptedDataType encryptedDataType = binaryDataTypeSecret.addNewEncryptedValue();
                encryptedDataType.addNewEncryptionMethod().setAlgorithm("http://www.w3.org/2001/04/xmlenc#rsa_1_5");
                Cipher cipher = Cipher.getInstance("RSA");

                cipher.init(Cipher.ENCRYPT_MODE, certificate);
                CipherDataType cipherDataType = encryptedDataType.addNewCipherData();
                cipherDataType.setCipherValue(cipher.doFinal(key.getEncoded()));

                keyContainerDocument.save(outFile);



        } catch (InvalidKeyException e) {
            throw new ExitCodeException(e.getMessage(), 1);
        } catch (CertificateEncodingException e) {
            throw new ExitCodeException(e.getMessage(), 1);
        } catch (NoSuchAlgorithmException e) {
            throw new ExitCodeException(e.getMessage(), 1);
        } catch (NoSuchPaddingException e) {
            throw new ExitCodeException(e.getMessage(), 1);
        } catch (BadPaddingException e) {
            throw new ExitCodeException(e.getMessage(), 1);
        } catch (IllegalBlockSizeException e) {
            throw new ExitCodeException(e.getMessage(), 1);
        } catch (IOException e) {
            throw new ExitCodeException(e.getMessage(), 1);
        }

    }
}
