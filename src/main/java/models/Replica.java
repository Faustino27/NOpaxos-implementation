package models;

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

    public void addClientConnection(Short clientId, ChannelHandlerContext ctx) {
        clientConnections.put(clientId, ctx);
    }

    public ChannelHandlerContext getClientConnection(Short clientId) {
        return clientConnections.get(clientId);
    }

    public void removeClientConnection(Short clientId) {
        clientConnections.remove(clientId);
    }


    public Replica(int port) {
        this.port = port;
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
