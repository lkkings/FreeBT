package com.lkd.bt.spider.task;

import cn.hutool.core.util.ArrayUtil;
import com.lkd.bt.spider.config.Config;
import com.lkd.bt.spider.entity.Node;
import com.lkd.bt.spider.filter.InfoHashFilter;
import com.lkd.bt.spider.service.impl.NodeServiceImpl;
import com.lkd.bt.spider.socket.Sender;
import com.lkd.bt.spider.socket.UDPServerFactory;
import com.lkd.bt.spider.socket.handler.UDPServerHandler;
import com.lkd.bt.spider.util.BTUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
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
    private final List<String> nodeIds;
    private final ReentrantLock lock;
    private final Condition condition;
    private final Sender sender;

    private final UDPServerFactory udpServerFactory;

    private final NodeServiceImpl nodeService;

    private final InfoHashFilter filter;

    private final List<UDPServerHandler> udpServerHandlers;


    /**
     * 发送队列
     */
    private final BlockingDeque<InetSocketAddress> queue;

    public FindNodeTask(Config config, Sender sender,
                        UDPServerFactory udpServerFactory, List<UDPServerHandler> udpServerHandlers, NodeServiceImpl nodeService, InfoHashFilter filter) {
        this.udpServerFactory = udpServerFactory;
        this.nodeService = nodeService;
        this.filter = filter;
        this.config = config;
        this.nodeIds = config.getMain().getNodeIds();
        this.sender = sender;
        this.queue = new LinkedBlockingDeque<>(config.getPerformance().getFindNodeTaskMaxQueueLength());
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
        this.udpServerHandlers = udpServerHandlers;
        this.name = "FindNodeTask";
    }



    /**
     * 入队首
     */
    public void put(InetSocketAddress address) {
        // 如果插入失败
        if(!queue.offerFirst(address)){
            //从末尾移除一个
            log.info(LOG+"-队列已满");
            queue.pollLast();
        }
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
     * 初始化发送任务
     * 向yml中的节点发送请求
     */
    private void joinDHT() {
        //获取初始化发送地址
        InetSocketAddress[] initAddresses = getInitAddresses();
        List<String> nodeIds = config.getMain().getNodeIds();
        for (int i = 0; i < nodeIds.size(); i++) {
            String nodeId = nodeIds.get(i);
            //向每个地址发送请求
            for (InetSocketAddress address : initAddresses) {
                this.sender.findNode(address,nodeId, BTUtil.generateNodeIdString(),i);
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
        //等待连接成功,获取到发送用的channel,再进行下一步
        Thread.sleep(5000);
        //加入DHT网络
        joinDHT();
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
        int size = nodeIds.size();
        TimeUnit milliseconds = TimeUnit.MILLISECONDS;
        //开启多个线程，每个线程负责
        for (int i = 0; i < config.getPerformance().getFindNodeTaskThreadNum(); i++) {
            new Thread(()->{
                int j;
                while (true) {
                    try {
                        //轮询使用每个端口向外发送请求
                        for (j = 0; j < size; j++) {
                            sender.findNode(queue.take(),nodeIds.get(j), BTUtil.generateNodeIdString(),j);
                            pause(lock, condition, pauseTime, milliseconds);
                        }
                    }
                    catch (Exception e) {
                        log.error("[FindNodeTask]异常.error:{}",e.getMessage());
                        if (queue.isEmpty()){
                            log.info("{}节点队列为空,准备重新加入DHT网络",LOG);
                            pause(lock, condition,3,TimeUnit.SECONDS);
                            joinDHT();
                        }
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
                log.info("{}DHT服务关闭",LOG);
            } catch (Exception e) {
                log.error("{},端口:{},发生未知异常,准备重新启动.异常:{}", LOG, port, e.getMessage(), e);
            }
            finally {
                udpServerFactory.destroy();
            }
        }
    }


    /**
     * 长度
     */
    public int size() {
        return queue.size();
    }

}
