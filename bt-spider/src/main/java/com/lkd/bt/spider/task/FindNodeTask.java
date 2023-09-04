package com.lkd.bt.spider.task;

import com.lkd.bt.spider.config.Config;
import com.lkd.bt.spider.socket.Sender;
import com.lkd.bt.spider.util.BTUtil;
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


    /**
     * 发送队列
     */
    private final BlockingDeque<InetSocketAddress> queue;

    public FindNodeTask(Config config,Sender sender) {
        this.name = "FindNodeTask";
        this.config = config;
        this.nodeIds = config.getMain().getNodeIds();
        this.sender = sender;
        this.queue = new LinkedBlockingDeque<>(config.getPerformance().getFindNodeTaskMaxQueueLength());
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
    }

    /**
     * 入队首
     * announce_peer等
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
     * 循环执行该任务
     */
    protected void start() {
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
                    } catch (Exception e) {
                        log.error("[FindNodeTask]异常.error:{}",e.getMessage());
                    }
                }
            }).start();
        }
    }

    /**
     * 长度
     */
    public int size() {
        return queue.size();
    }
}
