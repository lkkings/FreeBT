package com.lkd.bt.spider;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class GetIPAddress {

    public static void main(String[] args) {
        try {
            // 获取本机的 InetAddress 对象
            InetAddress localhost = InetAddress.getLocalHost();

            // 获取本机的 IP 地址
            String ipAddress = localhost.getHostAddress();
            System.out.println("IP Address: " + ipAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
