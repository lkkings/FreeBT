package com.lkd.bt.spider.dto.bt.base;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by lkkings on 2023/8/24
 * 通用请求参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CommonRequest extends CommonParam{
    /**方法(ping/find_node等)*/
    protected String q;
}
