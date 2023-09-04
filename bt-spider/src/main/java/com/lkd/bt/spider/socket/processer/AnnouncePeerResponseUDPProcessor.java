package com.lkd.bt.spider.socket.processer;

import com.lkd.bt.spider.enums.QEnum;
import com.lkd.bt.spider.enums.YEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by lkkings on 2023/8/24
 * ANNOUNCE_PEER 回复 处理器
 */
@Slf4j
//@Component
public class AnnouncePeerResponseUDPProcessor extends UDPProcessor {
	private static final String LOG = "[ANNOUNCE_PEER_RECEIVE]";

	@Override
	boolean process1(Process process) {
		log.info("{}TODO",LOG);
		return true;
	}

	@Override
	boolean isProcess(Process process) {
		return QEnum.ANNOUNCE_PEER.equals(process.getMessage().getMethod()) && YEnum.RECEIVE.equals(process.getMessage().getStatus());
	}
}
