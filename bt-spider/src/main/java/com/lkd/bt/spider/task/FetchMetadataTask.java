package com.lkd.bt.spider.task;

import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lkd.bt.common.entity.Metadata;
import com.lkd.bt.common.util.CodeUtil;
import com.lkd.bt.common.vo.MetadataVO;
import com.lkd.bt.spider.config.Config;
import com.lkd.bt.spider.entity.InfoHash;
import com.lkd.bt.spider.service.impl.InfoHashServiceImpl;
import com.lkd.bt.spider.service.impl.MetadataServiceImpl;
import com.lkd.bt.spider.socket.TCPServerFactory;
import com.lkd.bt.spider.util.BTUtil;
import com.lkd.bt.spider.util.Bencode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Created by lkkings on 2023/8/26
 * 根据bep-009/bep-010协议获取metadata信息任务
 */
@Order(1)
@Component
@Slf4j
public class FetchMetadataTask extends Task{
	private static final String LOG = "[FetchMetadataByPeerTask]";

	private final Config config;
	private final TCPServerFactory tcpServerFactory;
	private final Bencode bencode;
	private final InfoHashServiceImpl infoHashService;
	private final ObjectMapper objectMapper;
	private final MetadataServiceImpl metadataService;


	//等待连接peers的infoHash队列
	private final DelayQueue<DelayInfoHash> queue;

	public FetchMetadataTask(Config config, TCPServerFactory tcpServerFactory, Bencode bencode
							, InfoHashServiceImpl infoHashService, ObjectMapper objectMapper, MetadataServiceImpl metadataService) {
		this.config = config;
		this.tcpServerFactory = tcpServerFactory;
		this.bencode = bencode;
		this.infoHashService = infoHashService;
		this.objectMapper = objectMapper;
		this.metadataService = metadataService;
		this.queue = new DelayQueue<>();
	}


	/**
	 * 入队
	 */
	public void put(String infoHash, long startTime) {
		queue.offer(new DelayInfoHash(infoHash, startTime));
	}

	/**
	 * 入队
	 */
	public void put(String infoHash){
		put(infoHash,System.currentTimeMillis());
	}


	/**
	 * 删除某个任务
	 */
	public void remove(String infoHash) {
		queue.removeIf(item -> item.getInfoHash().equals(infoHash));
	}

	/**
	 * 队列长度
	 */
	public int size() {
		return queue.size();
	}

	/**
	 * 异步任务
	 */
	protected void start() {
		for (int i = 0; i < config.getPerformance().getFetchMetadataByPeerTaskTreadNum(); i++) {
			new Thread(() -> {
				while (true) {
					try {
						run();
					} catch (Exception e) {
						log.error("{}异常.e:{}",e.getMessage(), e);
					}
				}
			}).start();
		}
	}

	/**
	 * 单个任务
	 */
	@SneakyThrows
	private void run() {
		DelayInfoHash delayInfoHash = queue.take();
		try {
			log.info("{}开始新任务.infoHash:{}", LOG, delayInfoHash.getInfoHash());
			Metadata metadata = fetchMetadata(delayInfoHash.getInfoHash());
			if (metadata != null) {
				metadataService.save(metadata);
				log.info("{}成功.infoHash:{}", LOG, delayInfoHash.getInfoHash());
			}
		} finally {
			//无论成功失败与否,都删除infoHash表中该记录
			infoHashService.remove(new LambdaQueryWrapper<InfoHash>().eq(InfoHash::getInfoHash,delayInfoHash.getInfoHash()));
		}

	}

	/**
	 * 存入延时队列的数据
	 */
	@Data
	@AllArgsConstructor
	private static class DelayInfoHash implements Delayed {
		private String infoHash;
		private long startTime;

		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(startTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		}

		@Override
		public int compareTo(Delayed o) {
			return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
		}
	}

	/**
	 * 获取metadata
	 */
	@SneakyThrows
	private Metadata fetchMetadata(String infoHashHexStr) {
		CountDownLatch latch = new CountDownLatch(1);
		//从数据库中查询,是否有记录
		InfoHash infoHash = infoHashService.getOne(new LambdaQueryWrapper<InfoHash>().eq(InfoHash::getInfoHash,infoHashHexStr));
		if (infoHash == null)
			return null;
		Metadata metadata = null;
		//peer地址
		String[] addressArr = infoHash.getPeerAddress().split(";");

		//每次最多同时建立5个连接,如果peers数量超过5个,则分批获取
		int maxNum = 5;
		for (int i = 0; i < addressArr.length; i += maxNum) {
			List<Result> results = new LinkedList<>();
			//向5个peer执行发送任务
			for (int j = 0; j < 5; j++) {
				if (addressArr.length <= i + j) break;
				String[] ipPort = addressArr[i + j].split(":");
				final Result result = new Result(latch);
				results.add(result);
				tcpServerFactory.build().handler(new CustomChannelInitializer(infoHashHexStr, result))
						.connect(new InetSocketAddress(ipPort[0], Integer.parseInt(ipPort[1])))
						.addListener(new ConnectListener(infoHashHexStr, BTUtil.generateNodeId()));
			}
			//暂停10s 或 被唤醒
			latch.await(config.getPerformance().getFetchMetadataByPeerTaskReadTimeoutSecond(), TimeUnit.SECONDS);
			//尝试解析

			for (Result result : results) {
				if (result.getResult() != null) {
					metadata = bytes2Metadata(result.getResult(), infoHashHexStr);
					if (metadata != null) break;
				}
			}
			if (metadata != null) return metadata;
		}
		return null;
	}

