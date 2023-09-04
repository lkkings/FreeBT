package com.lkd.bt.spider.socket.processer;

import com.lkd.bt.spider.entity.Node;
import com.lkd.bt.spider.enums.NodeRankEnum;
import com.lkd.bt.spider.enums.QEnum;
import com.lkd.bt.spider.enums.YEnum;
import com.lkd.bt.spider.socket.RoutingTable;
import com.lkd.bt.spider.socket.Sender;
import com.lkd.bt.spider.task.GetPeersTask;
import com.lkd.bt.spider.util.BTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by lkkings on 2023/8/24
 * findNode 请求 处理器
 */
@Order(4)
@Slf4j
@Component
@RequiredArgsConstructor
public class FindNodeRequestUDPProcessor extends UDPProcessor {
	private static final String LOG = "[FIND_NODE]";

	private final List<RoutingTable> routingTables;

	private final Sender sender;


	@Override
	boolean process1(Process process) {
		//截取出要查找的目标nodeId和 请求发送方nodeId
		Map<String, Object> aMap = BTUtil.getParamMap(process.getRawMap(), "a", "FIND_NODE,找不到a参数.map:" + process.getRawMap());
		byte[] targetNodeId = BTUtil.getParamString(aMap, "target", "FIND_NODE,找不到target参数.map:" + process.getRawMap())
				.getBytes();
		byte[] id = BTUtil.getParamString(aMap, "id", "FIND_NODE,找不到id参数.map:" + process.getRawMap()).getBytes();
		//查找
		List<Node> nodes = routingTables.get(process.getIndex()).getForTop8(targetNodeId);
		log.info("{}.发送者:{},返回的nodes:{}", LOG, sender,nodes);
		this.sender.findNodeReceive(process.getMessage().getMessageId(), process.getSender(),
				nodeIds.get(process.getIndex()), nodes,process.getIndex());
		//操作路由表
		routingTables.get(process.getIndex()).put(new Node(id, process.getSender(), NodeRankEnum.FIND_NODE.getCode()));

		return true;
	}

	@Override
	boolean isProcess(Process process) {
		return QEnum.FIND_NODE.equals(process.getMessage().getMethod()) && YEnum.QUERY.equals(process.getMessage().getStatus());
	}
}
