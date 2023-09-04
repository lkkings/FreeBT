package com.lkd.bt.common.util;

import java.util.Random;

/**
 * 随机工具类
 */

public class RandomUtil {
    public static byte[] simpleNextBytes(int len) {
        byte[] nodeId = new byte[len];
        new Random().nextBytes(nodeId);
        return nodeId;
    }
}
