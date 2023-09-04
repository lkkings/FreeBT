package com.lkd.bt.spider.dto.bt;

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

/**
 * Created by lkkings on 2023/8/24
 * get_peers方法请求&回复
 * get_peers Query = {"t":"aa", "y":"q", "q":"get_peers", "a": {"id":"abcdefghij0123456789", "info_hash":"mnopqrstuvwxyz123456"}}
 * bencoded = d1:ad2:id20:abcdefghij01234567899:info_hash20:mnopqrstuvwxyz123456e1:q9:get_peers1:t2:aa1:y1:qe
 * Response with peers = {"t":"aa", "y":"r", "r": {"id":"abcdefghij0123456789", "token":"aoeusnth", "values": ["axje.u", "idhtnm"]}}
 * bencoded = d1:rd2:id20:abcdefghij01234567895:token8:aoeusnth6:valuesl6:axje.u6:idhtnmee1:t2:aa1:y1:re
 * Response with closest nodes = {"t":"aa", "y":"r", "r": {"id":"abcdefghij0123456789", "token":"aoeusnth", "nodes": "def456..."}}
 * bencoded = d1:rd2:id20:abcdefghij01234567895:nodes9:def456...5:token8:aoeusnthe1:t2:aa1:y1:re
 */
public abstract class GetPeers {

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
         * 种子文件的infohash
         */
        private String info_hash;
    }


    /**
     * 请求
     * 该类不自动生成消息id
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
            q = QEnum.GET_PEERS.getCode();
            a = new RequestContent();
        }

        /**
         * 指定请求发送方nodeID/ 要查找的nodeId构造
         */
        public Request(String nodeId,String info_hash,String messageId) {
            init();
            t = messageId;
            a.id = nodeId;
            a.info_hash = info_hash;
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

        /**
         * 回复方定义的token
         */
        private String token;

        /**
         * 当有该种子时,回复的是values,没有时,回复的是nodes.
         */
        private String nodes;
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
         * 指定回复方id/ token/ nodes
         */
        public Response(String nodeId,String token,String nodes,String messageId) {
            init();
            r.id = nodeId;
            r.token = token;
            r.nodes = nodes;
            t = messageId;
        }
    }
}
