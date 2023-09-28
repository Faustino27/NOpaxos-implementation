package models;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PacketEncoder extends MessageToByteEncoder<Packet> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) throws Exception {
        // Convert the packet's header and data to bytes and write to the ByteBuf
        // For simplicity, let's assume that the header contains a UUID in string format
        // and the data is a string. This is a basic example.
        
        byte[] headerBytes = packet.getHeader().toString().getBytes();
        byte[] dataBytes = packet.getData().getBytes();

        // First write lengths
        out.writeInt(headerBytes.length);
        out.writeInt(dataBytes.length);

        // Then write the actual bytes
        out.writeBytes(headerBytes);
        out.writeBytes(dataBytes);
    }
}
