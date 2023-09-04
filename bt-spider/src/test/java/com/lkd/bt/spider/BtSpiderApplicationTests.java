package com.lkd.bt.spider;

import com.lkd.bt.spider.socket.processer.UDPProcessor;
import com.lkd.bt.spider.socket.processer.UDPProcessorManager;
import com.lkd.bt.spider.task.InitTask;
import com.lkd.bt.spider.util.Bencode;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class BtSpiderApplicationTests {
	@Resource
	InitTask initTask;

	@Resource
	Bencode bencode;

	@Test
	void contextLoads() {
		Map<String,Object> map = new HashMap<>(4);
		map.put("t","aa");
		map.put("y","q");
		map.put("q","ping");
		Map<String,Object> amap = new HashMap<>(2);
		amap.put("id","abcdefghij0123456789");
		map.put("a",amap);
		String s = bencode.encodeDict(map);
		System.out.println(s);
		String s1 = "d1:y1:q1:q4:ping1:ad2:id20:abcdefghij0123456789e1:t2:aae";
		Map decode = bencode.decode(bencode.toBytes(s1), Map.class);
		System.out.println();
	}

	@Test
	void initTaskTest(){
		initTask.run();
	}

}
