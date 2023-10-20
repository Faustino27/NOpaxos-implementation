package models;

import java.io.Serializable;

public class Header implements Serializable{
    private int sequenceNumber; // The sequence number assigned by the sequencer
    private int sessionNumber; // The OUM session number assigned by the sequencer
    private String groupId; // The ID of the group to which the packet belongs
    private short senderId;
    private boolean mensageType; // false = request, true = reply

    // Constructor, getters, and setters for the Header class
    public Header(short senderId) {
        this.senderId = senderId;
    }

    public Header(short senderId, int sequenceNumber, int sessionNumber, boolean mensageType) {
        this.sequenceNumber = sequenceNumber;
        this.sessionNumber = sessionNumber;
        this.senderId = senderId;
        this.mensageType = mensageType;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(int sessionNumber) {
        this.sessionNumber = sessionNumber;
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

    public boolean getMensageType() {
        return mensageType;
    }

    public void setMensageType(boolean mensageType) {
        this.mensageType = mensageType;
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
