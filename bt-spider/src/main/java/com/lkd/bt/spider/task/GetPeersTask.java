package com.lkd.bt.spider.task;

import com.lkd.bt.common.util.CodeUtil;
import com.lkd.bt.spider.config.Config;
import com.lkd.bt.spider.constant.RedisConstant;
import com.lkd.bt.spider.dto.GetPeersSendInfo;
import com.lkd.bt.spider.entity.Node;
import com.lkd.bt.spider.filter.InfoHashFilter;
import com.lkd.bt.spider.socket.RoutingTable;
import com.lkd.bt.spider.socket.Sender;
import com.lkd.bt.spider.util.BTUtil;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RHyperLogLog;
import org.redisson.api.RMapCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Created by lkkings on 2023/8/24
 * 发送get_peers请求,以获取目标主机
 */
@Order(0)
@Component
@Slf4j
public class GetPeersTask extends Task implements Pauseable {
	private static final String LOG = "[GetPeersTask]";

	private final List<RoutingTable> routingTables;
	private final Config config;
	private final ReentrantLock lock;
	private final Condition condition;
	private final Sender sender;
	private final FetchMetadataTask fetchMetadataByPeerTask;

	private final InfoHashFilter filter;

	private final RBlockingQueue<String> queue;

	private final RHyperLogLog<String> rFilter;

	private final RMapCache<String, GetPeersSendInfo> rMapCache;

	public GetPeersTask(List<RoutingTable> routingTables,@Qualifier(RedisConstant.GET_PEERS_TASK_QUEUE) RBlockingQueue<String> queue,
						Config config, Sender sender, FetchMetadataTask fetchMetadataByPeerTask, InfoHashFilter filter,@Qualifier(RedisConstant.INFO_HASH_FILTER) RHyperLogLog<String> rFilter, @Qualifier(RedisConstant.GET_PEER_SEND_INFO) RMapCache<String, GetPeersSendInfo> rMapCache) {
		this.routingTables = routingTables;
		this.config = config;
		this.queue = queue;
		this.sender = sender;
		this.fetchMetadataByPeerTask = fetchMetadataByPeerTask;
		this.filter = filter;
		this.rFilter = rFilter;
		this.rMapCache = rMapCache;
		this.lock = new ReentrantLock();
		this.condition = this.lock.newCondition();
		this.name = "GetPeersTask";
	}

	/**
	 * 入队并做双重过滤去重处理
	 */
	public void put(String infoHashHexStr) {
		if(infoHashHexStr.length()== 40){
			log.error("{}info_hash length error -{}",LOG,infoHashHexStr);
			return;
		}
		if (filter.put(infoHashHexStr) && rFilter.add(infoHashHexStr)) {
			queue.offer(infoHashHexStr);
		}
	}


	/**
	 * 删除某个任务
	 */
	public void remove(String infoHashHexStr) {
		queue.remove(infoHashHexStr);
	}

	/**
	 * 长度
	 */
	public int size() {
		return queue.size();
	}

	/**
	 * 任务线程
	 */
	protected void start() {
		final int getPeersTaskCreateIntervalMillisecond = config.getPerformance().getGetPeersTaskCreateIntervalMs();
		final int getPeersTaskPauseSecond = config.getPerformance().getGetPeersTaskPauseSecond();
		new Thread(() -> {
			while (true) {
				try {
					//如果当前查找任务过多. 暂停30s再继续
					if (rMapCache.size() > config.getPerformance().getGetPeersTaskConcurrentNum()) {
						log.info("{}当前任务数过多,暂停获取新任务线程{}s.", LOG, getPeersTaskPauseSecond);
						pause(lock, condition, getPeersTaskPauseSecond, TimeUnit.SECONDS);
						continue;
					}
					//开启新任务
					run(queue.take());
					//开始一个任务后,暂停
					pause(lock, condition, getPeersTaskCreateIntervalMillisecond, TimeUnit.MILLISECONDS);
				} catch (Exception e) {
					log.error("{}异常.e:{}", LOG, e.getMessage(), e);
				}
			}
		}).start();
	}


	/**
	 * 开始任务
	 */
	private void run(String infoHashHexStr) {
		//消息id
		String messageId = BTUtil.generateMessageIDOfGetPeers();
		log.info("{}开始新任务.消息Id:{},infoHash:{}", LOG, messageId, infoHashHexStr);

		//当前已发送节点id
		List<byte[]> nodeIdList = new ArrayList<>();
		for (int i = 0; i < routingTables.size(); i++) {
			//获取最近的8个地址
			List<Node> nodeList = routingTables.get(i).getForTop8(CodeUtil.hexStr2Bytes(infoHashHexStr));
			//目标nodeId
			nodeIdList.addAll(nodeList.stream().map(Node::getNodeIdBytes).toList());
			//目标地址
			List<InetSocketAddress> addresses = nodeList.stream().map(Node::toAddress).collect(Collectors.toList());
			//批量发送
			this.sender.getPeersBatch(addresses, config.getMain().getNodeIds().get(i), new String(CodeUtil.hexStr2Bytes(infoHashHexStr), CharsetUtil.ISO_8859_1), messageId, i);
		}
		//存入缓存
		rMapCache.put(messageId, new GetPeersSendInfo(infoHashHexStr).put(nodeIdList));
		//存入任务队列
		fetchMetadataByPeerTask.put(infoHashHexStr,getStartTime());
	}


	/**
	 * 根据缓存过期时间,计算fetchMetadataByPeerTask任务开始时间
	 */
	public long getStartTime() {
		return System.currentTimeMillis() + config.getPerformance().getGetPeersTaskExpireSecond() * 1000;
	}
}
