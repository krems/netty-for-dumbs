package ru.mipt.dpqe.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.function.Consumer;

class ClientHandler extends ChannelInboundHandlerAdapter {
    private final ByteBuf firstMessage;
    private final Consumer<String> consumer;

    ClientHandler(Consumer<String> consumer) {
        this.consumer = consumer;
        firstMessage = Unpooled.buffer(8192);
        for (int i = 0; i < 8; i ++) {
            firstMessage.writeByte(0);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(firstMessage);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        consumer.accept((String) msg);
    }
}
