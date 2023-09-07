package com.lkd.bt.spider.util;

import cn.hutool.core.util.ArrayUtil;
import com.lkd.bt.common.exception.BTException;
import com.lkd.bt.common.util.CodeUtil;
import com.lkd.bt.common.util.EnumUtil;
import com.lkd.bt.common.util.RandomUtil;
import com.lkd.bt.spider.config.Config;
import com.lkd.bt.spider.dto.Message;
import com.lkd.bt.spider.dto.Peer;
import com.lkd.bt.spider.entity.Node;
import com.lkd.bt.spider.enums.QEnum;
import com.lkd.bt.spider.enums.YEnum;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lkkings on 2023/8/24
 */
@Slf4j
@Component
public class BTUtil {

    //用于递增消息ID
    private static final AtomicInteger messageIDGenerator = new AtomicInteger(1);
    private static final AtomicInteger getPeersMessageIDGenerator = new AtomicInteger(1);
    //递增刷新阈值
    private static int maxMessageID = 1<<15;
    /**
     * 从channel中获取到当前通道的id
     */
    public static String getChannelId(Channel channel) {
        return channel.id().asShortText();
    }

    /**
     * 生成一个随机的nodeId
     */
    public static byte[] generateNodeId() {
        return RandomUtil.simpleNextBytes(20);
    }

    /**
     * 生成一个随机的nodeID
     */
    public static String generateNodeIdString() {
        return new String(generateNodeId());
    }


    /**
     * 生成一个递增的t,相当于消息id
     * 使用指定生成器
     */
    private static String generateMessageID(AtomicInteger generator) {
        int result;
        //当大于阈值时,重置
        if ((result = generator.getAndIncrement()) > maxMessageID) {
            generator.lazySet(1);
        }
        return new String(CodeUtil.int2TwoBytes(result));
    }

    /**
     * 生成一个递增的t,相当于消息id
     */
    public static String generateMessageID() {
        return generateMessageID(messageIDGenerator);
    }
    public static byte[] generateNeighborNodeId(String nodeId) {
        SecureRandom random = new SecureRandom();
        byte[] preNodeIdBytes = ArrayUtil.sub(CodeUtil.hexStr2Bytes(nodeId),0,16);
        byte[] sufNodeIdBytes = ArrayUtil.sub(generateNodeId(),16,20);
        return ArrayUtil.addAll(preNodeIdBytes,sufNodeIdBytes);
    }

    public static String generateNeighborNodeIdString(String nodeId) {
        return new String(generateNeighborNodeId(nodeId));
    }

    /**
     * 生成一个递增的t,相当于消息id
     * 用于get_peers请求
     */
    public static String generateMessageIDOfGetPeers() {
        return generateMessageID(getPeersMessageIDGenerator);
    }


    /**
     * 根据解析后的消息map,获取消息信息,例如 消息方法(ping/find_node等)/ 消息状态(请求/回复/异常)
     */
    public static Message getMessage(Map<String, Object> map) throws Exception {
        Message messageInfo = new Message();
        //状态 (请求/回复/异常)
        String y = getParamString(map, "y", "y属性不存在.map:" + map);
        Optional<YEnum> yEnumOptional = EnumUtil.getByCode(y, YEnum.class);
        messageInfo.setStatus(yEnumOptional.orElseThrow(()->new BTException("y属性值不正确.map:" + map)));
        //消息id
        String t = getParamString(map, "t", "t属性不存在.map:" + map);
        messageInfo.setMessageId(t);
        //获取方法 ping/find_node等
        //如果是请求, 直接从请求主体获取其方法
        if (EnumUtil.equals(messageInfo.getStatus().getCode(), YEnum.QUERY)) {
            String q = getParamString(map, "q", "q属性不存在.map:" + map);

            Optional<QEnum> qEnumOptional = EnumUtil.getByCode(q, QEnum.class);
            messageInfo.setMethod(qEnumOptional.orElseThrow(()->new BTException("q属性值不正确.map:" + map)));

        } else  if (EnumUtil.equals(messageInfo.getStatus().getCode(), YEnum.RECEIVE))  {
            Map<String, Object> rMap = BTUtil.getParamMap(map, "r", "r属性不存在.map:" + map);

            if(rMap.get("token") != null){
                messageInfo.setMethod(QEnum.GET_PEERS);
            }else if(rMap.get("nodes") != null){
                messageInfo.setMethod(rMap.get("token") == null ? QEnum.FIND_NODE : QEnum.GET_PEERS);
            }else{
                throw new BTException("未知类型的回复消息.消息:" + map);
            }
        }
        return messageInfo;
    }

    /**
     * 从Map中获取Object属性
     */
    public static Object getParam(Map<String, Object> map, String key, String log) {
        Object obj = map.get(key);
        if (obj == null)
            throw new BTException(log);
        return obj;
    }

    /**
     * 从Map中获取String属性
     */
    public static String getParamString(Map<String, Object> map, String key, String log) {
        Object obj = getParam(map, key, log);
        return (String) obj;
    }

    /**
     * 从Map中获取Integer属性
     */
    public static Integer getParamInteger(Map<String, Object> map, String key, String log) {
        Object obj = getParam(map, key, log);
        return (Integer) obj;
    }

    /**
     * 从Map中获取List属性
     */

    @SuppressWarnings("unchecked")
    public static List<String> getParamList(Map<String, Object> map, String key, String log) {
        Object obj = getParam(map, key, log);
        return (List<String>) obj;
    }

    /**
     * 从Map中获取Map属性
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getParamMap(Map<String, Object> map, String key, String log) {
        Object obj = getParam(map, key, log);
        return (Map<String, Object>) obj;
    }

    /**
     * 从udp返回的sender属性中,提取出ip
     */
    public static String getIpBySender(InetSocketAddress sender) {
        return sender.getAddress().toString().substring(1);
    }

    /**
     * 从回复的r对象中取出nodes
     */
    public static List<Node> getNodeListByRMap(Map<String, Object> rMap) {
        byte[] nodesBytes = BTUtil.getParamString(rMap, "nodes", "FIND_NODE,找不到nodes参数.rMap:" + rMap).getBytes();
        List<Node> nodeList = new LinkedList<>();
        for (int i = 0; i + Config.NODE_BYTES_LEN < nodesBytes.length; i += Config.NODE_BYTES_LEN) {
            //byte[26] 转 Node
            Node node = new Node(ArrayUtil.sub(nodesBytes, i, i + Config.NODE_BYTES_LEN));
            nodeList.add(node);
        }
        return nodeList;
    }

    /**
     * 获取peer地址
     * @param peerStrList
     * @return
     */
    public static String getPeerAddress(List<String> peerStrList){
        StringBuilder peersInfoBuilder = new StringBuilder();
        getPeerList(peerStrList).forEach(peer -> peersInfoBuilder.append(peer.getIp()).append(":").append(peer.getPort()).append(";"));
        return peersInfoBuilder.toString();
    }

    /**
     * 获取peer列表
     * @param peerStrList
     * @return
     */
    public static List<Peer> getPeerList(List<String> peerStrList){
        List<Peer> peerList = new LinkedList<>();
        for (String rawPeer : peerStrList) {
            //byte[6] 转 Peer
            Peer peer = new Peer(rawPeer.getBytes());
            peerList.add(peer);
        }
        return peerList;
    }
}
