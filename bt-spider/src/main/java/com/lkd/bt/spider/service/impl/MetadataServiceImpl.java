package com.lkd.bt.spider.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lkd.bt.common.entity.Metadata;
import com.lkd.bt.spider.mapper.MetadataMapper;
import com.lkd.bt.spider.service.IMetadataService;
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
