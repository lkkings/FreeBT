package com.lkd.bt.spider.entity;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.lkd.bt.common.exception.BTException;
import com.lkd.bt.common.util.CodeUtil;
import com.lkd.bt.spider.config.Config;
import com.lkd.bt.spider.util.BTUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;

/**
 * Created by lkkings on 2023/8/24
 * DHT网络节点封装
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class Node {
    private Long id;


    /**
     * 存储16进制形式的String, byte[20] 转的16进制String,长度固定为40
     */
    private String nodeId  = "";


    private String ip;

    private Integer port;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * nodeIds 字节表示
     */
    private byte[] nodeIdBytes;

    /**
     * 最后活动时间(收到请求或收到回复)
     */
    private Date lastActiveTime = new Date();

    /**
     * 权重,允许并发导致的一些误差
     */
    private Integer rank = 0;



    /**
     * 增加rank
     */
    public Node addRank(int addValue) {
        if(Integer.MAX_VALUE - rank >= addValue)
            rank += addValue;
        else
            rank = Integer.MAX_VALUE;
        return this;
    }

    /**
     * 检查该节点信息是否完整
     */
    public void check() {
        //此处对小于1024的私有端口.不作为错误.
        if(nodeIdBytes == null || nodeIdBytes.length != 20 ||
                StrUtil.isBlank(ip) || port == null ||  port > 65535)
            throw new BTException("该节点信息有误:" + this);
    }

    /**
     * Node 转 InetSocketAddress
     */
    public InetSocketAddress toAddress() {
        return new InetSocketAddress(this.ip, this.port);
    }

    /**
     * List<Node> 转 byte[]
     */
    public static byte[] toBytes(List<Node> nodes) {
        if(CollectionUtils.isEmpty(nodes))
            return new byte[0];
        byte[] result = new byte[nodes.size() * Config.NODE_BYTES_LEN];
        for (int i = 0; i + Config.NODE_BYTES_LEN <= result.length; i+=Config.NODE_BYTES_LEN) {
            System.arraycopy(nodes.get(i/Config.NODE_BYTES_LEN).toBytes(),0,result,i,Config.NODE_BYTES_LEN);
        }
        return result;
    }

    /**
     * Node 转 byte[]
     */
    public byte[] toBytes() {
        check();
        //nodeIds
        byte[] nodeBytes = new byte[Config.NODE_BYTES_LEN];
        System.arraycopy(nodeIdBytes, 0, nodeBytes, 0, 20);
        //ip
        String[] ips = ip.split("\\.");
        if(ips.length != 4)
            throw new BTException("该节点IP有误,节点信息:" + this);
        byte[] ipBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            ipBytes[i] = (byte) Integer.parseInt(ips[i]);
        }
        System.arraycopy(ipBytes, 0, nodeBytes, 20, 4);

        //ports
        byte[] portBytes = CodeUtil.int2TwoBytes(port);
        System.arraycopy(portBytes, 0, nodeBytes, 24, 2);

        return nodeBytes;
    }

    /**
     * byte[26] 转 Node
     */
    public Node(byte[] bytes) {
        if (bytes.length != Config.NODE_BYTES_LEN)
            throw new BTException("转换为Node需要bytes长度为"+Config.NODE_BYTES_LEN+",当前为:" + bytes.length);
        nodeIdBytes = ArrayUtil.sub(bytes, 0, 20);
        ip = CodeUtil.bytes2Ip(ArrayUtil.sub(bytes, 20, 24));
        port = CodeUtil.bytes2Port(ArrayUtil.sub(bytes, 24, Config.NODE_BYTES_LEN));
        initHexStrNodeId();
    }

    public Node(byte[] nodeIdBytes, String ip, Integer port) {
        this.nodeIdBytes = nodeIdBytes;
        this.ip = ip;
        this.port = port;
        initHexStrNodeId();

    }

    public Node(byte[] nodeIdBytes, String ip, Integer port, Integer rank) {
        this.nodeIdBytes = nodeIdBytes;
        this.ip = ip;
        this.port = port;
        this.rank = rank;
        initHexStrNodeId();
    }

    public Node(byte[] nodeIdBytes, InetSocketAddress sender, Integer rank) {
        if(nodeIdBytes.length != 20){
            System.out.println();
        }
        this.nodeIdBytes = nodeIdBytes;
        this.ip = BTUtil.getIpBySender(sender);
        this.port = sender.getPort();
        this.rank = rank;
        initHexStrNodeId();
    }

    //生成nodeId
    public void initHexStrNodeId() {
        if(this.nodeIdBytes != null)
            this.nodeId = CodeUtil.bytes2HexStr(this.nodeIdBytes);
    }
}
