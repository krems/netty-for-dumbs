package ru.mipt.dpqe.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


public class ServerCommunicator {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Consumer<String> listener;
    private final int port;
    private volatile EventLoopGroup bossGroup;
    private volatile EventLoopGroup workerGroup;
    private volatile ServerHandler serverHandler;
    private volatile Future<?> future;

    public ServerCommunicator(Consumer<String> listener, int port) {
        this.listener = listener;
        this.port = port;
    }

    public void broadcastMessage(String message) {
        ServerHandler handler = this.serverHandler;
        if (handler != null) {
            handler.broadcast(message);
        }
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        future = executor.submit(this::fire);
    }

    private void fire() {
        try {
            doFire();
        } catch (Throwable e) {
            stop();
        }
    }

    private void doFire() throws InterruptedException {
        workerGroup = new NioEventLoopGroup();
        bossGroup = new NioEventLoopGroup(1);
        ServerBootstrap b = new ServerBootstrap();
        serverHandler = new ServerHandler(listener);
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
//                    .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ServerInitializer(serverHandler));
        b.bind(port).sync().channel().closeFuture().sync();
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        ServerHandler handler = this.serverHandler;
        if (handler != null) {
            handler.stop();
            this.serverHandler = null;
        }
        Future<?> future = this.future;
        if (future != null) {
            future.cancel(true);
            this.future = null;
        }
        EventLoopGroup bossGroup = this.bossGroup;
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            this.bossGroup = null;
        }
        EventLoopGroup workerGroup = this.workerGroup;
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            this.workerGroup = null;
        }
    }
}
