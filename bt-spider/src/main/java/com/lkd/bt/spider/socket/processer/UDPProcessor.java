package com.lkd.bt.spider.socket.processer;

import com.lkd.bt.common.exception.BTException;
import com.lkd.bt.spider.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by lkkings on 2023/8/24
 * udp处理器接口
 */
@Slf4j
@Component
public abstract class UDPProcessor {

	protected static Config config;
	protected static List<String> nodeIds;

	@Autowired
	public void init(Config config) {
		UDPProcessor.config = config;
		UDPProcessor.nodeIds = config.getMain().getNodeIds();
	}

	/**
	 * 下一个处理器
	 */
	private UDPProcessor next;

	/**
	 * 处理请求模版方法
	 * 不可重写
	 */
	final boolean process(Process process) {
		//此处next为空时直接返回false
		if(!isProcess(process))
			return next != null && next.process(process);
		try {
			return process1(process);
		}catch (BTException e) {
			log.error("[处理异常]e:{}",e.getMessage());
		} catch (Exception e) {
			log.error("[处理异常]e:{}",e.getMessage(),e);
		}
		return false;
	}

	/**
	 * 处理请求 真正的处理方法
	 */
	abstract boolean process1(Process process);

	/**
	 * 是否使用该处理器
	 */
	abstract boolean isProcess(Process process);

	/**
	 * 设置下一个处理器
	 */
	void setNext(UDPProcessor udpProcessor){
		this.next = udpProcessor;
	}
}
