package com.lkd.bt.spider.dto;

import com.lkd.bt.spider.enums.QEnum;
import com.lkd.bt.spider.enums.YEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Created by lkkings on 2023/8/24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Message {
    /**
     * 方法
     */
    private QEnum method;

    /**
     * 状态
     */
    private YEnum status;

    /**
     * 消息id
     */
    private String messageId;

}
