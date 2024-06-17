package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ClientHandler extends SimpleChannelInboundHandler<Packet> {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    Random rand = new Random();

    Client client;
    public ClientHandler(Client client) {
        this.client = client;
    }

    private static List<Long> timingResults = new ArrayList<>();


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        logger.info("Client received packet: " + packet.getData());
        long startTime = System.nanoTime();
        if (packet.getSequenceNumber() != client.getAndSetShouldSendNewRequest(packet.getSequenceNumber())) {
            logger.info("Received response: " + packet.getData());
            Header header = new Header(client.getClientId());
            int timer = 5 + rand.nextInt(10);
            logger.info("Client will wait " + timer + " seconds before sending next packet.");
            
            TimeUnit.SECONDS.sleep(timer);

            header.setSequenceNumber(client.getLastSequenceNumber());
    
            client.sendRequestSequencer(header);
            
        } else {
            logger.info("Alrady processed this packet. Ignoring.");
        }
        long endTime = System.nanoTime();
        timingResults.add(endTime - startTime);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
