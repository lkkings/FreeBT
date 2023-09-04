package com.lkd.bt.spider.socket.processer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created by lkkings on 2023/8/24
 * 处理器管理器
 */
@Slf4j
@Component
public class UDPProcessorManager {

	/**
	 * 第一个处理器
	 */
	private UDPProcessor first;

	/**
	 * 最后一个处理器
	 */
	private UDPProcessor last;

	/**
	 * 注册
	 */
	public void register(UDPProcessor processor) {
		if (first == null) {
			first = last = processor;
			return;
		}
		last.setNext(processor);
		last = processor;
	}

	/**
	 * 处理请求
	 */
	public boolean process(Process process) {
		return first.process(process);
	}
}
