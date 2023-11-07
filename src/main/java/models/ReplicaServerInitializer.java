package models;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class ReplicaServerInitializer extends ChannelInitializer<SocketChannel> {

    Replica replica;
    ReplicaServerInitializer(Replica replica){
        this.replica = replica;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // Add encoders and decoders specific to your Packet class
        ch.pipeline().addLast(new ObjectEncoder());
        ch.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
        // Add the handler
        ch.pipeline().addLast(new ReplicaServerHandler(replica));
    }
}
