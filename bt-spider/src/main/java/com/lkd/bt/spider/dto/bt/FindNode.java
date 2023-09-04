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
 * find_node方法请求&回复
 * find_node Query = {"t":"aa", "y":"q", "q":"find_node", "a": {"id":"abcdefghij0123456789", "target":"mnopqrstuvwxyz123456"}}
 * bencoded = d1:ad2:id20:abcdefghij01234567896:target20:mnopqrstuvwxyz123456e1:q9:find_node1:t2:aa1:y1:qe
 * Response = {"t":"aa", "y":"r", "r": {"id":"0123456789abcdefghij", "nodes": "def456..."}}
 * bencoded = d1:rd2:id20:0123456789abcdefghij5:nodes9:def456...e1:t2:aa1:y1:re
 **/
public abstract class FindNode {

    /**
     * 主体
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
         * 要查找的nodeID
         */
        private String target;
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

        /**主体,包含请求发送方的nodeID*/
        private RequestContent a = new RequestContent();

        private void init() {
            t = BTUtil.generateMessageID();
            y = YEnum.QUERY.getCode();
            q = QEnum.FIND_NODE.getCode();
        }

        /**
         * 指定请求发送方nodeID/ 要查找的nodeId构造
         */
        public Request(String nodeId,String targetNodeId) {
            init();
            a.id = nodeId;
            a.target = targetNodeId;
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
         * 与要查找的nodeId最接近的8个node的nodeIds
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

        /**主体,包含请求发送方的nodeID(也就是自己的)*/
        private ResponseContent r;

        private void init() {
            y = YEnum.RECEIVE.getCode();
            r = new ResponseContent();
        }

        /**
         * 指定请回复方nodeID/ nodes
         */
        public Response(String nodeId,String nodes,String messageId) {
            init();
            r.id = nodeId;
            r.nodes = nodes;
            t = messageId;
        }
    }
}
