package com.lkd.bt.spider.socket.processer;

import com.lkd.bt.spider.enums.QEnum;
import com.lkd.bt.spider.enums.YEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by lkkings on 2023/8/24
 * ping 回复 处理器
 */
@Slf4j
//@Component
public class PingResponseUDPProcessor extends UDPProcessor {

	@Override
	boolean process1(Process process) {

		return true;
	}

	@Override
	boolean isProcess(Process process) {
		return QEnum.PING.equals(process.getMessage().getMethod()) && YEnum.RECEIVE.equals(process.getMessage().getStatus());
	}
}
