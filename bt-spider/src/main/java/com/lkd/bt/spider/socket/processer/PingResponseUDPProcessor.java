package com.lkd.bt.spider.socket.processer;

import com.lkd.bt.spider.enums.QEnum;
import com.lkd.bt.spider.enums.YEnum;
import com.lkd.bt.spider.socket.core.Process;
import com.lkd.bt.spider.socket.core.UDPProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by lkkings on 2023/8/24
 * ping 回复 处理器
 */
@Slf4j
//@Component
public class PingResponseUDPProcessor extends UDPProcessor {

	@Override
	public boolean process1(Process process) {

		return true;
	}

	@Override
	public boolean isProcess(Process process) {
		return QEnum.PING.equals(process.getMessage().getMethod()) && YEnum.RECEIVE.equals(process.getMessage().getStatus());
	}
}
