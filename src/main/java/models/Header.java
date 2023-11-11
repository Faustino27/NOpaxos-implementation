package models;

import java.io.Serializable;

public class Header implements Serializable {
    private int sequenceNumber; // The sequence number assigned by the sequencer
    private short senderId;
    private short messageType;
    // 0 = client hand Shake, 1 - Client request, 2 - replica hand Shake, 3 - replica request, 4 - replica response
    // Constructor, getters, and setters for the Header class
    public Header(short senderId) {
        this.senderId = senderId;
        this.messageType = 1;
    }

    public Header(short senderId, short messageType) {
        this.senderId = senderId;
        this.messageType = messageType;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public short getSenderId() {
        return senderId;
    }

    public void setSenderId(short senderId) {
        this.senderId = senderId;
    }

    public void setMessageType(short messageType) {
        this.messageType = messageType;
    }

    public short getMessageType() {
        return this.messageType;
    }
    @Override
    public String toString() {
        return "Header{" +
                "sequenceNumber=" + sequenceNumber +
                // ", sessionNumber=" + sessionNumber +
                // ", groupId='" + groupId + '\'' +
                ", senderId='" + senderId + '\'' +
                '}';
    }

}
