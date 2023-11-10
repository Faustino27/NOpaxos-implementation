package models;

import java.io.Serializable;

public class Header implements Serializable {
    private int sequenceNumber; // The sequence number assigned by the sequencer
    private String groupId; // The ID of the group to which the packet belongs
    private short senderId;
    private boolean firstMessage;
    private boolean replica;

    // Constructor, getters, and setters for the Header class
    public Header(short senderId) {
        this.senderId = senderId;
        this.firstMessage = false;
        this.replica = true;
    }

    public Header(short senderId, int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
        // this.senderAdress = senderAdress;
        this.senderId = senderId;
        this.firstMessage = false;

    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getGroupId() {
        return groupId;
    }

    public short getSenderId() {
        return senderId;
    }

    public void setSenderId(short senderId) {
        this.senderId = senderId;
    }

    public void setFirstMessage(boolean firstMessage) {
        this.firstMessage = firstMessage;
    }

    public boolean isFirstMessage() {
        return this.firstMessage;
    }
    public void setReplica(boolean isReplica) {
        this.replica = isReplica;
    }

    public boolean isReplica() {
        return this.replica;
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
