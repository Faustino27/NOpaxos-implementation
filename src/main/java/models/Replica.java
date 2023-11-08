package models;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Replica{

    private final int port;
    private static final Logger logger = Logger.getLogger(Replica.class.getName());

    private static final ConcurrentHashMap<Short, ChannelHandlerContext> clientConnections = new ConcurrentHashMap<>();
    private final Map<Short, Long> lastSequenceNumber = new HashMap<>();
    private final Map<Short, LinkedHashSet<Packet>> recentPackets = new HashMap<>();
    private final int maxRecentPackets = 100;

    public Replica(int port) {
        this.port = port;
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

    public synchronized void updateSequenceNumber(Short clientKey, long sequenceNumber) {
        lastSequenceNumber.put(clientKey, sequenceNumber);
    }

    public synchronized Long getLastSequenceNumber(Short clientKey) {
        return lastSequenceNumber.getOrDefault(clientKey, 0L);
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
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                     .channel(NioServerSocketChannel.class)
                     .childHandler(new ReplicaServerInitializer(this));

            bootstrap.bind(port).sync().channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]); // Pass the port number as a command-line argument
        logger.info("Starting replica on port " + port);
        Replica replica = new Replica(port);
        replica.start();
    }
}
