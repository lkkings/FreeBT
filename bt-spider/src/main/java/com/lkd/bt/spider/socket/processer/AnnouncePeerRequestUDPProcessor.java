package com.lkd.bt.spider.socket.processer;

import com.lkd.bt.common.util.CodeUtil;
import com.lkd.bt.spider.dto.bt.AnnouncePeer;
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
import com.lkd.bt.spider.task.FetchMetadataTask;
import com.lkd.bt.spider.task.FindNodeTask;
import com.lkd.bt.spider.task.GetPeersTask;
import com.lkd.bt.spider.util.BTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Created by lkkings on 2023/8/24
 * ANNOUNCE_PEER 请求 处理器
 */
@Order(2)
@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncePeerRequestUDPProcessor extends UDPProcessor {
    private static final String LOG = "[ANNOUNCE_PEER]";

    private final List<RoutingTable> routingTables;

    private final NodeServiceImpl nodeService;

    private final GetPeersTask getPeersTask;

    private final Sender sender;

    private final InfoHashServiceImpl infoHashService;

    private final RBlockingQueue<Node> findNodeQueue;

    @Override
    public boolean process1(Process process) {
        AnnouncePeer.RequestContent requestContent = new AnnouncePeer.RequestContent(process.getRawMap(), process.getSender().getPort());
        String infoHashHexStr = requestContent.getInfo_hash();
        int index  = process.getIndex();
        byte[] nodeIdBytes = CodeUtil.hexStr2Bytes(requestContent.getId());
        if ((infoHashHexStr.length() != 40) || (nodeIdBytes.length != 20)){
            log.error("错误的info_hash:{} 或nodeId length: {}",infoHashHexStr,nodeIdBytes.length);
            return true;
        }

        //入库
        infoHashService.saveInfoHash(infoHashHexStr, BTUtil.getIpBySender(process.getSender()) + ":" + requestContent.getPort() + ";");
        //回复
        this.sender.announcePeerReceive(process.getMessage().getMessageId(), process.getSender(), nodeIds.get(process.getIndex()), index);
        Node node = new Node(nodeIdBytes, process.getSender(), NodeRankEnum.ANNOUNCE_PEER.getCode());
        //加入路由表
        routingTables.get(index).put(node);
        //入库
        nodeService.save(node);
        //加入任务队列
        getPeersTask.put(infoHashHexStr);
        //加入findNode任务队列
        findNodeQueue.offer(node);
        return true;
    }

    @Override
    public boolean isProcess(Process process) {
        return QEnum.ANNOUNCE_PEER.equals(process.getMessage().getMethod()) && YEnum.QUERY.equals(process.getMessage().getStatus());
    }
}
