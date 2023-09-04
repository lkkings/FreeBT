package com.lkd.bt.spider.socket.processer;

import com.lkd.bt.spider.entity.Node;
import com.lkd.bt.spider.enums.NodeRankEnum;
import com.lkd.bt.spider.enums.QEnum;
import com.lkd.bt.spider.enums.YEnum;
import com.lkd.bt.spider.socket.RoutingTable;
import com.lkd.bt.spider.task.FindNodeTask;
import com.lkd.bt.spider.util.BTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by lkkings on 2023/8/24
 * findNode 回复 处理器
 */
@Order(1)
@Slf4j
@Component
@RequiredArgsConstructor
public class FindNodeResponseUDPProcessor extends UDPProcessor {
	private static final String LOG = "[FIND_NODE_RECEIVE]";

	private final List<RoutingTable> routingTables;

	private final FindNodeTask findNodeTask;

	@Override
	boolean process1(Process process) {
		//回复主体
		Map<String, Object> rMap = BTUtil.getParamMap(process.getRawMap(), "r", "FIND_NODE,找不到r参数.map:" + process.getRawMap());
		List<Node> nodeList = BTUtil.getNodeListByRMap(rMap);
		//为空退出
		if (CollectionUtils.isEmpty(nodeList)) return true;
		//去重
		Node[] nodes = nodeList.stream().distinct().toArray(Node[]::new);
		log.info("{}.发送者:{},返回的nodes:{}", LOG, process.getSender(),nodes);
		//将nodes加入发送队列
		for (Node node : nodes) {
			findNodeTask.put(node.toAddress());
		}
		byte[] id = BTUtil.getParamString(rMap, "id", "FIND_NODE,找不到id参数.map:" + process.getRawMap()).getBytes();
		//将发送消息的节点加入路由表
		routingTables.get(process.getIndex()).put(new Node(id, process.getSender(), NodeRankEnum.FIND_NODE_RECEIVE.getCode()));
		return true;
	}

	@Override
	boolean isProcess(Process process) {
		return QEnum.FIND_NODE.equals(process.getMessage().getMethod()) && YEnum.RECEIVE.equals(process.getMessage().getStatus());
	}


}
