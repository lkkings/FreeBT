package com.lkd.bt.spider.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lkd.bt.common.entity.Metadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Metadata Mapper 接口
 * </p>
 *
 * @author lkkings
 * @since 2023-08-26
 */
@Mapper
public interface MetadataMapper extends BaseMapper<Metadata> {
    List<String> getInfoHashByPage(@Param("start") int start, @Param("end") int end);
}
