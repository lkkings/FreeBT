package com.lkd.bt.spider.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lkd.bt.spider.entity.Node;
import com.lkd.bt.spider.mapper.NodeMapper;
import com.lkd.bt.spider.service.INodeService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * node 服务实现类
 * </p>
 *
 * @author lkkings
 * @since 2023-09-03
 */
@Service
public class NodeServiceImpl extends ServiceImpl<NodeMapper, Node> implements INodeService {

}
