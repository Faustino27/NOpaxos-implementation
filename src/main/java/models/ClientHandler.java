package models;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ClientHandler extends SimpleChannelInboundHandler<Packet> {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private AtomicInteger totalMessages = new AtomicInteger(0);
    private static final int MAX_MESSAGES = 1000;
    private AtomicBoolean processing = new AtomicBoolean(false); // Flag to indicate if a message is being processed

    Client client;

    public ClientHandler(Client client) {
        this.client = client;
    }

    private static List<Long> timingResults = new ArrayList<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        synchronized (this) { // Synchronize the block to control the sending process
            if (processing.get()) {
                logger.info("Already processing a packet. Ignoring.");
                return;
            }

            processing.set(true);

            if (totalMessages.incrementAndGet() >= MAX_MESSAGES) {
                logger.info("Reached maximum message limit of " + MAX_MESSAGES + ". Shutting down client.");
                writeTimingResultsToFile();
                client.stop();
                return;
            }
            long startTime = System.nanoTime();

            if (client.getAndSetShouldSendNewRequest(packet.getSequenceNumber())) {
                logger.info("Received response: " + packet.getData());
                Header header = new Header(client.getClientId());
                logger.info("Seding new message number: " + totalMessages);

                header.setSequenceNumber(client.getLastSequenceNumber());

                client.sendRequestSequencer(header);
                long endTime = System.nanoTime();
                timingResults.add((endTime - startTime) / 1000000);

            } 
            processing.set(false);
        }
    }

    private void writeTimingResultsToFile() {
        if (client.getClientId() == 0) {
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
