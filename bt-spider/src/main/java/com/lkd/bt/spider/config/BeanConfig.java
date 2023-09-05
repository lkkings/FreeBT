package com.lkd.bt.spider.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.lkd.bt.spider.socket.RoutingTable;
import com.lkd.bt.spider.socket.Sender;
import com.lkd.bt.spider.socket.processer.UDPProcessor;
import com.lkd.bt.spider.socket.processer.UDPProcessorManager;
import com.lkd.bt.spider.task.FindNodeTask;
import com.lkd.bt.spider.util.Bencode;
import lombok.SneakyThrows;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lkkings on 2023/8/24
 * 注入bean
 */
@Configuration
public class BeanConfig {
	/**
	 * 初始化配置的所有节点id
	 */
	@Autowired
	public void initConfigNodeIds(Config config) {
		config.getMain().initNodeIds();
	}

	/**
	 *注入json处理工具
	 */
	@Bean
	public ObjectMapper objectMapper(){
		return new ObjectMapper();
	}

	/**
	 * DHT服务端处理器
	 */
	@Bean
	public List<FindNodeTask.UDPServerHandler> UdpServerHandlers(Bencode bencode, Config config,
																		 UDPProcessorManager udpProcessorManager,
																		 Sender sender){
		int size = config.getMain().getNodeIds().size();
		List<FindNodeTask.UDPServerHandler> udpServerHandlers = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			udpServerHandlers.add(new FindNodeTask.UDPServerHandler(i, bencode, udpProcessorManager, sender));
		}
		return udpServerHandlers;
	}

	/**
	 * UDP处理器管理器
	 */
	@Bean
	public UDPProcessorManager udpProcessorManager(List<UDPProcessor> udpProcessors) {
		UDPProcessorManager udpProcessorManager = new UDPProcessorManager();
		udpProcessors.forEach(udpProcessorManager::register);
		return udpProcessorManager;
	}

	/**
	 * 创建多个路由表
	 */
	@Bean
	public List<RoutingTable> routingTables(Config config) {
		List<Integer> ports = config.getMain().getPorts();
		List<RoutingTable> result = new ArrayList<>(ports.size());
		List<String> nodeIds = config.getMain().getNodeIds();
		for (int i = 0; i < ports.size(); i++) {
			result.add(new RoutingTable(config, nodeIds.get(i).getBytes(), ports.get(i)));
		}
		return result;
	}
	/**
	 * 创建redisson客户端
	 */
	@Bean
	@SneakyThrows
	public RedissonClient redissonClient(){
		// 加载 YAML 配置文件
		ClassPathResource configFile = new ClassPathResource("redisson-config.yaml");
		org.redisson.config.Config config = org.redisson.config.Config.fromYAML(configFile.getInputStream());
	    return Redisson.create(config);
	}

}
