package com.lkd.bt.spider.socket;

import com.lkd.bt.spider.config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created by lkkings on 2023/8/26
 * tcp 服务工厂
 */

@Component
public class UDPServerFactory extends BootstrapFactory{

    public UDPServerFactory(Config config) {
        //创建线程组 - 手动设置线程数,默认为cpu核心数2倍
        NioEventLoopGroup eventLoopGroup =  new NioEventLoopGroup(config.getPerformance().getUdpServerMainThreadNum());
        this.eventLoopGroup = eventLoopGroup;
        this.bootstrap = new Bootstrap().group(eventLoopGroup)
                //通道类型UDP
                .channel(NioDatagramChannel.class)
                //广播即UDP连接
                .option(ChannelOption.SO_BROADCAST, true)
                //UDP读缓冲区
                .option(ChannelOption.SO_RCVBUF, 10000 * 1024)
                //UDP写缓冲区
                .option(ChannelOption.SO_SNDBUF, 10000 * 1024);
    }
}
