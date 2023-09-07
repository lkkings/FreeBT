package com.lkd.bt.spider.task;

import cn.hutool.core.util.ArrayUtil;
import com.lkd.bt.common.exception.BTException;
import com.lkd.bt.spider.config.Config;
import com.lkd.bt.spider.constant.RedisConstant;
import com.lkd.bt.spider.dto.Message;
import com.lkd.bt.spider.entity.Node;
import com.lkd.bt.spider.filter.InfoHashFilter;
import com.lkd.bt.spider.service.impl.NodeServiceImpl;
import com.lkd.bt.spider.socket.Sender;
import com.lkd.bt.spider.socket.UDPServerFactory;
import com.lkd.bt.spider.socket.core.Process;
import com.lkd.bt.spider.socket.core.UDPProcessor;
import com.lkd.bt.spider.socket.core.UDPProcessorManager;
import com.lkd.bt.spider.util.BTUtil;
import com.lkd.bt.spider.util.Bencode;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RQueue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by lkkings on 2023/8/26
 * 发送find_node请求，查找目标主机
 */
@Order(3)
@Component
@Slf4j
public class FindNodeTask extends Task implements Pauseable {
    private static final String LOG = "[FindNodeTask]";
    private final Config config;
    private final ReentrantLock lock;
    private final Condition condition;
    private final Sender sender;

    private final UDPServerFactory udpServerFactory;

    private final NodeServiceImpl nodeService;

    private final InfoHashFilter filter;

    private final List<UDPServerHandler> udpServerHandlers;

    private final Bencode bencode;

    private final List<UDPProcessor> udpProcessors;

    private final RBlockingQueue<Node> queue;



    public FindNodeTask(Config config, Sender sender,@Qualifier(RedisConstant.FIND_NODE_TASK_QUEUE) RBlockingQueue<Node> queue,
                        UDPServerFactory udpServerFactory,NodeServiceImpl nodeService, InfoHashFilter filter, Bencode bencode, List<UDPProcessor> udpProcessors) {
        this.udpServerFactory = udpServerFactory;
        this.nodeService = nodeService;
        this.filter = filter;
        this.config = config;
        this.sender = sender;
        this.bencode = bencode;
        this.udpProcessors = udpProcessors;
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
        this.queue = queue;
        this.udpServerHandlers = udpServerHandlers();
        this.name = "FindNodeTask";
    }

    /**
     * DHT服务端处理器
     */
    public List<UDPServerHandler> udpServerHandlers(){
        int size = config.getMain().getNodeIds().size();
        List<UDPServerHandler> udpServerHandlers = new ArrayList<>(size);
        UDPProcessorManager udpProcessorManager = new UDPProcessorManager();
        udpProcessors.forEach(udpProcessorManager::register);
        for (int i = 0; i < size; i++) {
            udpServerHandlers.add(new UDPServerHandler(i, udpProcessorManager));
        }
        return udpServerHandlers;
    }


    /**
     * 获取初始化发送地址集合
     */
    private InetSocketAddress[] getInitAddresses() {
        // 从数据库中查询地址
        Integer initTaskSendNum = config.getMain().getInitTaskSendNum();
        //获取配置文件中的初始化地址
        InetSocketAddress[] initAddressArray = config.getMain().getInitAddressArray();
        List<Node> nodeList = nodeService.getBaseMapper().findTopNode(initTaskSendNum);
        initAddressArray = ArrayUtil.addAll(initAddressArray, nodeList.stream().map(Node::toAddress).toArray(InetSocketAddress[]::new));
        return initAddressArray;
    }

    /**
     * 当节点队列为空时，重新加入DHT网络
     */
    @SneakyThrows
    @Scheduled(fixedRate = 10,initialDelay = 5,timeUnit=TimeUnit.SECONDS)
    private void joinDHT() {
        if (queue.isEmpty()) {
            //获取初始化发送地址
            InetSocketAddress[] initAddresses = getInitAddresses();
            List<String> nodeIds = config.getMain().getNodeIds();
            for (int i = 0; i < nodeIds.size(); i++) {
                //向每个地址发送请求
                for (InetSocketAddress address : initAddresses) {
                    this.sender.findNode(address,BTUtil.generateNodeIdString(),i);
                }
            }
        }
    }

    /**
     * 循环执行该任务
     */
    @SneakyThrows
    protected void start() {
        //启动过滤器
        filter.enable();
        //启动DHT服务
        runDHTServer();
        //发送find_node请求
        startSendFindNode();
    }
    private void runDHTServer(){
        List<Integer> ports = config.getMain().getPorts();
        for (int i = 0; i < ports.size(); i++) {
            final int index = i;
            new Thread(() -> run(ports.get(index), index)).start();
        }
    }

    private void startSendFindNode(){
        int pauseTime = config.getPerformance().getFindNodeTaskIntervalMS();
        List<String> nodeIds = config.getMain().getNodeIds();
        TimeUnit milliseconds = TimeUnit.MILLISECONDS;
        //开启多个线程，每个线程负责轮询使用每个端口向外发送请求
        for (int i = 0; i < config.getPerformance().getFindNodeTaskThreadNum(); i++) {
            new Thread(()->{
                int j;
                while (true) {
                    try {
                        //轮询使用每个端口向外发送请求
                        for (j = 0; j <  nodeIds.size(); j++) {
                            Node node = queue.take();
                            log.info("{}发现节点{}",LOG,node);
                            sender.findNode(node.toAddress(), node.getNodeId(), BTUtil.generateNodeIdString(),j);
                            pause(lock, condition, pauseTime, milliseconds);
                        }
                    }
                    catch (Exception e) {
                        log.error("[FindNodeTask]异常.error:{}",e.getMessage());
                    }
                }
            }).start();
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
            } catch (Exception e) {
                log.error("{},端口:{},发生未知异常,准备重新启动.异常:{}", LOG, port, e.getMessage(), e);
            }
            finally {
                udpServerFactory.destroy();
                log.info("{}DHT服务关闭",LOG);
            }
        }
    }


    /**
     * 长度
     */
    public int size() {
        return queue.size();
    }

    @ChannelHandler.Sharable
    class UDPServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        private static final String LOG = "[DHT服务端处理类]-";

        //当前处理器针对的nodeId索引
        private final int index;

        private final UDPProcessorManager udpProcessorManager;


        public UDPServerHandler(int index, UDPProcessorManager udpProcessorManager) {
            this.index = index;
            this.udpProcessorManager = udpProcessorManager;
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
            log.info("{}请求方法{}",LOG,message.getMethod().getMessage());
            udpProcessorManager.process(new Process(message, map, sender, this.index));
        }


        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            //给发送器工具类的channel赋值
            sender.setChannel(ctx.channel(), this.index);
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
