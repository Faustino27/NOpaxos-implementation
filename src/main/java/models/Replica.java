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
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
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
    private int replicaId;



    private final Logger logger = Logger.getLogger(Replica.class.getName());

    private final ConcurrentHashMap<Short, ChannelHandlerContext> clientConnections = new ConcurrentHashMap<>();
    private Integer lastSequenceNumber = 0;
    private Integer expectedPacketToProcess = 0;

    private Map<String, Channel> replicaChannels = new HashMap<>();
    private LinkedHashSet<Packet> recentPackets = new LinkedHashSet<>();
    private BlockingQueue<Packet> packetQueue = new LinkedBlockingQueue<>();
    private EventLoopGroup group = new NioEventLoopGroup(1);
    private int maxRecentPackets = 10000;
    private Properties properties;
    private final Object lockObject = new Object(); // This lock object is shared across threads
    AtomicInteger processados = new AtomicInteger(0);

    private final ConcurrentSkipListMap<Integer, Packet> packetMap = new ConcurrentSkipListMap<>();
    public Replica(int myPort) {
        this.myPort = myPort;
        replicaId = myPort - 9000;

        properties = new Properties();
        try {
            FileInputStream fis = new FileInputStream("config.properties");
            properties.load(fis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void addToRecentPacketSet(Packet packet) {
    if (recentPackets.size() >= maxRecentPackets) {
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
            //logger.info("Connecting to replica on port: " + port);
            final int targetPort = port;
            int replicaNumber = port - 9000;
            String ip = properties.getProperty("replica" + replicaNumber + ".ip");
            String mapKey = ip + ":" + port;

            Bootstrap bootstrap = getBootstrap();
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

    private Bootstrap getBootstrap() {
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
        return bootstrap;
    }

    private void sendHandShakeReplica(String mapKey) {
        String message = "First Mensage";
        Header header = new Header((short) 1, (short) 2);
        // 2 = hand shake replica to replica
        Packet packet = new Packet(header, message);
        packet.setData(message);
        Channel replicaChannel = replicaChannels.get(mapKey);
        if (replicaChannel != null && replicaChannel.isActive()) {
            logger.fine("Seding Hand Shake to replica " + mapKey);
            replicaChannel.writeAndFlush(Arrays.asList(packet)); 
        } else {
            logger.warning("Replica Channel is not active. Cannot send packet.");
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]); // Pass the port number as a command-line argument
        Replica replica = new Replica(port);
        new Thread(replica::processPacketLoop).start();
        replica.start();

    }

    public List<Packet> processMissingPacketsRequest(Packet packet) {
        List<Packet> packets = new ArrayList<>();
        int[] numbers = getNumbers(packet.getData());
        logger.info("Organizing missing packets between " + numbers[0] + " and " + numbers[1]);
        for (Packet p : recentPackets) {
            if (numbers[1] <= p.getSequenceNumber()) {
                break;
            }
            if (numbers[0] <= p.getSequenceNumber()) {
                logger.info("Adding packet {"+ p.getSequenceNumber() +"} to missing packets list. First packet = "
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
        // synchronized (lockObject) {
        //     if ((packetQueue.isEmpty() && packetMap.isEmpty())) {
        //         try {
        //             lockObject.wait();
        //         } catch (InterruptedException e) {
        //             Thread.currentThread().interrupt(); // Handle interrupted exception
        //             return;
        //         }
        //     }
        // }

        if (!packetQueue.isEmpty()
                && packetQueue.peek().getSequenceNumber() == expectedPacketToProcess) {
            //logger.info("Processing first packet in RECENT queue: " + packetQueue.peek().getSequenceNumber());
            processPacket(removeFromPacketQueue());
            expectedPacketToProcess++;
        } 
        if (!packetMap.isEmpty() && packetMap.containsKey(expectedPacketToProcess)) {
            //logger.info("Processing first packet in WAITING queue: " + expectedPacketToProcess);
            processPacket(packetMap.get(expectedPacketToProcess));
            packetMap.remove(expectedPacketToProcess);
            expectedPacketToProcess++;
            lastSequenceNumber++;
        }
    }

    private void processPacket(Packet packet) {
        processados.incrementAndGet();
        Short clientKey = packet.getSenderId();
        ChannelHandlerContext clientCtx = getClientConnection(clientKey);
        Header header = new Header((short)replicaId);
        header.setSequenceNumber(packet.getSequenceNumber());
        Packet responsePacket = new Packet(header,
                packet.getData());
        //logger.info("Preparing to send response to client: " + packet.getHeader().getSenderId());
        if (clientCtx != null && clientCtx.channel().isActive()) {
            clientCtx.writeAndFlush(responsePacket).addListener(future -> {
                if (future.isSuccess()) {
                    //logger.info("Response sent to client: " + packet.getSenderId());
                } else {
                    //logger.warning("Failed to send response to client: " + packet.getSenderId());
                }
            });
        } else {
            logger.warning("No active context found for client: " + packet.getSenderId());
        }

    }

    private int[] getNumbers(String str) {
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

    public void addToPacketMap(Packet packet) {
        //logger.info("Adding packet to waiting queue: " + packet);
        packetMap.put(packet.getSequenceNumber(), packet);
        synchronized (lockObject) {
            lockObject.notifyAll(); // Notify all waiting threads
        }
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

    public synchronized void updateLastSequenceNumber(int sequenceNumber) {
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
    public int getReplicaId() {
        return replicaId;
    }

    public void resetProcessados() {
        processados.set(0);
    }
}
