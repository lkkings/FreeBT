package com.lkd.bt.spider.task;

import com.lkd.bt.common.exception.BTException;
import com.lkd.bt.spider.config.Config;
import com.lkd.bt.spider.dto.Message;
import com.lkd.bt.spider.entity.Node;
import com.lkd.bt.spider.filter.InfoHashFilter;
import com.lkd.bt.spider.service.impl.NodeServiceImpl;
import com.lkd.bt.spider.socket.Sender;
import com.lkd.bt.spider.socket.UDPServerFactory;
import com.lkd.bt.spider.socket.processer.Process;
import com.lkd.bt.spider.socket.processer.UDPProcessorManager;
import com.lkd.bt.spider.util.BTUtil;
import com.lkd.bt.spider.util.Bencode;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by lkkings on 2023/8/24
 * 初始化任务
 */
@Order(2)
@Slf4j
@Component
public class InitTask extends Task{
    private static final String LOG = "[Init Task]-";
    private final Config config;
    private final Sender sender;
    private final UDPServerFactory udpServerFactory;

    private final NodeServiceImpl nodeService;

    private final InfoHashFilter filter;

    private final List<UDPServerHandler> udpServerHandlers;

    private final Bencode bencode;

    private final UDPProcessorManager udpProcessorManager;


    public InitTask(Config config, Sender sender, UDPServerFactory udpServerFactory, NodeServiceImpl nodeService, InfoHashFilter filter, Bencode bencode, UDPProcessorManager udpProcessorManager) {
        this.config = config;
        this.sender = sender;
        this.udpServerFactory = udpServerFactory;
        this.nodeService = nodeService;
        this.filter = filter;
        this.bencode = bencode;
        this.udpProcessorManager = udpProcessorManager;
        this.udpServerHandlers = getInitUdpServerHandlers();
        this.name = "InitTask";
    }
    /**
     * DHT服务端处理器
     */
    private List<UDPServerHandler> getInitUdpServerHandlers(){
        int size = config.getMain().getNodeIds().size();
        List<UDPServerHandler> udpServerHandlers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            udpServerHandlers.add(new UDPServerHandler(i, bencode, udpProcessorManager, sender));
        }
        return udpServerHandlers;
    }

    /**
     * 获取初始化发送地址集合
     */
    private InetSocketAddress[] getInitAddresses() {
        // 从数据库中查询地址
        Integer initTaskSendNum = config.getMain().getInitTaskSendNum();
        List<Node> nodeList = nodeService.getBaseMapper().findTopNode(initTaskSendNum);
        //获取配置文件中的初始化地址
        return config.getMain().getInitAddressArray();
    }

    /**
     * 初始化发送任务
     * 向yml中的节点发送请求
     */
    private void initSend(InetSocketAddress[] initAddressArray) {
        List<String> nodeIds = config.getMain().getNodeIds();
        for (int i = 0; i < nodeIds.size(); i++) {
            String nodeId = nodeIds.get(i);
            //向每个地址发送请求
            for (InetSocketAddress address : initAddressArray) {
                this.sender.findNode(address,nodeId, BTUtil.generateNodeIdString(),i);
            }
        }
    }

    @Override
    @SneakyThrows
    protected void start() {
        filter.enable();
        //获取初始化发送地址
        InetSocketAddress[] initAddresses = getInitAddresses();
        //启动DHT服务
        runDHTServer();
        //等待连接成功,获取到发送用的channel,再进行下一步
        Thread.sleep(5000);
        initSend(initAddresses);
    }

    private void runDHTServer(){
        List<Integer> ports = config.getMain().getPorts();
        for (int i = 0; i < ports.size(); i++) {
            final int index = i;
            new Thread(() -> run(ports.get(index), index)).start();
        }
    }

    /**
     * 保证UDP服务端开启,即使运行出错
     */
    private void run(int port, int index) {
        while (true) {
            try {
                log.info("{}DHT服务端启动...当前端口:{}",LOG, port);
                udpServerFactory.build()
                        .handler(udpServerHandlers.get(index))
                        .bind(port).sync()
                        .channel()
                        .closeFuture()
                        .await();
                log.info("{}DHT服务关闭",LOG);
            } catch (Exception e) {
                log.error("{},端口:{},发生未知异常,准备重新启动.异常:{}", LOG, port, e.getMessage(), e);
            }
            finally {
                udpServerFactory.destroy();
            }
        }
    }

    @ChannelHandler.Sharable
    private static class UDPServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        private static final String LOG = "[DHT服务端处理类]-";

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
}
