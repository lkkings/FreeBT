package com.lkd.bt.spider.dto.bt;

import com.lkd.bt.common.exception.BTException;
import com.lkd.bt.common.util.CodeUtil;
import com.lkd.bt.spider.dto.bt.base.CommonRequest;
import com.lkd.bt.spider.dto.bt.base.CommonResponse;
import com.lkd.bt.spider.enums.QEnum;
import com.lkd.bt.spider.enums.YEnum;
import com.lkd.bt.spider.util.BTUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * Created by lkkings on 2023/8/24
 * 宣告peer方法请求&回复
 * announce_peers Query = {"t":"aa", "y":"q", "q":"announce_peer", "a": {"id":"abcdefghij0123456789", "implied_port": 1, "info_hash":"mnopqrstuvwxyz123456", "port": 6881, "token": "aoeusnth"}}
 * bencoded = d1:ad2:id20:abcdefghij01234567899:info_hash20:<br /> mnopqrstuvwxyz1234564:porti6881e5:token8:aoeusnthe1:q13:announce_peer1:t2:aa1:y1:qe
 * Response = {"t":"aa", "y":"r", "r": {"id":"mnopqrstuvwxyz123456"}}
 * bencoded = d1:rd2:id20:mnopqrstuvwxyz123456e1:t2:aa1:y1:re
 */
public abstract class AnnouncePeer {
    /**
     * 请求主体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class RequestContent {
        /**
         * 请求方nodeID
         */
        private String id;

        /**
         * 可选, 为1时,忽略port参数,直接将请求发送节点的发送port作为port
         */
        private Integer implied_port;

        /**
         * 种子文件的infohash
         */
        private String info_hash;

        /**
         * 正在下载种子的端口
         */
        private Integer port;

        /**
         * 之前get_peers请求中的token
         */
        private String token;

        public  RequestContent (Map<String, Object> map,int defaultPort) {
            Map<String, Object> aMap = BTUtil.getParamMap(map, "a", "ANNOUNCE_PEER,找不到a参数.map:" + map);
            info_hash = CodeUtil.bytes2HexStr(BTUtil.getParamString(aMap, "info_hash", "ANNOUNCE_PEER,找不到info_hash参数.map:" + map)
                    .getBytes());
            if (aMap.get("implied_port") == null || ((long) aMap.get("implied_port") )== 0) {
                Object portObj = aMap.get("port");
                if(portObj == null)
                    throw new BTException("ANNOUNCE_PEER,找不到port参数.map:" + map);
                port = ((Long) portObj).intValue();
            }else
                port = defaultPort;
            id = CodeUtil.bytes2HexStr(BTUtil.getParamString(aMap, "id", "ANNOUNCE_PEER,找不到id参数.map:" + map).getBytes());

        }
    }


    /**
     * 请求
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Request extends CommonRequest {

        /**主体,包含请求发送方的nodeID(也就是自己的)*/
        private RequestContent a;

        private void init() {
            t = BTUtil.generateMessageID();
            y = YEnum.QUERY.getCode();
            q = QEnum.ANNOUNCE_PEER.getCode();
            a = new RequestContent();
        }

        /**
         * 指定请求发送方nodeID/ 要查找的nodeId构造
         */
        public Request(String nodeId,String info_hash,int port,String token) {
            init();
            a.id = nodeId;
            a.info_hash = info_hash;
            a.port = port;
            a.token = token;
        }
    }

    /**
     * 响应主体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class ResponseContent {
        /**
         * 回复方nodeID
         */
        private String id;
    }

    /**
     * 响应
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Response extends CommonResponse {

        /**主体,*/
        private ResponseContent r;

        private void init() {
            y = YEnum.RECEIVE.getCode();
            r = new ResponseContent();
        }

        /**
         * 指定回复方id
         */
        public Response(String nodeId,String messageId) {
            init();
            r.id = nodeId;
            t = messageId;
        }
    }



}
