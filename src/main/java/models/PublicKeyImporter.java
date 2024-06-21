package models;

import java.io.FileReader;
import java.io.IOException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

public class PublicKeyImporter {

    PublicKey publicKey;

    public PublicKeyImporter() {

        this.publicKey = importPublicKey();
    }

    public static PublicKey importPublicKey() {
        try {
            FileReader fileReader = new FileReader("publicKey.pem");
            PEMParser pemParser = new PEMParser(fileReader);
            SubjectPublicKeyInfo subjectPublicKeyInfo = (SubjectPublicKeyInfo) pemParser.readObject();
            pemParser.close();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            return converter.getPublicKey(subjectPublicKeyInfo);
        } catch (IOException e) {
            System.out.println("Error importing public key");
            return null;
        }

    }

    public boolean verifySignature(String message, String signature){
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(this.publicKey);
            sig.update(message.getBytes());
            return sig.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            System.out.println("Error verifying signature");
            return false;
        }
    }
}
