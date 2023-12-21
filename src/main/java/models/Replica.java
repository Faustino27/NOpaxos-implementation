package models;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private Integer lastSequenceNumber = 0;
    private Integer expectedPacketToProcess = 0;

    private final Map<String, Channel> replicaChannels = new HashMap<>();
    private final LinkedHashSet<Packet> recentPackets = new LinkedHashSet<>();
    private final BlockingQueue<Packet> waitingQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Packet> packetQueue = new LinkedBlockingQueue<>();
    private final EventLoopGroup group = new NioEventLoopGroup(1);
    private final int maxRecentPackets = 100;
    private Properties properties;
    private final Object lockObject = new Object(); // This lock object is shared across threads

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

    public synchronized void addRecentPacket(Packet packet) {
        if (recentPackets.size() >= maxRecentPackets) {
            // Remove the oldest packet if we exceed the max number of recent packets
            Packet oldestPacket = recentPackets.iterator().next();
            recentPackets.remove(oldestPacket);
        }
        recentPackets.add(packet);
    }

    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
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
        Header header = new Header((short) 1, (short) 2);
        // 2 = hand shake replica - replica
        Packet packet = new Packet(header, message);
        packet.setData(message);
        Channel repliChannel = replicaChannels.get(mapKey);
        if (repliChannel != null && repliChannel.isActive()) {
            logger.info("Seding Hand Shake to replica " + mapKey);
            repliChannel.writeAndFlush(Arrays.asList(packet)); 
        } else {
            logger.warning("Replica Channel is not active. Cannot send packet.");
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]); // Pass the port number as a command-line argument
        logger.info("Starting replica on port " + port);
        Replica replica = new Replica(port);
        new Thread(replica::processPacketLoop).start();
        replica.start();

    }

    public List<Packet> getMissingPackets(Packet packet) {
        List<Packet> packets = new ArrayList<>();
        int[] numbers = getNumbers(packet.getData());
        logger.info("Getting missing packets between " + numbers[0] + " and " + numbers[1]);
        for (Packet p : recentPackets) {
            if (numbers[1] <= p.getSequenceNumber()) {
                break;
            }
            if (numbers[0] <= p.getSequenceNumber()) {
                logger.info("Adding packet to missing packets list: " + p.getSequenceNumber() + " - first = "
                        + numbers[0]);
                Packet copy = new Packet(p.getHeader(), p.getData());
                copy.getHeader().setMessageType((short) 4);
                packets.add(copy);
            }
        }
        return packets;
    }

    private void processPacketLoop() {
        while (true) {
            processFirstPacketQueue();
        }
    }

    private void processFirstPacketQueue() {
        synchronized (lockObject) {
            if ((packetQueue.isEmpty() && waitingQueue.isEmpty())) {
                try {
                    lockObject.wait(); // Wait until a condition might be true
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Handle interrupted exception
                    return;
                }
            }
        }

        if (!packetQueue.isEmpty()
                && packetQueue.peek().getSequenceNumber() == expectedPacketToProcess) {
            logger.info("Processing first packet in queue: " + packetQueue.peek().getSequenceNumber());
            processPacket(removeFromPacketQueue());
            expectedPacketToProcess++;
        } else if (!waitingQueue.isEmpty() && waitingQueue.peek().getSequenceNumber() == expectedPacketToProcess) {
            logger.info("Processing first packet in WAITING queue: " + waitingQueue.peek().getSequenceNumber());
            processPacket(removeFromWaitingQueue());
            expectedPacketToProcess++;
            lastSequenceNumber++;
        }
    }

    private void processPacket(Packet packet) {
        Short clientKey = packet.getSenderId();
        ChannelHandlerContext clientCtx = getClientConnection(clientKey);
        Packet responsePacket = new Packet(packet.getHeader(),
                "Response to client " + packet.getSenderId() + ": replica " + this.myPort + " received your message.");
        logger.info("Preparing to send response to client: " + packet.getHeader().getSenderId());
        if (clientCtx != null && clientCtx.channel().isActive()) {
            clientCtx.writeAndFlush(responsePacket).addListener(future -> {
                if (future.isSuccess()) {
                    logger.info("Response sent to client: " + packet.getSenderId());
                } else {
                    logger.warning("Failed to send response to client: " + packet.getSenderId());
                }
            });
        } else {
            logger.warning("No active context found for client: " + packet.getSenderId());
        }

    }

    private static int[] getNumbers(String str) {
        Pattern pattern = Pattern.compile("first\\s*=\\s*(\\d+)\\s*and\\s*last\\s*=\\s*(\\d+)");
        Matcher matcher = pattern.matcher(str);

        if (matcher.find()) {
            int firstNumber = Integer.parseInt(matcher.group(1));
            int lastNumber = Integer.parseInt(matcher.group(2));
            return new int[] { firstNumber, lastNumber };
        } else {
            throw new IllegalArgumentException("Format not matched");
        }
    }

    public void addToWaitingQueue(Packet packet) {
        this.waitingQueue.add(packet);
        synchronized (lockObject) {
            lockObject.notifyAll(); // Notify all waiting threads
        }

    }

    public Packet removeFromWaitingQueue() {
        logger.info("Removing packet from waiting queue");
        try {
            logger.info("Waiting queue size: " + this.waitingQueue.size());
            return this.waitingQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addToPacketQueue(Packet packet) {
        this.packetQueue.add(packet);
        synchronized (lockObject) {
            lockObject.notifyAll(); // Notify all waiting threads
        }

    }

    public Packet removeFromPacketQueue() {
        try {
            return this.packetQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
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

    public synchronized void updateSequenceNumber(int sequenceNumber) {
        lastSequenceNumber = sequenceNumber;
    }

    public synchronized Integer getLastSequenceNumber() {
        return lastSequenceNumber;
    }

    public void setReplicaChannel(String key, Channel value) {
        this.replicaChannels.put(key, value);
    }

    public Channel getReplicaChannel(String key) {
        return this.replicaChannels.get(key);
    }

}
