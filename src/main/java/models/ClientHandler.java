package models;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ClientHandler extends SimpleChannelInboundHandler<Packet> {
    private Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private AtomicBoolean processing = new AtomicBoolean(false); // Flag to indicate if a message is being processed
    int timesSaved = 0;
    Client client;
    long startTime = System.nanoTime();

    public ClientHandler(Client client) {
        this.client = client;
    }

    private List<Long> timingResults = new ArrayList<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {

        
        if (processing.get()) {
            // logger.info("Already processing a packet. Ignoring.");
            return;
        }
        processing.set(true);
        
        if (timingResults.size() == 10000) {
            //logger.info("10000 requests processed. Stopping client " + client.getClientId());

            writeTimingResultsToFile();
            return;
        }

        if (client.getAndSetShouldSendNewRequest(packet.getSequenceNumber())) {
            timingResults.add((System.nanoTime() - startTime));
            Header header = new Header(client.getClientId());
            header.setSequenceNumber(client.getLastSequenceNumber());

            Packet newPacket = new Packet(header, "");
            newPacket.setDataSize(client.getDataSize());

            startTime = System.nanoTime();

            client.sendRequestSequencer(newPacket);

        }
        processing.set(false);
    }

    private void writeTimingResultsToFile() {
        if (client.getClientId() == 0) {
            Header header = new Header(client.getClientId());
            header.setMessageType((short) 5);
            header.setSequenceNumber(client.getLastSequenceNumber());
            Packet newPacket = new Packet(header, "");

            client.sendRequestSequencer(newPacket);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("timing_results.txt"))) {
                for (int i = 0; i < timingResults.size(); i++) {
                    writer.write((i + 1) + " - " + timingResults.get(i));
                    writer.newLine();
                }
                logger.info("Timing results written to timing_results.txt");
            } catch (IOException e) {
                logger.severe("Error writing timing results to file: " + e.getMessage());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
