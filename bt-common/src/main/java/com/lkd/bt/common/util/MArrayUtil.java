package com.lkd.bt.common.util;

/**
 * Created by lkkings on 2023/8/25
 */

public class MArrayUtil {
    public static int indexOf(byte[] bytes,byte target,int start){
        for (int i = start; i < bytes.length; i++) {
            if (bytes[i] == target) return i;
        }
        return -1;
    }

    public static int indexOf(byte[] bytes,byte target){
        return indexOf(bytes,target,0);
    }
}
