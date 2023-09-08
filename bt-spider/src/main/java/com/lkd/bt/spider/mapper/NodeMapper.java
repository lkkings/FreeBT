package com.lkd.bt.spider.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lkd.bt.spider.entity.Node;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * node Mapper 接口
 * </p>
 *
 * @author lkkings
 * @since 2023-09-03
 */
@Mapper
public interface NodeMapper extends BaseMapper<Node> {
    List<String> findTopNode(@Param("top") int top);
}
