package models;

public class Packet {

    private Header header; // The header of the packet
    private String data; // The data contained in the packet

    public Packet() {
    }

    public Packet(Header header, String data) {
        this.header = header;
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "header=" + header +
                ", data='" + data + '\'' +
                '}';
    }

}
