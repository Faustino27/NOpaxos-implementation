package models;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Sequencer {

    private Map<Short, Integer> senderCounters; // A map to maintain counters for different senders
    // private Map<String, Integer> sessionNumbers; // A map to maintain OUM session
    // numbers for different groups
    private Map<String, Channel> replicaChannels = new HashMap<>(); // Maintains connections to replicas

    private Properties properties;
    private static final Logger logger = Logger.getLogger(Sequencer.class.getName());

    public Sequencer() {
        this.senderCounters = new HashMap<>(); // Initialize the group counters map
        // this.sessionNumbers = new HashMap<>(); // Initialize the OUM session numbers
        // map
        properties = new Properties();
        try {
            FileInputStream fis = new FileInputStream("config.properties");
            properties.load(fis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to process a packet (assign a sequence number and update the session
    // number)
    public void processPacket(Packet packet) {
        if (packet == null) {
            logger.warning("Received packet is null.");
            return;
        }
        if (packet.getHeader() == null) {
            logger.warning("Received packet's header is null.");
            return;
        }

        Header header = packet.getHeader(); // Get the header from the packet
        short senderId = header.getSenderId(); // Get the group ID from the header

        // Get and update the counter for the group
        int senderCounter = this.senderCounters.getOrDefault(senderId, 0);
        // if(senderCounter == 3){
        //     this.senderCounters.put(senderId, 6);
        // }else{
        // }
        
        this.senderCounters.put(senderId, senderCounter + 1);
        header.setSequenceNumber(senderCounter);

        // Log the processing of the packet
        logger.info(String.format("Processed packet from client %s with sequence number %d",
                header.getSenderId(), senderCounter));
        logger.info("Packet data: " + packet.getData());
        forwardPacketToReplicas(packet);
    }

    // Method to handle sequencer failures
    public void handleSequencerFailure(String groupId) {

    }

    public void startServer(int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new SequencerServerInitializer(this));

            connectToReplicas();

            logger.info("Starting sequencer server on port " + port);
            bootstrap.bind(port).sync().channel().closeFuture().sync();

            logger.info("Sequencer server stopped");
        } catch (Exception e) {
            logger.severe("Error while starting the sequencer server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            logger.info("Sequencer server shutdown complete");
        }
    }

    public void forwardPacketToReplicas(Packet packet) {
        for (Channel channel : replicaChannels.values()) {
            if (channel.isActive()) {
                channel.writeAndFlush(packet);
            } else {
                // Handle disconnected replicas (e.g., retries, logging, etc.)
            }
        }
    }

    public void connectToReplicas() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new SequencerReplicaInitializer()); // This will handle communication with replicas

        for (int i = 1; i <= 3; i++) {
            String ip = properties.getProperty("replica" + i + ".ip");
            int port = Integer.parseInt(properties.getProperty("replica" + i + ".port"));

            try {
                Channel channel = bootstrap.connect(ip, port).sync().channel();
                replicaChannels.put("replica" + i, channel);
                logger.info("Connected to replica" + i + " at " + ip + ":" + port);
            } catch (InterruptedException e) {
                logger.severe("Error while connecting to replica" + i + ": " + e.getMessage());
                e.printStackTrace(); // or log it using a logger
            }
        }
    }

    public static void main(String[] args) {
        int port = 8080; // Choose your desired port
        Sequencer sequencer = new Sequencer();
        sequencer.startServer(port);
    }

}
