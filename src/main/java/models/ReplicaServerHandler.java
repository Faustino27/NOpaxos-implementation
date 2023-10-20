package models;

import java.util.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ReplicaServerHandler extends SimpleChannelInboundHandler<Packet> {

    private static final Logger logger = Logger.getLogger(Sequencer.class.getName());

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        // Handle the received packet.
        logger.info("Replica received packet: " + packet.getData());
        Header header = new Header(packet.getSenderId(), 0, 0, true);
        Packet responsePacket = new Packet(header, "Response to client " + packet.getSenderId() + ": replica received your message.");
        logger.info("Sending response to client: " + packet.getSenderId()+ "\npakcet = " + responsePacket.getData());

        ctx.writeAndFlush(responsePacket);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
