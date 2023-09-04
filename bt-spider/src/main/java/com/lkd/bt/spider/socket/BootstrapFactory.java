package com.lkd.bt.spider.socket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by lkkings on 2023/8/26
 */

public abstract class BootstrapFactory {
    Bootstrap bootstrap;


    NioEventLoopGroup eventLoopGroup;



    public Bootstrap build(){
        return bootstrap.clone();
    }

    public void destroy(){
        if (eventLoopGroup != null){
            eventLoopGroup.shutdownGracefully();
        }
    }

}
