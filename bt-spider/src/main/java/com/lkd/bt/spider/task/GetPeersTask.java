package com.lkd.bt.spider.task;

import com.lkd.bt.common.exception.BTException;
import com.lkd.bt.common.util.CodeUtil;
import com.lkd.bt.spider.config.Config;
import com.lkd.bt.spider.constant.RedisConstant;
import com.lkd.bt.spider.dto.GetPeersSendInfo;
import com.lkd.bt.spider.dto.Message;
import com.lkd.bt.spider.entity.Node;
import com.lkd.bt.spider.filter.InfoHashFilter;
import com.lkd.bt.spider.service.impl.RedisServiceImpl;
import com.lkd.bt.spider.socket.RoutingTable;
import com.lkd.bt.spider.socket.Sender;
import com.lkd.bt.spider.socket.processer.Process;
import com.lkd.bt.spider.socket.processer.UDPProcessorManager;
import com.lkd.bt.spider.util.BTUtil;
import com.lkd.bt.spider.util.Bencode;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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

	private final RedisServiceImpl redisService;

	private final BlockingQueue<String> queue;

	public GetPeersTask(RedisServiceImpl redisService, List<RoutingTable> routingTables,
						Config config, Sender sender, FetchMetadataTask fetchMetadataByPeerTask, InfoHashFilter filter) {
		this.redisService = redisService;
		this.routingTables = routingTables;
		this.config = config;
		this.queue = new LinkedBlockingQueue<>(config.getPerformance().getGetPeersTaskInfoHashQueueLen());
		this.sender = sender;
		this.fetchMetadataByPeerTask = fetchMetadataByPeerTask;
		this.filter = filter;
		this.lock = new ReentrantLock();
		this.condition = this.lock.newCondition();
		this.name = "GetPeersTask";
	}

	/**
	 * 入队并做双重过滤去重处理
	 */
	public void put(String infoHashHexStr) {
		if (filter.put(infoHashHexStr) && redisService.add(RedisConstant.INFO_HASH,infoHashHexStr)) {
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
					if (redisService.size(RedisConstant.GET_PEER_SEND_INFO) > config.getPerformance().getGetPeersTaskConcurrentNum()) {
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
		redisService.put(RedisConstant.GET_PEER_SEND_INFO,messageId, new GetPeersSendInfo(infoHashHexStr).put(nodeIdList));
		//存入任务队列
		fetchMetadataByPeerTask.put(infoHashHexStr,getStartTime());
	}


	/**
	 * 根据缓存过期时间,计算fetchMetadataByPeerTask任务开始时间
	 */
	public long getStartTime() {
		return System.currentTimeMillis() + config.getPerformance().getGetPeersTaskExpireSecond() * 1000;
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
