package models;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class PacketUtils {

    static Usig usig = new Usig();

    public static int getObjectSizeInBytes(Object obj) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
            return byteArrayOutputStream.size();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void main(String[] args) {
        Header header = new Header((short) 1);
        Packet packet = new Packet(header, "");
        SignatureCounterPair signedMessage = usig.signMessage(packet.getData());
        header.setSignature(signedMessage.getSignature());
        header.setSequenceNumber(signedMessage.getCounter());

        Header headerUsigned = new Header((short) 1);
        Packet packetUsigned = new Packet(headerUsigned, "");
        headerUsigned.setSequenceNumber(1);	

        int sizeInBytesSigned = getObjectSizeInBytes(packet);
        int sizeInBytesUsigned = getObjectSizeInBytes(packetUsigned);

        System.out.println("Size of Signed Packet object in bytes: " + sizeInBytesSigned);
        System.out.println("Size of Unsigned Packet object in bytes: " + sizeInBytesUsigned);

    }
}
