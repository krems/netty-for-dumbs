package ru.mipt.dpqe.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ClientCommunicator {
    private static final String SEPARATOR = System.lineSeparator();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            }
    );
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final boolean autoReconnect;
    private final Consumer<String> consumer;
    private final String host;
    private final int port;
    private volatile EventLoopGroup workerGroup;
    private volatile Channel channel;
    private volatile Future<?> future;

    public ClientCommunicator(String host, int port, Consumer<String> consumer) {
        this(host, port, consumer, true);
    }

    public ClientCommunicator(String host, int port, Consumer<String> consumer, boolean autoReconnect) {
        this.host = host;
        this.port = port;
        this.consumer = consumer;
        this.autoReconnect = autoReconnect;
    }

    public void send(String msg) {
        Channel channel = this.channel;
        if (channel != null) {
            channel.writeAndFlush(msg + SEPARATOR);
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
        } catch (InterruptedException e) {
            finish();
        } catch (Throwable e) {
            if (Thread.currentThread().isInterrupted()) {
                finish();
            } else if (autoReconnect) {
                restart();
            }
        }
    }

    private void doFire() throws InterruptedException {
        workerGroup = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new ClientInitializer(new ClientHandler(consumer)));

        channel = b.connect(host, port).sync().channel();
        channel.closeFuture().sync();
        if (autoReconnect) {
            restart();
        }
    }

    private void finish() {
        stop();
        cleanup();
    }

    private void restart() {
        finish();
        start();
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        Future<?> future = this.future;
        if (future != null) {
            future.cancel(true);
            this.future = null;
        }
    }

    private void cleanup() {
        Channel channel = this.channel;
        if (channel != null) {
            channel.close();
            this.channel = null;
        }
        EventLoopGroup workerGroup = this.workerGroup;
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            this.workerGroup = null;
        }
    }
}
