package com.lkd.bt.spider.socket.processer;

import com.lkd.bt.spider.enums.QEnum;
import com.lkd.bt.spider.enums.YEnum;
import com.lkd.bt.spider.socket.Sender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Created by lkkings on 2023/8/24
 * ping 请求 处理器
 */
@Order(6)
@Slf4j
@Component
public class PingRequestUDPProcessor  extends UDPProcessor {

	private final Sender sender;

	public PingRequestUDPProcessor(Sender sender) {
		this.sender = sender;
	}

	@Override
	boolean process1(Process process) {
		this.sender.pingReceive(process.getSender(), nodeIds.get(process.getIndex()),
				process.getMessage().getMessageId(),process.getIndex());
		return true;
	}

	@Override
	boolean isProcess(Process process) {
		return QEnum.PING.equals(process.getMessage().getMethod()) && YEnum.QUERY.equals(process.getMessage().getStatus());
	}
}
