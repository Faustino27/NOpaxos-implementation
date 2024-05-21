package models;

import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

public class Usig {
    int counter = 0;
    PrivateKey privateKey;
    PublicKey publicKey;

    public Usig() throws NoSuchAlgorithmException, InvalidKeyException {
        generateKeyPair();
        exportPublicKey();
    }

    public void generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
    }

    public void exportPublicKey() {
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter("publicKey.pem"))) {
            pemWriter.writeObject(publicKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 

    public String signMessage(String message) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes());
        byte[] digitalSignature = signature.sign();
        return Base64.getEncoder().encodeToString(digitalSignature);
    }

    public int getCounter() {
        return counter++;
    }

    public static void main(String[] args) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
        Usig usig = new Usig();
        try {
            String signature = usig.signMessage("Hello World");
            String signature2 = usig.signMessage("Hello World2");
            System.out.println("Signature 1 = " + signature + " size = " + signature.length());
            System.out.println("Signature 2 = " + signature2);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }
}
