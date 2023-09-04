package com.lkd.bt.spider.dto.bt.base;

import lombok.Data;

/**
 * Created by lkkings on 2023/8/24
 * 通用参数
 */
@Data
public class CommonParam {
    /**
     * 消息ID
     */
    protected String t;
    /**
     * 状态(请求/回复/异常)
     */
    protected String y;
}
