package models;

import java.io.FileReader;
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

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

public class Usig {
    int counter = 0;
    public PrivateKey privateKey;
    public PublicKey publicKey;

    public Usig() {
        try {
            publicKey = importPublicKey();
            privateKey = importPrivateKey();

        } catch (Exception e) {
            System.out.println("Error importing keys");
        }
    }

    public void generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
    }

    private static PrivateKey importPrivateKey() throws Exception {
        FileReader fileReader = new FileReader("privateKey.pem");
        PEMParser pemParser = new PEMParser(fileReader);
        PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();
        pemParser.close();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        return converter.getKeyPair(pemKeyPair).getPrivate();
    }

    public static PublicKey importPublicKey() throws IOException {
        FileReader fileReader = new FileReader("publicKey.pem");
        PEMParser pemParser = new PEMParser(fileReader);
        SubjectPublicKeyInfo subjectPublicKeyInfo = (SubjectPublicKeyInfo) pemParser.readObject();
        pemParser.close();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        return converter.getPublicKey(subjectPublicKeyInfo);
    }

    public void exportPublicKey() {
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter("publicKey.pem"))) {
            pemWriter.writeObject(publicKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportPrivateKey() {
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter("privateKey.pem"))) {
            pemWriter.writeObject(privateKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SignatureCounterPair signMessage(String message){
        try {
            int counter = getCounter();
            String newMessage = message + counter;
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(newMessage.getBytes());
            byte[] digitalSignature = signature.sign();
            SignatureCounterPair messageCounterPair = new SignatureCounterPair(Base64.getEncoder().encodeToString(digitalSignature), counter);
            return messageCounterPair;
            
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error signing message");
        }
        catch (InvalidKeyException  e) {
            System.out.println("Error signing message");

        }
        catch (SignatureException  e) {
            System.out.println("Error signing message");
        }
        return null;
    }

    public int getCounter() {
        return counter++;
    }

    // public static void main(String[] args) throws Exception {
    // Usig usig = new Usig();
    // try {
    // System.out.println("private Key = " + usig.privateKey.toString());
    // System.out.println("Public key = " + usig.publicKey.toString());
    // }catch (Exception e){
    // e.printStackTrace();
    // }
    // }
}
