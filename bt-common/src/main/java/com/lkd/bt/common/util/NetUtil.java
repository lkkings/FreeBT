package com.lkd.bt.common.util;

import java.net.InetSocketAddress;

/**
 * Created by lkkings on 2023/9/8
 */

public class NetUtil {
    public static String toAddress(String ip,int port){
        return ip.concat(":"+port);
    }

    public static InetSocketAddress toSocketAddress(String address){
        String[] ipPort= address.split(":");
        return new InetSocketAddress(ipPort[0],Integer.parseInt(ipPort[1]));
    }
}
