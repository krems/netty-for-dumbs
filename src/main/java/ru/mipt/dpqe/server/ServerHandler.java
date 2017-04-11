package ru.mipt.dpqe.server;

import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.function.Consumer;

@ChannelHandler.Sharable
class ServerHandler extends SimpleChannelInboundHandler<String> {
    private static final String SEPARATOR = System.lineSeparator();
    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final Consumer<String> listener;

    ServerHandler(Consumer<String> listener) {
        this.listener = listener;
    }

    void broadcast(String msg) {
        String response = msg + SEPARATOR;
        for (Channel channel : channels) {
            channel.writeAndFlush(response);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channels.add(ctx.channel());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {
        if (!request.isEmpty()) {
            listener.accept(request);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    void stop() {
        Channel last = null;
        for (Channel channel : channels) {
            last = channel;
            channel.close();
        }
        if (last != null) {
            last.parent().close();
        }
    }
}
