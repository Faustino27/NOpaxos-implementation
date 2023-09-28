package models;

public class Header {
    private int sequenceNumber; // The sequence number assigned by the sequencer
    private int sessionNumber; // The OUM session number assigned by the sequencer
    private String groupId; // The ID of the group to which the packet belongs
    private String senderId;

    // Constructor, getters, and setters for the Header class
    public Header(String senderId) {
        this.senderId = senderId;
    }

    public Header(String senderId, int sequenceNumber, int sessionNumber) {
        this.sequenceNumber = sequenceNumber;
        this.sessionNumber = sessionNumber;
        this.senderId = senderId;

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

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
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
