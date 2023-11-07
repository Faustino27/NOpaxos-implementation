package models;

import java.util.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;


public class ReplicaServerHandler extends SimpleChannelInboundHandler<Packet> {
    
    private static final Logger logger = Logger.getLogger(Sequencer.class.getName());
    Replica replica;

    ReplicaServerHandler(Replica replica){
        this.replica = replica;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        // Handle the received packet.
        logger.info("Replica received packet: " + packet.getData());
        if(packet.getHeader().isFirstMessage()){
            logger.info("First message from client: " + packet.getSenderId());
            logger.info("senderId: " + packet.getSenderId());
            replica.addClientConnection(packet.getHeader().getSenderId(), ctx);
        }else{
            treatMessage(ctx, packet);
        }


    }

    private void treatMessage(ChannelHandlerContext ctx, Packet packet){
        Short clientKey = packet.getSenderId();

        Header header = new Header(packet.getSenderId());
        Packet responsePacket = new Packet(header,
                "Response to client " + packet.getSenderId() + ": replica received your message.");
        logger.info("Preparing to send response to client: " + packet.getHeader().getSenderId());

        ChannelHandlerContext clientCtx = replica.getClientConnection(clientKey);
        logger.info("all connections saved");

        // Check if the context exists and is active
        if (clientCtx != null && clientCtx.channel().isActive()) {
            // Write and flush the response packet to the client using the existing context
            clientCtx.writeAndFlush(responsePacket).addListener(future -> {
                if (future.isSuccess()) {
                    logger.info("Response sent to client: " + packet.getSenderId());
                } else {
                    logger.warning("Failed to send response to client: " + packet.getSenderId());
                }
                // Optionally, you can decide whether to close the connection here
            });
        } else {
            logger.warning("No active context found for client: " + packet.getSenderId());
            logger.info("Client connections: " + replica.getClientConnection(clientKey));
            // Handle the case where the context doesn't exist or isn't active
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
