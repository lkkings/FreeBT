package service.impl;

import entity.Node;
import mapper.NodeMapper;
import service.INodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
