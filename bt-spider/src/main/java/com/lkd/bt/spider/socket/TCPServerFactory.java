package com.lkd.bt.spider.socket;

import com.lkd.bt.spider.config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created by lkkings on 2023/8/26
 * tcp 服务工厂
 */
@Component
public class TCPServerFactory extends BootstrapFactory{

    public TCPServerFactory(Config config) {
        //创建线程组 - 手动设置线程数,默认为cpu核心数2倍
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(config.getPerformance().getTcpClientThreadNum());
        this.eventLoopGroup = eventLoopGroup;
        this.bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                //通道类型为TCP
                .channel(NioSocketChannel.class)
                //设置TCP连接超时时间
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getPerformance().getTcpConnectTimeoutMs())
                //设置TCP读缓存区
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(1, 102400, Integer.MAX_VALUE));

    }
}
