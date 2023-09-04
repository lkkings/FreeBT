package com.lkd.bt.spider.enums;

import com.lkd.bt.common.enums.CodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by lkkings on 2023/8/24
 * krpc查询方法枚举
 */
@AllArgsConstructor
@Getter
public enum QEnum implements CodeEnum<String> {
    PING("ping", "用来侦探对方是否在线"),
    FIND_NODE("find_node", "查找目标主机"),
    GET_PEERS("get_peers", "查找拥有某种子的所有目标主机"),
    ANNOUNCE_PEER("announce_peer", "通知其他主机,该主机有对某种子的上传下载"),
    ;

    private final String code;
    private final String message;
}
