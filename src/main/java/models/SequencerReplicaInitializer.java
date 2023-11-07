package models;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class SequencerReplicaInitializer extends ChannelInitializer<SocketChannel> {

    public SequencerReplicaInitializer() {
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // For now, add the necessary encoders/decoders for your packet structure.
        // Assuming you have them from your client-sequencer communication.
        ch.pipeline().addLast(new ObjectEncoder());
        ch.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
        //ch.pipeline().addLast(new SequencerServerHandler(sequencer));
        // You can add a handler if you want to process responses from replicas or handle errors.
        // For now, we'll leave it out until we define the behavior we want on responses from replicas.
    }
}