	/**
	 * byte[] 转 {@link Metadata}
	 */
	@SuppressWarnings("unchecked")
	public Metadata bytes2Metadata(byte[] bytes, String infoHashHexStr) {
		try {
			String metadataStr = new String(bytes, CharsetUtil.UTF_8);
			String metadataBencodeStr = metadataStr.substring(0, metadataStr.indexOf("6:pieces")) + "e";
			Map<String, Object> rawMap = bencode.decode(metadataBencodeStr.getBytes(), Map.class);
			//名称
			String name = (String) rawMap.get("name");
			//总长度
			long length = 0;
			//文件
			List<MetadataVO.Info> infos;
			if (rawMap.containsKey("files")) {
				List<Map<String, Object>> files = (List<Map<String, Object>>) rawMap.get("files");
				//允许并行处理
				 infos = files.parallelStream().map(file -> {
					MetadataVO.Info info = new MetadataVO.Info();
					info.setLength((long) file.get("length"));
					Object pathObj = file.get("path");
					if (pathObj instanceof String) {
						info.setName((String) pathObj);
					} else if (pathObj instanceof List) {
						List<String> pathList = (List<String>) pathObj;
						StringBuilder pathSB = new StringBuilder();
						for (String item : pathList) {
							pathSB.append("/").append(item);
						}
						info.setName(pathSB.toString());
					}
					return info;
				}).toList();
				for (MetadataVO.Info info : infos) {
					length += info.getLength();
				}
			} else {
				length = (long) rawMap.get("length");
				infos = Collections.singletonList(new MetadataVO.Info(name, length));
			}
			return Metadata.builder()
					.name(name)
					.infoHash(infoHashHexStr)
					.length(length)
					.filesInfo(objectMapper.writeValueAsString(infos))
					.build();
		} catch (Exception e) {
			log.error("[bytes2Metadata]失败.e:", e.getMessage(), e);
		}
		return null;
	}


	/**
	 * 消息处理类
	 */
	@Getter
	@NoArgsConstructor
	private class FetchMetadataHandler extends SimpleChannelInboundHandler<ByteBuf> {
		private String infoHashHexStr;
		private Result result;

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
			byte[] bytes = new byte[msg.readableBytes()];
			msg.readBytes(bytes);

			String messageStr = new String(bytes, CharsetUtil.ISO_8859_1);

			//收到握手消息回复
			if (bytes[0] == (byte) 19) {
				//发送扩展消息
				SendExtendMessage(ctx);
			}

			//如果收到的消息中包含ut_metadata,提取出ut_metadata的值
			String utMetadataStr = "ut_metadata";
			String metadataSizeStr = "metadata_size";
			if (messageStr.contains(utMetadataStr) && messageStr.contains(metadataSizeStr)) {
				sendMetadataRequest(ctx, messageStr, utMetadataStr, metadataSizeStr);
			}

			//如果是分片信息
			if (messageStr.contains("msg_type")) {
//				log.info("收到分片消息:{}", messageStr);
				fetchMetadataBytes(messageStr);
			}
		}


		/***
		 * 获取metadataBytes
		 */
		private void fetchMetadataBytes(String messageStr) {
			String resultStr = messageStr.substring(messageStr.indexOf("ee") + 2);
			byte[] resultStrBytes = resultStr.getBytes();
			if (result.getResult() != null) {
				result.setResult(ArrayUtil.addAll(result.getResult(), resultStrBytes));
			} else {
				result.setResult(resultStrBytes);
			}
			//唤醒latch
			result.getLatch().countDown();
		}

