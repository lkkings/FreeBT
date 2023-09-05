package com.lkd.bt.spider.socket.handler;

import com.lkd.bt.common.exception.BTException;
import com.lkd.bt.spider.dto.Message;
import com.lkd.bt.spider.socket.Sender;
import com.lkd.bt.spider.socket.core.UDPProcessorManager;
import com.lkd.bt.spider.util.BTUtil;
import com.lkd.bt.spider.socket.core.Process;
import com.lkd.bt.spider.util.Bencode;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;


@ChannelHandler.Sharable
@Slf4j
public class UDPServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final String LOG = "[DHT服务端处理类]-";

    //当前处理器针对的nodeId索引
    private final int index;

    private final Bencode bencode;
    private final UDPProcessorManager udpProcessorManager;
    private final Sender sender;


    public UDPServerHandler(int index, Bencode bencode, UDPProcessorManager udpProcessorManager,
                            Sender sender) {
        this.index = index;
        this.bencode = bencode;
        this.udpProcessorManager = udpProcessorManager;
        this.sender = sender;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket packet) throws Exception {
        byte[] bytes = getBytes(packet);
        InetSocketAddress sender = packet.sender();
        //解码为map
        Map<String, Object> map;
        try {
            map = bencode.decode(bytes, Map.class);
        } catch (BTException e) {
            log.info("{}消息解码异常.发送者:{}.异常:{}", LOG, sender, e.getMessage());
            return;
        } catch (Exception e) {
            log.info("{}消息解码异常.发送者:{}.异常:{}", LOG, sender, e.getMessage(), e);
            return;
        }
        //解析出Message
        Message message;
        try {
            message = BTUtil.getMessage(map);
        } catch (BTException e) {
            log.info("{}解析MessageInfo异常.异常:{}", LOG, e.getMessage());
            return;
        } catch (Exception e) {
            log.info("{}解析MessageInfo异常.异常:{}", LOG, e.getMessage(), e);
            return;
        }
        udpProcessorManager.process(new Process(message, map, sender, this.index));
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //给发送器工具类的channel赋值
        this.sender.setChannel(ctx.channel(), this.index);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
    }

    /**
     * ByteBuf -> byte[]
     */
    private byte[] getBytes(DatagramPacket packet) {
        //读取消息到byte[]
        byte[] bytes = new byte[packet.content().readableBytes()];
        packet.content().readBytes(bytes);
        return bytes;
    }

    /**
     * 异常捕获
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("{}索引:{},发生异常:{}", LOG, index, cause.getMessage());
    }
}