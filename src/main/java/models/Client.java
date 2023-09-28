package models;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.concurrent.TimeUnit;


public class Client {

    private final String host;
    private final int port;
    private final String clientId;
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private Bootstrap bootstrap;
    private Channel channel;
    private EventLoopGroup group; // Moved to class level to shut it down gracefully
    Random rand = new Random();


    public Client(String host, int port) {
        this.host = host;
        this.port = port;
        this.clientId = UUID.randomUUID().toString();
    }

    public void start() {
        group = new NioEventLoopGroup();

        try {
            bootstrap = new Bootstrap();
            bootstrap.group(group)
                     .channel(NioSocketChannel.class)
                     .handler(new ClientInitializer());  // Define your client's channel pipeline here

            channel = bootstrap.connect(host, port).sync().channel();
            // Send messages or perform other client operations here
            Header header = new Header(clientId);
            logger.info("Header's senderId: " + header.getSenderId());
            Packet myPacket = new Packet(header, "Hello world!");
            while (true) {
                // Send messages or perform other client operations here
                sendRequest(myPacket);
    
                // Sleep for a random duration between 2 to 5 seconds
                TimeUnit.SECONDS.sleep(10 + rand.nextInt(4));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    public void stop() {
        try {
            if (channel != null) {
                channel.close().sync();
            }
            group.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            logger.warning("Error while shutting down client: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Client client = new Client("localhost", 8080);
        client.start();

        client.stop();
    }

    public void sendRequest(Packet packet) {
        Random rand = new Random();
        String message = "Hello replica " + rand.nextInt(1000); // Random number between 0 and 999
        packet.setData(message);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(packet);  // Send the packet to the sequencer
            logger.info("Client " + clientId + " is sending the request: " + packet.toString());
        } else {
            logger.warning("Channel is not active. Cannot send packet.");
        }
    }
    
    
    
}