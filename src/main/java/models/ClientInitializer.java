package models;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class ClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new PacketEncoder(), new PacketDecoder(), new ClientHandler());

        // Here, you can add encoders, decoders, and handlers to the pipeline
        // ch.pipeline().addLast(new YourEncoder());
        // ch.pipeline().addLast(new YourDecoder());
        // ch.pipeline().addLast(new ClientHandler());
    }
}
 