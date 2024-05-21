package models;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ReplicaServerHandler extends SimpleChannelInboundHandler<List<Packet>> {

    private static final Logger logger = Logger.getLogger(Sequencer.class.getName());
    Replica replica;

    ReplicaServerHandler(Replica replica) {
        this.replica = replica;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, List<Packet> packets) throws Exception {
        // Handle the received packet.
        for (Packet packet : packets) {
            logger.info("Replica received packet: " + packet.getData());
            short messageType = packet.getHeader().getMessageType();

            switch (messageType) {
                case 0:
                    logger.info("New client connection: " + packet.getSenderId());
                    replica.addClientConnection(packet.getSenderId(), ctx);
                    break;
                case 1:
                    logger.info("Replica received message from client: " + packet.getSenderId());
                    validateSequence(packet);
                    break;
                case 2:
                    InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                    String mapKey = socketAddress.getAddress().getHostAddress() + ":" + socketAddress.getPort();
                    replica.setReplicaChannel(mapKey, ctx.channel());
                    logger.info("New connection from: " + socketAddress);
                    break;
                case 3:
                    logger.info("Replica received request from replica: " + packet.getSenderId());
                    receivedReplicaRequest(packet, ctx);
                    break;
                case 4:
                    logger.info("Replica received response from replica: " + packet.getSenderId());
                    validateSequence(packet);
                    break;
                default:
                    break;
            }
        }
    }

    private void validateSequence(Packet packet) {
        int receivedSeqNum = packet.getSequenceNumber();
        int lastSeqNum = replica.getLastSequenceNumber();
        if (receivedSeqNum == lastSeqNum) {
            logger.info("Received expected packet: " + receivedSeqNum);
            replica.updateLastSequenceNumber(receivedSeqNum + 1);
            replica.addToPacketQueue(packet);
            replica.addToRecentPacketSet(packet);
        } else if (receivedSeqNum <= lastSeqNum) {
            // Packet is a duplicate or out of order
        } else {
            // There is a gap in the sequence
            logger.warning(
                    "Gap in the packet sequence. Expected {" + (lastSeqNum) + "}, but received: {" + receivedSeqNum + "} \nAdding packet to waiting queue");
            replica.addToWaitingQueue(packet);
            sendReplicasOrderRequest(lastSeqNum, packet);
        }

    }

    private void sendReplicasOrderRequest(int sequenceNumber, Packet recivedPakcet) {
        Map<String, Channel> replicaChannels = replica.getReplicaChannels();
        Header header = new Header(recivedPakcet.getSenderId(), (short) 3);
        header.setSequenceNumber(sequenceNumber);
        Packet packet = new Packet(header, "Requesting order, first = " + sequenceNumber + " and last = "
                + recivedPakcet.getHeader().getSequenceNumber());
        for (Map.Entry<String, Channel> entry : replicaChannels.entrySet()) {
            Channel replicaChannel = entry.getValue();
            if (replicaChannel != null && replicaChannel.isActive()) {
                logger.info("Sending request to replica: " + entry.getKey() + " for missing packets");
                replicaChannel.writeAndFlush(Arrays.asList(packet)); // Send the packet to the sequencer
            } else {
                logger.warning("Replica Channel is not active. Cannot send packet.");
            }
        }
    }

    private void receivedReplicaRequest(Packet packet, ChannelHandlerContext ctx) {
        // Check if the packet is in the recent packets list
        List<Packet> missingPackets = replica.processMissingPacketsRequest(packet);
        logger.info("Missing packets: " + missingPackets);
        if (missingPackets.size() > 0) {
            ctx.writeAndFlush(missingPackets);
        } else {
            logger.info("No missing packets found");
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
