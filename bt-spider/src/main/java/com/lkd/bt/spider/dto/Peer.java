package com.lkd.bt.spider.dto;

import cn.hutool.core.util.ArrayUtil;
import com.lkd.bt.common.exception.BTException;
import com.lkd.bt.common.util.CodeUtil;
import com.lkd.bt.spider.config.Config;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Created by lkkings on 2023/9/3
 */

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class Peer {

    private String ip;

    private Integer port;

    /**
     * byte[6] 转 Node
     */
    public Peer(byte[] bytes) {
        if (bytes.length != Config.PEER_BYTES_LEN)
            throw new BTException("转换为Peer需要bytes长度为6,当前为:" + bytes.length);
        //ip
        ip = CodeUtil.bytes2Ip(ArrayUtil.sub(bytes, 0, 4));

        //ports
        port = CodeUtil.bytes2Port(ArrayUtil.sub(bytes, 4, 6));
    }
}

