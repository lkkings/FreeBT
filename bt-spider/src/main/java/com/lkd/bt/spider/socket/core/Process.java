package com.lkd.bt.spider.socket.core;

import com.lkd.bt.spider.dto.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * Created by lkkings on 2023/8/24
 * 处理对象
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class Process {

	/**
	 * 消息信息
	 */
	private Message message;

	/**
	 * 原始map
	 */
	private Map<String,Object> rawMap;

	/**
	 * 消息发送者
	 */
	private InetSocketAddress sender;

	/**
	 * index
	 */
	private int index;
}
