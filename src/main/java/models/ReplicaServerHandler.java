package models;

import java.net.InetSocketAddress;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.logging.Logger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ReplicaServerHandler extends SimpleChannelInboundHandler<Packet> {

    private static final Logger logger = Logger.getLogger(Sequencer.class.getName());
    Replica replica;

    ReplicaServerHandler(Replica replica) {
        this.replica = replica;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        // Handle the received packet.
        logger.info("Replica received packet: " + packet.getData());
        Header header = packet.getHeader();
        short messageType = header.getMessageType();

        switch (messageType) {
            case 0:
                logger.info("First message from client: " + packet.getSenderId());
                logger.info("senderId: " + packet.getSenderId());
                replica.addClientConnection(packet.getHeader().getSenderId(), ctx);
                break;
            case 1:
                validateSequence(packet);
                processPacket(ctx, packet);
                break;
            case 2:
                InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                String mapKey = socketAddress.getAddress().getHostAddress() + ":" + socketAddress.getPort();
                replica.setReplicaChannel(mapKey, ctx.channel());
                logger.info("New connection from: " + socketAddress);
                break;
            case 3:
                receivedReplicaRequest(packet, ctx);
                break;
            default:
                break;
        }
    }

    private void validateSequence(Packet packet) {
        Short clientKey = packet.getSenderId();
        int receivedSeqNum = packet.getHeader().getSequenceNumber();

        int lastSeqNum = replica.getLastSequenceNumber(clientKey);

        if (receivedSeqNum == lastSeqNum) {
            // Update the last sequence number in the Replica
            replica.updateSequenceNumber(clientKey, receivedSeqNum+1);
            // Add this packet to the recent packets list in the Replica
            replica.addRecentPacket(clientKey, packet);
        } else if (receivedSeqNum <= lastSeqNum) {
            // Packet is a duplicate or out of order
            logger.warning("Received out of order or duplicate packet. Expected: " + (lastSeqNum )
                    + ", but received: " + receivedSeqNum);
            sendReplicasOrderRequest(lastSeqNum,packet.getSenderId());
            // Handle the out-of-order or duplicate packet appropriately
        } else {

            // There is a gap in the sequence
            logger.warning(
                    "Gap in the packet sequence. Expected: " + (lastSeqNum ) + ", but received: " + receivedSeqNum);
            sendReplicasOrderRequest(lastSeqNum, packet.getSenderId());
            // Handle the missing packet(s)
            // For example, you might notify other replicas to request the missing packet(s)
        }

    }

    private void sendReplicasOrderRequest(int sequenceNumber, short senderId) {
        Map<String, Channel> replicaChannels = replica.getReplicaChannels();
        for (Map.Entry<String, Channel> entry : replicaChannels.entrySet()) {
            Channel replicaChannel = entry.getValue();
            if (replicaChannel != null && replicaChannel.isActive()) {
                logger.info("Sending reqeuest to replica: " + entry.getKey() + " for missing packets");
                Header header = new Header(senderId, (short) 3);
                header.setSequenceNumber(sequenceNumber);
                Packet packet = new Packet(header, "Requesting order");
                replicaChannel.writeAndFlush(packet); // Send the packet to the sequencer
            } else {
                logger.warning("Replica Channel is not active. Cannot send packet.");
            }
        }
    }

    private void receivedReplicaRequest(Packet packet, ChannelHandlerContext ctx) {
        // Check if the packet is in the recent packets list
        LinkedHashSet<Packet> missingPackets = replica.getMissingPackets(packet);
        if (missingPackets.size() > 0) {
            // Send each packet in the missing packets list to the replica
            for (Packet missingPacket : missingPackets) {
                ctx.writeAndFlush(missingPacket);
            }
        }else{
            logger.info("No missing packets");
        }
    }

    private void processPacket(ChannelHandlerContext ctx, Packet packet) {
        Short clientKey = packet.getSenderId();

        Header header = new Header(packet.getSenderId());
        Packet responsePacket = new Packet(header,
                "Response to client " + packet.getSenderId() + ": this replica received your message.");
        logger.info("Preparing to send response to client: " + packet.getHeader().getSenderId());

        ChannelHandlerContext clientCtx = replica.getClientConnection(clientKey);
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
            // Handle the case where the context doesn't exist or isn't active
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
