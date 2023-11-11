package models;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class Replica {

    private final int myPort;
    private static final Logger logger = Logger.getLogger(Replica.class.getName());

    private static final ConcurrentHashMap<Short, ChannelHandlerContext> clientConnections = new ConcurrentHashMap<>();
    private final Map<Short, Integer> lastSequenceNumber = new HashMap<>();
    private final Map<String, Channel> replicaChannels = new HashMap<>();
    private final Map<Short, LinkedHashSet<Packet>>  recentPackets = new HashMap<>();
    private final EventLoopGroup group = new NioEventLoopGroup();
    private final int maxRecentPackets = 100;
    private Properties properties;

    public Replica(int myPort) {
        this.myPort = myPort;

        properties = new Properties();
        try {
            FileInputStream fis = new FileInputStream("config.properties");
            properties.load(fis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Map<String, Channel> getReplicaChannels() {
        return new HashMap<>(this.replicaChannels);
    }

    public void addClientConnection(Short clientId, ChannelHandlerContext ctx) {
        clientConnections.put(clientId, ctx);
    }

    public ChannelHandlerContext getClientConnection(Short clientId) {
        return clientConnections.get(clientId);
    }

    public void removeClientConnection(Short clientId) {
        clientConnections.remove(clientId);
    }

    public synchronized void updateSequenceNumber(Short clientKey, int sequenceNumber) {
        lastSequenceNumber.put(clientKey, sequenceNumber);
    }

    public synchronized Integer getLastSequenceNumber(Short clientKey) {
        return lastSequenceNumber.getOrDefault(clientKey, 0);
    }

    public void setReplicaChannel(String key, Channel value) {
        this.replicaChannels.put(key, value);
    }

    public Channel getReplicaChannel(String key) {
        return this.replicaChannels.get(key);
    }

    public synchronized void addRecentPacket(Short clientKey, Packet packet) {
        LinkedHashSet<Packet> packets = recentPackets.computeIfAbsent(clientKey, k -> new LinkedHashSet<>());
        if (packets.size() >= maxRecentPackets) {
            // Remove the oldest packet if we exceed the max number of recent packets
            Packet oldestPacket = packets.iterator().next();
            packets.remove(oldestPacket);
        }
        packets.add(packet);
    }

    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        try {
            connectToReplicas();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, group)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ReplicaServerInitializer(this));

            bootstrap.bind(myPort).sync().channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            group.shutdownGracefully();
        }
    }

    public void connectToReplicas() {
        // first replica starts at port 9001
        for (int port = 9001; port < this.myPort; port++) {
            logger.info("Connecting to replica on port: " + port);
            final int targetPort = port;
            int replicaNumber = port - 9000;
            String ip = properties.getProperty("replica" + replicaNumber + ".ip");
            String mapKey = ip + ":" + port;

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ObjectEncoder());
                            ch.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                            ch.pipeline().addLast(new ReplicaServerHandler(Replica.this));
                        }
                    });

            // Connect to the other replicas
            try {
                Channel channel = bootstrap.connect(ip, port).sync().channel();
                replicaChannels.put(mapKey, channel);
                logger.info("Connected to replica on Port: " + targetPort);
                sendHandShakeReplica(mapKey);

            } catch (InterruptedException e) {
                logger.warning("Error while connecting to replica" + port + ": " + e.getMessage());
            }
        }
    }

    private void sendHandShakeReplica(String mapKey) {
        String message = "First Mensage"; // Random number between 0 and 999
        Header header = new Header((short) 1,(short)2);
        // 2 = hand shake replica - replica
        Packet packet = new Packet(header, message);
        packet.setData(message);
        Channel repliChannel = replicaChannels.get(mapKey);
        if (repliChannel != null && repliChannel.isActive()) {
            logger.info("Seding Hand Shake  to replica " + mapKey);
            repliChannel.writeAndFlush(packet); // Send the packet to the sequencer
        } else {
            logger.warning("Replica Channel is not active. Cannot send packet.");
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]); // Pass the port number as a command-line argument
        logger.info("Starting replica on port " + port);
        Replica replica = new Replica(port);
        replica.start();
    }

    public LinkedHashSet<Packet> getMissingPackets(Packet packet) {
        //return a list of missing packets with the  sequence number greater or equal to the sequence number of the packet
        LinkedHashSet<Packet> packets = recentPackets.get(packet.getSenderId());
        for (Packet p : packets) {
            if (p.getHeader().getSequenceNumber() >= packet.getHeader().getSequenceNumber()) {
                packets.remove(p);
            }
        }
        return packets;

    }

}
