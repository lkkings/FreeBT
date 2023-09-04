package service.impl;

import entity.Metadata;
import mapper.MetadataMapper;
import service.IMetadataService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Metadata 服务实现类
 * </p>
 *
 * @author lkkings
 * @since 2023-08-26
 */
@Service
public class MetadataServiceImpl extends ServiceImpl<MetadataMapper, Metadata> implements IMetadataService {

}
