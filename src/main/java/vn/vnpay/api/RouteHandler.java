package vn.vnpay.api;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

@FunctionalInterface
public interface RouteHandler {
    void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception;
}