		/**
		 * 发送 metadata 请求消息
		 */
		private void sendMetadataRequest(ChannelHandlerContext ctx, String messageStr, String utMetadataStr, String metadataSizeStr) {
			int utMetadataIndex = messageStr.indexOf(utMetadataStr) + utMetadataStr.length() + 1;
			//ut_metadata值
			int utMetadataValue = Integer.parseInt(messageStr.substring(utMetadataIndex, utMetadataIndex + 1));
			int metadataSizeIndex = messageStr.indexOf(metadataSizeStr) + metadataSizeStr.length() + 1;
			String otherStr = messageStr.substring(metadataSizeIndex);
			//metadata_size值
			int metadataSize = Integer.parseInt(otherStr.substring(0, otherStr.indexOf("e")));
			//分块数
			int blockSum = (int) Math.ceil((double) metadataSize / Config.METADATA_PIECE_SIZE);
			log.info("该种子metadata大小:{},分块数:{}",metadataSize,blockSum);

			//发送metadata请求
			for (int i = 0; i < blockSum; i++) {
				Map<String, Object> metadataRequestMap = new LinkedHashMap<>();
				metadataRequestMap.put("msg_type", 0);
				metadataRequestMap.put("piece", i);
				byte[] metadataRequestMapBytes = bencode.encode(metadataRequestMap);
				byte[] metadataRequestBytes = new byte[metadataRequestMapBytes.length + 6];
				metadataRequestBytes[4] = 20;
				metadataRequestBytes[5] = (byte) utMetadataValue;
				byte[] lenBytes = CodeUtil.int2Bytes(metadataRequestMapBytes.length + 2);
				System.arraycopy(lenBytes, 0, metadataRequestBytes, 0, 4);
				System.arraycopy(metadataRequestMapBytes, 0, metadataRequestBytes, 6, metadataRequestMapBytes.length);
				ctx.channel().writeAndFlush(Unpooled.copiedBuffer(metadataRequestBytes));
				log.info("{}发送metadata请求消息:{}", infoHashHexStr, new String(metadataRequestBytes, CharsetUtil.ISO_8859_1));
			}
		}

		/**
		 * 发送扩展消息
		 *
		 * @param ctx
		 */
		private void SendExtendMessage(ChannelHandlerContext ctx) {
			Map<String, Object> extendMessageMap = new LinkedHashMap<>();
			Map<String, Object> extendMessageMMap = new LinkedHashMap<>();
			extendMessageMMap.put("ut_metadata", 1);
			extendMessageMap.put("m", extendMessageMMap);
			byte[] tempExtendBytes = bencode.encode(extendMessageMap);
			byte[] extendMessageBytes = new byte[tempExtendBytes.length + 6];
			extendMessageBytes[4] = 20;
			extendMessageBytes[5] = 0;
			byte[] lenBytes = CodeUtil.int2Bytes(tempExtendBytes.length + 2);
			System.arraycopy(lenBytes, 0, extendMessageBytes, 0, 4);
			System.arraycopy(tempExtendBytes, 0, extendMessageBytes, 6, tempExtendBytes.length);
			log.info("{}发送扩展消息:{}", infoHashHexStr, new String(extendMessageBytes, CharsetUtil.ISO_8859_1));
			ctx.channel().writeAndFlush(Unpooled.copiedBuffer(extendMessageBytes));
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			log.error("{}{}异常:{}", LOG, infoHashHexStr, cause.getMessage());
			//关闭
			ctx.close();
		}

		public FetchMetadataHandler(String infoHashHexStr, Result result) {
			this.infoHashHexStr = infoHashHexStr;
			this.result = result;
		}
	}

	/**
	 * 连接监听器
	 */
	@AllArgsConstructor
	private static class ConnectListener implements ChannelFutureListener {
		private String infoHashHexStr;
		//自己的peerId,直接定义为和nodeId相同即可
		private byte[] selfPeerId;

		@Override
		public void operationComplete(ChannelFuture future){
			if (future.isSuccess()) {
				//连接成功发送握手消息
				SendHandshakeMessage(future);
				return;
			}
			//如果失败 ,不做任何操作
			future.channel().close();
		}

		/**
		 * 发送握手消息
		 */
		private void SendHandshakeMessage(ChannelFuture future) {
			byte[] infoHash = CodeUtil.hexStr2Bytes(infoHashHexStr);
			byte[] sendBytes = new byte[68];
			System.arraycopy(Config.GET_METADATA_HANDSHAKE_PRE_BYTES, 0, sendBytes, 0, 28);
			System.arraycopy(infoHash, 0, sendBytes, 28, 20);
			System.arraycopy(selfPeerId, 0, sendBytes, 48, 20);
			future.channel().writeAndFlush(Unpooled.copiedBuffer(sendBytes));
		}
	}

	/**
	 * 通道初始化器
	 */
	@AllArgsConstructor
	private class CustomChannelInitializer extends ChannelInitializer {
		private String infoHashHexStr;
		private final Result result;

		@Override
		protected void initChannel(Channel ch){
			ch.pipeline()
					.addLast(new ReadTimeoutHandler(config.getPerformance().getFetchMetadataByPeerTaskReadTimeoutSecond()))
					.addLast(new FetchMetadataHandler(infoHashHexStr, result));

		}
	}

	/**
	 * 返回对象
	 */
	@Data
	private class Result {
		private byte[] result;
		private final CountDownLatch latch;

		public Result(CountDownLatch latch) {
			this.latch = latch;
		}
	}

}
