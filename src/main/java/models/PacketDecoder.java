package models;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class PacketDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // For simplicity, we're assuming that the packet's header is a UUID in string format
        // and the data is a string. This matches the encoder's assumption.

        if (in.readableBytes() < 8) { // Checking for two integers worth of bytes
            return; // Not enough data yet
        }

        in.markReaderIndex(); // Mark the current position

        int headerLength = in.readInt();
        int dataLength = in.readInt();

        if (in.readableBytes() < headerLength + dataLength) {
            in.resetReaderIndex(); // Reset to the marked position
            return; // Full packet not yet received
        }

        byte[] headerBytes = new byte[headerLength];
        in.readBytes(headerBytes);
        String headerString = new String(headerBytes);
        Header header = new Header(headerString);

        byte[] dataBytes = new byte[dataLength];
        in.readBytes(dataBytes);
        String dataString = new String(dataBytes);

        Packet packet = new Packet(header, dataString);
        out.add(packet);
    }
}
