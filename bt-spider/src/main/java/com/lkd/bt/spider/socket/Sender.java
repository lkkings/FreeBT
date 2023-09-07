package com.lkd.bt.spider.socket;

import com.lkd.bt.common.exception.BTException;
import com.lkd.bt.common.util.BeanUtil;
import com.lkd.bt.spider.config.Config;
import com.lkd.bt.spider.dto.bt.AnnouncePeer;
import com.lkd.bt.spider.dto.bt.FindNode;
import com.lkd.bt.spider.dto.bt.GetPeers;
import com.lkd.bt.spider.dto.bt.Ping;
import com.lkd.bt.spider.entity.Node;
import com.lkd.bt.spider.task.Pauseable;
import com.lkd.bt.spider.util.BTUtil;
import com.lkd.bt.spider.util.Bencode;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by lkkings on 2023/8/24
 * 封装发送请求
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class Sender implements Pauseable {

    private List<Channel> channels;
    private Bencode bencode;

    //get_peers请求发送锁
    private  final ReentrantLock getPeersLock = new ReentrantLock();
    private Condition getPeersCondition;

    private Config config;
    private  int getPeersPauseMS;
    /**
     * 使用channel发送消息
     */
    public void writeAndFlush(byte[] bytes, InetSocketAddress address, int index) {
        if (!channels.get(index).isWritable()) {
            channels.get(index).close();
            throw new BTException("发送消息异常");
        }
        channels.get(index).writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(bytes), address));
    }

    /**
     * 发送ping请求
     */
    public  void ping(InetSocketAddress address, String nodeID,int index) {
        Ping.Request request = new Ping.Request(nodeID);

        writeAndFlush(bencode.encode(BeanUtil.beanToMap(request)), address,index);
    }



    /**
     * 回复ping请求
     */
    public  void pingReceive(InetSocketAddress address, String nodeID,String messageId,int index) {
        Ping.Response response = new Ping.Response(nodeID, messageId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)), address,index);
    }

    /**
     * 发送find_node请求(传入nodeId时,发送与nodeId逻辑距离较近的节点)
     */
    @SneakyThrows
    public  void findNode(InetSocketAddress address, String nodeId,String targetNodeId,int index) {
        FindNode.Request request = new FindNode.Request(BTUtil.generateNeighborNodeIdString(nodeId), targetNodeId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(request)), address,index);
    }

    /**
     * 发送find_node请求(不传入nodeId时,发送索引绑定的nodeId)
     */
    @SneakyThrows
    public  void findNode(InetSocketAddress address,String targetNodeId,int index) {
        List<String> nodeIds = config.getMain().getNodeIds();
        FindNode.Request request = new FindNode.Request(nodeIds.get(index), targetNodeId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(request)), address,index);
    }




    /**
     * 回复find_node回复
     */
    public  void findNodeReceive(String messageId, InetSocketAddress address, String nodeId, List<Node> nodeList, int index) {
        FindNode.Response response = new FindNode.Response(nodeId, new String(Node.toBytes(nodeList)),messageId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)),address,index);
    }





    /**
     * 回复announce_peer
     */
    public  void announcePeerReceive(String messageId,InetSocketAddress address, String nodeId,int index) {
        AnnouncePeer.Response response = new AnnouncePeer.Response(nodeId,messageId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)),address,index);
    }

    /**
     * 回复get_peers
     */
    public  void getPeersReceive(String messageId,InetSocketAddress address, String nodeId, String token, List<Node> nodeList,int index) {
        GetPeers.Response response = new GetPeers.Response(nodeId, token, new String(Node.toBytes(nodeList)),messageId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)),address,index);
    }

    /**
     * 批量发送get_peers
     */
    public  void getPeersBatch(List<InetSocketAddress> addresses, String nodeId,String infoHash,String messageId,int index) {
        GetPeers.Request request = new GetPeers.Request(nodeId, infoHash,messageId);
        byte[] encode = bencode.encode(BeanUtil.beanToMap(request));
        for (InetSocketAddress address : addresses) {
            try {
                writeAndFlush(encode,address,index);
                pause(getPeersLock,getPeersCondition,getPeersPauseMS, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.error("发送GET_PEERS,失败.e:{}",e.getMessage());
            }
        }
    }


    @Autowired
    public void init(Bencode bencode, Config config) {
        this.bencode = bencode;
        this.channels = new ArrayList<>(config.getMain().getPorts().size());
        //增加size到指定数量.以在setChannel方法中不越界
        for (int i = 0; i < config.getMain().getPorts().size(); i++) {
            this.channels.add(null);
        }
        this.getPeersCondition = this.getPeersLock.newCondition();
        this.getPeersPauseMS = config.getPerformance().getGetPeersRequestSendIntervalMs();
    }

    public  void setChannel(Channel channel,int index) {
        this.channels.set(index,channel);
    }
}

