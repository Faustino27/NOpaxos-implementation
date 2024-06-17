package models;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    final int MAX_ENTRIES = 100;

    LinkedHashMap<String, Boolean> messagesSent = new LinkedHashMap<String, Boolean>(MAX_ENTRIES + 1, 1.0f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_ENTRIES;
        }
    };
    private Integer counter = 0;
    private Map<String, Channel> replicaChannels = new HashMap<>();
    private Properties properties;
    private static final Logger logger = Logger.getLogger(Sequencer.class.getName());
    private Usig usig = new Usig();

    public Sequencer() {
        properties = new Properties();
        try {
            FileInputStream fis = new FileInputStream("config.properties");
            properties.load(fis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
        String messageId = header.getSenderId() + ":" + header.getSequenceNumber();

        if (messagesSent.containsKey(messageId)) {
            logger.info("Message already sent");
            return;
        }

        
        messagesSent.put(messageId, true);
        
        SignatureCounterPair signedMessage = usig.signMessage(packet.getData());
        header.setSignature(signedMessage.getSignature());
        header.setSequenceNumber(signedMessage.getCounter());
        counter = signedMessage.getCounter();
        
        logger.info(String.format("Processed packet from client %s with sequence number %d",
                header.getSenderId(), counter - 1));

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
                if (replicaChannels.get("replica1") == channel && counter == 2) {
                    logger.info("Simulando falha na replica 1");
                } else {
                    channel.writeAndFlush(Arrays.asList(packet));
                }
            } else {
                // Handle disconnected replicas
            }
        }
    }

    public void connectToReplicas() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new SequencerReplicaInitializer());

        for (int i = 1; i <= 3; i++) {
            String ip = properties.getProperty("replica" + i + ".ip");
            int port = Integer.parseInt(properties.getProperty("replica" + i + ".port"));

            try {
                Channel channel = bootstrap.connect(ip, port).sync().channel();
                replicaChannels.put("replica" + i, channel);
                logger.info("Connected to replica" + i + " at " + ip + ":" + port);
            } catch (InterruptedException e) {
                logger.severe("Error while connecting to replica" + i + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        int port = 8080; // Choose your desired port
        Sequencer sequencer = new Sequencer();
        sequencer.startServer(port);
    }

}
