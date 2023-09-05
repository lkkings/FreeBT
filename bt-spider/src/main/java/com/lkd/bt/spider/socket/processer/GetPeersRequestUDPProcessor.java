package com.lkd.bt.spider.socket.processer;

import com.lkd.bt.common.util.CodeUtil;
import com.lkd.bt.spider.entity.Node;
import com.lkd.bt.spider.enums.NodeRankEnum;
import com.lkd.bt.spider.enums.QEnum;
import com.lkd.bt.spider.enums.YEnum;
import com.lkd.bt.spider.socket.RoutingTable;
import com.lkd.bt.spider.socket.Sender;
import com.lkd.bt.spider.socket.core.Process;
import com.lkd.bt.spider.socket.core.UDPProcessor;
import com.lkd.bt.spider.task.GetPeersTask;
import com.lkd.bt.spider.util.BTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;


/**
 * Created by lkkings on 2023/8/24
 * ANNOUNCE_PEER 请求 处理器
 */
@Order(3)
@Slf4j
@Component
@RequiredArgsConstructor
public class GetPeersRequestUDPProcessor extends UDPProcessor {
	private static final String LOG = "[GET_PEERS]";

	private final List<RoutingTable> routingTables;

	private final Sender sender;

	private final GetPeersTask getPeersTask;

	@Override
	public boolean process1(Process process) {
		Map<String, Object> rawMap = process.getRawMap();
		InetSocketAddress sender = process.getSender();
		int index = process.getIndex();

		Map<String, Object> aMap = BTUtil.getParamMap(rawMap, "a", "GET_PEERS,找不到a参数.map:" + rawMap);
		byte[] infoHash = BTUtil.getParamString(aMap, "info_hash", "GET_PEERS,找不到info_hash参数.map:" + rawMap).getBytes();
		byte[] id = BTUtil.getParamString(aMap, "id", "GET_PEERS,找不到id参数.map:" + rawMap).getBytes();
		List<Node> nodes = routingTables.get(index).getForTop8(infoHash);
		log.info("{}发送者:{},info_hash:{}", LOG, sender,infoHash);
		//回复时,将自己的nodeId伪造为 和该节点异或值相差不大的值
		this.sender.getPeersReceive(process.getMessage().getMessageId(), sender,
				CodeUtil.generateSimilarInfoHashString(id, config.getMain().getSimilarNodeIdNum()),
				config.getMain().getToken(), nodes, index);
		//加入路由表
		routingTables.get(index).put(new Node(id, sender, NodeRankEnum.GET_PEERS.getCode()));
		getPeersTask.put(CodeUtil.bytes2HexStr(infoHash));
		return true;
	}

	@Override
	public boolean isProcess(Process process) {
		return QEnum.GET_PEERS.equals(process.getMessage().getMethod()) && YEnum.QUERY.equals(process.getMessage().getStatus());
	}
}
