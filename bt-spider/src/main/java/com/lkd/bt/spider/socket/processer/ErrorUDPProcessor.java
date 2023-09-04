package com.lkd.bt.spider.socket.processer;

import com.lkd.bt.spider.enums.YEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Created by lkkings on 2023/8/24
 * 异常回复  处理器
 * 使其优先级比其他处理器高
 */
@Order(0)
@Slf4j
@Component
public class ErrorUDPProcessor extends UDPProcessor {
	private static final String LOG = "[ERROR_PROCESS]";

	@Override
	boolean process1(Process process) {
		log.error("{}对方节点:{},回复异常信息:{}", LOG, process.getSender(), process.getRawMap());
		return true;
	}

	@Override
	boolean isProcess(Process process) {
		return YEnum.ERROR.equals(process.getMessage().getStatus());
	}
}
