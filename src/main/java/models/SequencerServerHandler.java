package models;

import java.util.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class SequencerServerHandler extends ChannelInboundHandlerAdapter {

    private Sequencer sequencer;
    private static final Logger logger = Logger.getLogger(SequencerServerHandler.class.getName()); // Logger for error handling and logging


    public SequencerServerHandler(Sequencer sequencer) {
        this.sequencer = sequencer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (sequencer == null) {
            logger.warning("Sequencer is null in SequencerServerHandler.");
            return;
        }
        
        if (msg instanceof Packet) {
            Packet receivedPacket = (Packet) msg;
            if (receivedPacket == null || receivedPacket.getHeader() == null) {
                logger.info("Received a null packet or packet with a null header.");
                return;
            }
            Header header = receivedPacket.getHeader();
            if(!header.getMensageType()){
                sequencer.getClientChannels().putIfAbsent(header.getSenderId(), ctx.channel());
                sequencer.processPacket(receivedPacket);
            }
            else if(header.getMensageType()){
                logger.info("Seding reponse from replica back to client " + header.getSenderId());
                sequencer.getClientChannels().get(header.getSenderId()).writeAndFlush(receivedPacket);
            }
        }
    }

    

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}

