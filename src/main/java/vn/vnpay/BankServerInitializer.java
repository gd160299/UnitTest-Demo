package vn.vnpay;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import vn.vnpay.api.BankHandler;
import vn.vnpay.service.AccountService;

public class BankServerInitializer extends ChannelInitializer<SocketChannel> {

    private final AccountService accountService;

    public BankServerInitializer(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(1048576));
        pipeline.addLast(new BankHandler(this.accountService));
    }
}
