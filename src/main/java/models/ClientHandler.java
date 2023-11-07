package models;

import java.util.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ClientHandler extends SimpleChannelInboundHandler<Packet> {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        logger.info("Received response: " + packet.getData());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
