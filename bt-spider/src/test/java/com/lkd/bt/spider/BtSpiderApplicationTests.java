package com.lkd.bt.spider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lkd.bt.spider.entity.InfoHash;
import com.lkd.bt.spider.entity.Node;
import com.lkd.bt.spider.service.impl.InfoHashServiceImpl;
import com.lkd.bt.spider.service.impl.NodeServiceImpl;
import com.lkd.bt.spider.util.Bencode;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class BtSpiderApplicationTests {

	@Resource
	Bencode bencode;

	@Resource
	InfoHashServiceImpl infoHashService;

	@Resource
	NodeServiceImpl nodeService;

	@Test
	void contextLoads() {
		List<String> topNode = nodeService.getBaseMapper().findTopNode(8);
		System.out.println();
	}


}
