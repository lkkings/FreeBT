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
 * ping 请求&回复
 * ping Query = {"t":"aa", "y":"q", "q":"ping", "a":{"id":"abcdefghij0123456789"}}
 * bencoded = d1:ad2:id20:abcdefghij0123456789e1:q4:ping1:t2:aa1:y1:qe
 * Response = {"t":"aa", "y":"r", "r": {"id":"mnopqrstuvwxyz123456"}}
 * bencoded = d1:rd2:id20:mnopqrstuvwxyz123456e1:t2:aa1:y1:re
 */
public abstract class Ping {


    /**
     * 主体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Content {
        private String id;
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
        private Content a;

        private void init() {
            t = BTUtil.generateMessageID();
            y = YEnum.QUERY.getCode();
            q = QEnum.PING.getCode();
            a = new Content();
        }

        /**
         * 指定请求发送方nodeID,构造
         */
        public Request(String id) {
            init();
            a.id = id;
        }
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
        /**主体,包含回复者的nodeID*/
        private Content r;

        private void init() {
            y = YEnum.RECEIVE.getCode();
            r = new Content();
        }

        /**
         * 根据回复方nodeID/ 消息id构造
         */
        public Response(String id,String messageId) {
            init();
            t = messageId;
            r.id = id;
        }
    }



}
