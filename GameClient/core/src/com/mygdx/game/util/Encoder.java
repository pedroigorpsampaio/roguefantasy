package com.mygdx.game.util;

import com.badlogic.gdx.Gdx;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.KeyTransRecipientInformation;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class Encoder {
    private static Encoder instance; // singleton instance
    private final X509Certificate certificate;
    private final PrivateKey key;

    public Encoder() {
        Security.setProperty("crypto.policy", "unlimited");
        int maxKeySize = 0;
        try {
            maxKeySize = javax.crypto.Cipher.getMaxAllowedKeyLength("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        //System.out.println("Max Key Size for AES : " + maxKeySize);

        //Security.addProvider(new BouncyCastleProvider());
        CertificateFactory certFactory= null;
        try {
            certFactory = CertificateFactory
                    .getInstance("X.509");
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }

        try {
            certificate = (X509Certificate) certFactory
                    .generateCertificate(Gdx.files.internal("data/Soleus.cer").read());
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }

        char[] keystorePassword = "zonwWdh33OaaMAkoti5q".toCharArray();
        char[] keyPassword = "zonwWdh33OaaMAkoti5q".toCharArray();

        KeyStore keystore = null;
        try {
            keystore = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
        try {
            keystore.load(Gdx.files.internal("data/Soleus.p12").read(), keystorePassword);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
        try {
            key = (PrivateKey) keystore.getKey("soleus", keyPassword);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] generateSalt16Byte() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);

        //byte[] salt = new byte[16]; // 16 x0's fixed salt

        return salt;
    }

    // generates hash to be stored in db from raw password
    public static String generateHash(String rawPassword) {
        return BCrypt.withDefaults().hashToString(10, rawPassword.toCharArray());
    }

    // checks if raw password matches with bcrypt hash
    public static boolean verifyBCryptHash(String rawPassword, String bcryptHash) {
        BCrypt.Result result = BCrypt.verifyer().verify(rawPassword.toCharArray(), bcryptHash);
        return result.verified;
    }

    public static Encoder getInstance() {
        if(instance == null)
            instance = new Encoder();

        return instance;
    }

    // signs and then encrypts message, returning an array of bytes containing the result
    public byte[] signAndEncryptData(String plainData) {
        // sign data
        byte[] signedBytes;
        try {
            signedBytes = signData(plainData.getBytes(), certificate, key);
        } catch (CertificateEncodingException e) {
            System.err.println("Error signing data: "+e.getMessage());
            return new byte[0];
        } catch (OperatorCreationException e) {
            System.err.println("Error signing data: "+e.getMessage());
            return new byte[0];
        } catch (CMSException e) {
            System.err.println("Error signing data: "+e.getMessage());
            return new byte[0];
        } catch (IOException e) {
            System.err.println("Error signing data: "+e.getMessage());
            return new byte[0];
        }
        // confirms data is correctly signed
        Boolean check = false;
        try {
            check = verifySignData(signedBytes);
        } catch (CMSException e) {
            System.err.println("Could not verify data signature after signing: "+e.getMessage());
        } catch (IOException e) {
            System.err.println("Could not verify data signature after signing: "+e.getMessage());
        } catch (OperatorCreationException e) {
            System.err.println("Could not verify data signature after signing: "+e.getMessage());
        } catch (CertificateException e) {
            System.err.println("Could not verify data signature after signing: "+e.getMessage());
        }
        if(check == false) {
            System.err.println("Signature does not match");
            return new byte[0];
        }
        // encrypts data
        byte[] encryptedData;
        try {
            encryptedData = encryptData(signedBytes, certificate);
        } catch (CertificateEncodingException e) {
            System.err.println("Error while encrypting the data: "+e.getMessage());
            return new byte[0];
        } catch (CMSException e) {
            System.err.println("Error while encrypting the data: "+e.getMessage());
            return new byte[0];
        } catch (IOException e) {
            System.err.println("Error while encrypting the data: "+e.getMessage());
            return new byte[0];
        }
        // returns encrypted data
        return encryptedData;
    }

    public String decryptSignedData(byte[] encryptedData) {
        // decrypts data
        byte[] decryptedData;
        try {
            decryptedData = decryptData(encryptedData, key);
        } catch (CMSException e) {
            System.err.println("Error while decrypting the data: "+e.getMessage());
            return "";
        }
        // Now get the content contained in the CMS EncapsulatedContentInfo
        CMSSignedData signedData = null;
        try {
            signedData = new CMSSignedData(decryptedData);
        } catch (CMSException e) {
            System.err.println("Error while getting data content: "+e.getMessage());
            return "";
        }
        CMSProcessable p = signedData.getSignedContent();
        byte[] dataContent = (byte[]) p.getContent();
        // return data content as string
        return new String(dataContent, Charset.forName("UTF-8"));
    }

    private byte[] encryptData(byte[] data,
                                     X509Certificate encryptionCertificate)
            throws CertificateEncodingException, CMSException, IOException {

        byte[] encryptedData = null;
        if (null != data && null != encryptionCertificate) {
            CMSEnvelopedDataGenerator cmsEnvelopedDataGenerator = new CMSEnvelopedDataGenerator();
            JceKeyTransRecipientInfoGenerator jceKey = new JceKeyTransRecipientInfoGenerator(encryptionCertificate);
            cmsEnvelopedDataGenerator.addRecipientInfoGenerator(jceKey);
            CMSTypedData msg = new CMSProcessableByteArray(data);
            OutputEncryptor encryptor = new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES128_CBC).build();
            CMSEnvelopedData cmsEnvelopedData = cmsEnvelopedDataGenerator.generate(msg, encryptor);
            encryptedData = cmsEnvelopedData.getEncoded();
        }
        return encryptedData;
    }

    private byte[] decryptData(
            byte[] encryptedData,
            PrivateKey decryptionKey)
            throws CMSException {

        byte[] decryptedData = null;
        if (null != encryptedData && null != decryptionKey) {
            CMSEnvelopedData envelopedData = new CMSEnvelopedData(encryptedData);

            Collection<RecipientInformation> recipients
                    = envelopedData.getRecipientInfos().getRecipients();
            KeyTransRecipientInformation recipientInfo
                    = (KeyTransRecipientInformation) recipients.iterator().next();
            JceKeyTransRecipient recipient
                    = new JceKeyTransEnvelopedRecipient(decryptionKey);

            return recipientInfo.getContent(recipient);
        }
        return decryptedData;
    }

    private byte[] signData(byte[] data, final X509Certificate signingCertificate, final PrivateKey signingKey) throws CertificateEncodingException, OperatorCreationException, CMSException, IOException {
        byte[] signedMessage = null;
        List<X509Certificate> certList = new ArrayList<X509Certificate>();
        CMSTypedData cmsData = new CMSProcessableByteArray(data);
        certList.add(signingCertificate);
        Store certs = new JcaCertStore(certList);
        CMSSignedDataGenerator cmsGenerator = new CMSSignedDataGenerator();
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(signingKey);
        cmsGenerator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build()).build(contentSigner, signingCertificate));
        cmsGenerator.addCertificates(certs);
        CMSSignedData cms = cmsGenerator.generate(cmsData, true);
        signedMessage = cms.getEncoded();
        return signedMessage;
    }

    public static boolean verifySignData(final byte[] signedData) throws CMSException, IOException, OperatorCreationException, CertificateException {
        ByteArrayInputStream bIn = new ByteArrayInputStream(signedData);
        ASN1InputStream aIn = new ASN1InputStream(bIn);
        CMSSignedData s = new CMSSignedData(ContentInfo.getInstance(aIn.readObject()));
        aIn.close();
        bIn.close();
        Store certs = s.getCertificates();
        SignerInformationStore signers = s.getSignerInfos();
        Collection<SignerInformation> c = signers.getSigners();
        Iterator it = c.iterator();
        Boolean verified = false;
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation) it.next();
            Collection<X509CertificateHolder> certCollection = certs.getMatches(signer.getSID());
            Iterator<X509CertificateHolder> certIt = certCollection.iterator();
            X509CertificateHolder certHolder = certIt.next();
            X509Certificate certFromSignedData = new JcaX509CertificateConverter().getCertificate(certHolder);
            if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certFromSignedData))) {
                verified = true;
            } else {
                verified = false;
            }
        }
        return verified;
    }
}
