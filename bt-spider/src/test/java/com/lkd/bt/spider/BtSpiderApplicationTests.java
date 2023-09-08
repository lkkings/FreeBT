package com.lkd.bt.spider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lkd.bt.spider.entity.InfoHash;
import com.lkd.bt.spider.service.impl.InfoHashServiceImpl;
import com.lkd.bt.spider.util.Bencode;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class BtSpiderApplicationTests {

	@Resource
	Bencode bencode;

	@Resource
	InfoHashServiceImpl infoHashService;

	@Test
	void contextLoads() {
		InfoHash one = infoHashService.getOne(new LambdaQueryWrapper<InfoHash>().eq(InfoHash::getInfoHash, "09b0f27b102d15958cbfbc71974f112591e4b118"));
		System.out.println();
	}


}
