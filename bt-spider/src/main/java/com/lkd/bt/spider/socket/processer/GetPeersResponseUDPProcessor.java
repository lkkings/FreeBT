package com.lkd.bt.spider.socket.processer;

import com.lkd.bt.common.util.CodeUtil;
import com.lkd.bt.spider.constant.RedisConstant;
import com.lkd.bt.spider.dto.GetPeersSendInfo;
import com.lkd.bt.spider.dto.Message;
import com.lkd.bt.spider.entity.Node;
import com.lkd.bt.spider.enums.NodeRankEnum;
import com.lkd.bt.spider.enums.QEnum;
import com.lkd.bt.spider.enums.YEnum;
import com.lkd.bt.spider.service.impl.InfoHashServiceImpl;
import com.lkd.bt.spider.service.impl.NodeServiceImpl;
import com.lkd.bt.spider.socket.RoutingTable;
import com.lkd.bt.spider.socket.Sender;
import com.lkd.bt.spider.socket.core.Process;
import com.lkd.bt.spider.socket.core.UDPProcessor;
import com.lkd.bt.spider.task.FindNodeTask;
import com.lkd.bt.spider.util.BTUtil;
import io.netty.util.CharsetUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Created by lkkings on 2023/8/24
 * GET_PEERS 回复 处理器
 */
@Order(5)
@Slf4j
@Component
@RequiredArgsConstructor
public class GetPeersResponseUDPProcessor extends UDPProcessor {
	private static final String LOG = "[GET_PEERS_RECEIVE]";

	private final List<RoutingTable> routingTables;

	private final FindNodeTask findNodeTask;

	private final Sender sender;

	private final RedisServiceImpl redisService;

	private final InfoHashServiceImpl infoHashService;

	private final NodeServiceImpl nodeService;


	@Override
	public boolean process1(Process process) {
		Message message = process.getMessage();
		Map<String, Object> rawMap = process.getRawMap();
		InetSocketAddress sender = process.getSender();
		int index = process.getIndex();
		RoutingTable routingTable = routingTables.get(index);

		//查询缓存
		GetPeersSendInfo getPeersSendInfo = (GetPeersSendInfo) redisService.get(RedisConstant.GET_PEER_SEND_INFO,message.getMessageId());
		//查询rMap,此处rMap不可能不存在
		Map<String, Object> rMap = BTUtil.getParamMap(rawMap, "r", "");
		//缓存过期，则不做任何处理了
		if (getPeersSendInfo == null) return true;

		byte[] id = BTUtil.getParamString(rMap, "id", "GET_PEERS-RECEIVE,找不到id参数.map:" + rMap).getBytes(CharsetUtil.ISO_8859_1);
		//如果返回的是nodes
		if (rMap.get("nodes") != null) {
			return nodesHandler(message, sender, index, routingTable, getPeersSendInfo, rMap, id);
		}

		if (rMap.get("values") == null) return true;
		//如果返回的是values peer
		return valuesHandler(message, rawMap, sender, routingTable, getPeersSendInfo, rMap, id,index);
	}

	/**
	 * 处理values返回
	 */
	private boolean valuesHandler(Message message, Map<String, Object> rawMap, InetSocketAddress sender, RoutingTable routingTable, GetPeersSendInfo getPeersSendInfo, Map<String, Object> rMap, byte[] id,int index) {
		List<String> rawPeerList;
		try {
			rawPeerList = BTUtil.getParamList(rMap, "values", "GET_PEERS-RECEIVE,找不到values参数.map:" + rawMap);
		} catch (Exception e) {
			//如果发生异常,说明该values参数可能是string类型的
			String values = BTUtil.getParamString(rawMap, "values", "GET_PEERS-RECEIVE,找不到values参数.map:" + rawMap);
			rawPeerList = Collections.singletonList(values);
		}
		if (CollectionUtils.isEmpty(rawPeerList)) {
			routingTable.delete(id);
			return true;
		}
		//将peers连接为字符串
		String address = BTUtil.getPeerAddress(rawPeerList);
		log.info("{}发送者:{},info_hash:{},消息id:{},返回peers:{}", LOG, sender, getPeersSendInfo.getInfoHash(), message.getMessageId(),address);
		//清除该任务缓存 和 连接peer任务
		redisService.remove(RedisConstant.GET_PEER_SEND_INFO,message.getMessageId());
		//入库
		infoHashService.saveInfoHash(getPeersSendInfo.getInfoHash(),address);

		//节点入库
		nodeService.save(new Node(null, BTUtil.getIpBySender(sender), sender.getPort()));
		routingTable.put(new Node(id, sender, NodeRankEnum.GET_PEERS_RECEIVE_OF_VALUE.getCode()));
		//并向该节点发送findNode请求
		findNodeTask.put(sender);
		return true;
	}

	/**
	 * 处理nodes返回
	 * @param message 发送过来的消息信息
	 * @param sender 发送者
	 * @param index 当前端口索引
	 * @param routingTable 路由表
	 * @param getPeersSendInfo 之前缓存的get_peers发送信息
	 * @param rMap 消息的原始map
	 * @param id 对方id
	 */
	private boolean nodesHandler(Message message, InetSocketAddress sender, int index, RoutingTable routingTable, GetPeersSendInfo getPeersSendInfo, Map<String, Object> rMap, byte[] id) {
		List<Node> nodeList = BTUtil.getNodeListByRMap(rMap);
		//如果nodes为空
		if (CollectionUtils.isEmpty(nodeList)) {
			routingTable.delete(id);
			return true;
		}
		//向新节点发送消息
		nodeList.forEach(item -> this.sender.findNode(item.toAddress(), nodeIds.get(index), BTUtil.generateNodeIdString(), index));
		//将消息发送者加入路由表.
		routingTable.put(new Node(id, sender, NodeRankEnum.GET_PEERS_RECEIVE.getCode()));
		log.info("{}GET_PEERS-RECEIVE,发送者:{},info_hash:{},消息id:{},返回nodes", LOG, sender, getPeersSendInfo.getInfoHash(), message.getMessageId());

		//取出未发送过请求的节点
		List<Node> unSentNodeList = nodeList.stream().filter(node -> !getPeersSendInfo.contains(node.getNodeIdBytes())).toList();
		//为空退出
		if (CollectionUtils.isEmpty(unSentNodeList)) {
			log.info("{}发送者:{},info_hash:{},消息id:{},所有节点已经发送过请求.", LOG, sender, getPeersSendInfo.getInfoHash(), message.getMessageId());
			return true;
		}
		//未发送过请求的节点id
		List<byte[]> unSentNodeIdList = unSentNodeList.stream().map(Node::getNodeIdBytes).collect(Collectors.toList());
		//将其加入已发送队列
		getPeersSendInfo.put(unSentNodeIdList);
		//未发送过请求节点的地址
		List<InetSocketAddress> unSentAddressList = unSentNodeList.stream().map(Node::toAddress).collect(Collectors.toList());
		//批量发送请求
		this.sender.getPeersBatch(unSentAddressList, nodeIds.get(index),
				new String(CodeUtil.hexStr2Bytes(getPeersSendInfo.getInfoHash()), CharsetUtil.ISO_8859_1),
				message.getMessageId(), index);
		return true;
	}

	@Override
	public boolean isProcess(Process process) {
		return QEnum.GET_PEERS.equals(process.getMessage().getMethod()) && YEnum.RECEIVE.equals(process.getMessage().getStatus());
	}
}
