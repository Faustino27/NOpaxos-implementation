package models;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class SequencerServerInitializer extends ChannelInitializer<SocketChannel> {
    
    private Sequencer sequencer;

    public SequencerServerInitializer(Sequencer sequencer) {
        this.sequencer = sequencer;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // Add encoders and decoders specific to your Packet class
        ch.pipeline().addLast(new PacketEncoder());
        ch.pipeline().addLast(new PacketDecoder());
        // Add the server-side handler
        ch.pipeline().addLast(new SequencerServerHandler(sequencer));
    }
}

