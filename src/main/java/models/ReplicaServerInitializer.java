package models;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class ReplicaServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // Add encoders and decoders specific to your Packet class
        ch.pipeline().addLast(new PacketEncoder());
        ch.pipeline().addLast(new PacketDecoder());
        // Add the handler
        ch.pipeline().addLast(new ReplicaServerHandler());
    }
}
