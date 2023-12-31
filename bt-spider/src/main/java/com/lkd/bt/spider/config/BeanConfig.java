package com.lkd.bt.spider.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.lkd.bt.spider.constant.RedisConstant;
import com.lkd.bt.spider.dto.GetPeersSendInfo;
import com.lkd.bt.spider.entity.Node;
import com.lkd.bt.spider.socket.RoutingTable;
import lombok.SneakyThrows;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Constants;
import org.springframework.core.io.ClassPathResource;

import java.net.InetSocketAddress;
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

	@Bean(name = RedisConstant.GET_PEER_SEND_INFO)
	public RMapCache<String, GetPeersSendInfo> rMapCache(RedissonClient redisson){
		return redisson.getMapCache(RedisConstant.GET_PEER_SEND_INFO);
	}

	@Bean(name = RedisConstant.INFO_HASH_FILTER)
	public RHyperLogLog<String> rHyperLogLog(RedissonClient redisson){
		return redisson.getHyperLogLog(RedisConstant.INFO_HASH_FILTER);
	}


	@Bean(name = RedisConstant.FIND_NODE_TASK_QUEUE)
	public RBlockingQueue <Node> findNodeTaskRQueue(RedissonClient redisson){
		return redisson.getBlockingQueue(RedisConstant.FIND_NODE_TASK_QUEUE);
	}


	@Bean(name = RedisConstant.GET_PEERS_TASK_QUEUE)
	public RBlockingQueue <String> getPeersTaskRQueue(RedissonClient redisson){
		return redisson.getBlockingQueue(RedisConstant.GET_PEERS_TASK_QUEUE);
	}
}
