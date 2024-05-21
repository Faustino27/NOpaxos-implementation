package models;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Client {

    private final String hostSequencer;
    private final int portSequencer;
    private final short clientId;
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private Bootstrap bootstrap;
    private Channel sequencerChannel;
    private Map<String, Channel> replicaChannels = new HashMap<>();
    private int lastSequenceNumber = 0;
    private Properties properties;

    private final AtomicInteger shouldSendNewRequest = new AtomicInteger(-1);

    public int getAndSetShouldSendNewRequest(int newValue) {
        return shouldSendNewRequest.getAndSet(newValue);
    }

    private EventLoopGroup group; // Moved to class level to shut it down gracefully
    Random rand = new Random();

    public Client(String host, int port) {
        this.hostSequencer = host;
        this.portSequencer = port;
        this.clientId = (short) new Random().nextInt(Short.MAX_VALUE + 1);

        properties = new Properties();
        try {
            FileInputStream fis = new FileInputStream("config.properties");
            properties.load(fis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public short getClientId() {
        return this.clientId;
    }

    public void start() {
        group = new NioEventLoopGroup();

        try {
            bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ClientInitializer(this)); // Define your client's channel pipeline here

            sequencerChannel = bootstrap.connect(hostSequencer, portSequencer).sync().channel();
            // Send messages or perform other client operations here
            logger.info("Client connected to sequencer at " + hostSequencer + ":" + portSequencer);
            connectToReplicas();
            Header header = new Header(this.clientId);
            logger.info("Header's senderId: " + header.getSenderId());
            header.setSequenceNumber(lastSequenceNumber++);
            logger.info("Header's sequenceNumber: " + lastSequenceNumber);

            sendRequestSequencer(header);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void stop() {
        try {
            if (sequencerChannel != null) {
                sequencerChannel.close().sync();
            }
            group.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            logger.warning("Error while shutting down client: " + e.getMessage());
        }
    }

    public void sendRequestSequencer(Header header) {
        Packet packet = new Packet(header, genereateNewMenssage());
        if (sequencerChannel != null && sequencerChannel.isActive()) {
            logger.info("Client " + clientId + " is sending the request: " + packet.toString());
            sequencerChannel.writeAndFlush(packet);
        } else {
            logger.warning("Sequencer Channel is not active. Cannot send packet.");
        }
    }

    public String genereateNewMenssage() {
        Random rand = new Random();
        return "Random Integer sent from client: " + rand.nextInt(1000);
    }

    public void sendRequestReplica(int replicaNumber) {
        String message = "First Mensage"; 
        Header header = new Header(clientId, (short) 0);
        Packet packet = new Packet(header, message);
        packet.setData(message);

        Channel repliChannel = replicaChannels.get("replica" + replicaNumber);
        if (repliChannel != null && repliChannel.isActive()) {
            logger.info("SENDING FIRST MENSAGE TO REPLICA " + replicaNumber);
            repliChannel.writeAndFlush(Arrays.asList(packet));
        } else {
            logger.warning("Replica Channel is not active. Cannot send packet.");
        }
    }

    public void connectToReplicas() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ClientInitializer(this));
        for (int i = 1; i <= 3; i++) { // Adjust the loop to match the number of replicas
            String ip = properties.getProperty("replica" + i + ".ip");
            int port = Integer.parseInt(properties.getProperty("replica" + i + ".port"));

            try {
                Channel channel = bootstrap.connect(ip, port).sync().channel();
                replicaChannels.put("replica" + i, channel);
                logger.info("Connected to replica" + i + " at " + ip + ":" + port);
                sendRequestReplica(i);
            } catch (InterruptedException e) {
                logger.warning("Error while connecting to replica" + i + ": " + e.getMessage());
            }
        }
    }

    public int getLastSequenceNumber() {
        return lastSequenceNumber++;
    }

    public static void main(String[] args) {
        Client client = new Client("localhost", 8080);
        client.start();
        // client.stop();
    }

}