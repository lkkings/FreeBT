package com.lkd.bt.spider.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * info_hash
 * </p>
 *
 * @author lkkings
 * @since 2023-09-03
 */
@Getter
@Setter
@Builder
@TableName("info_hash")
@ApiModel(value = "InfoHash对象", description = "info_hash")
public class InfoHash implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("info_hash")
    @TableField("info_hash")
    private String infoHash;

    @ApiModelProperty("peer地址, ip:port形式")
    @TableField("peer_address")
    private String peerAddress;

    @ApiModelProperty("创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @ApiModelProperty("修改时间")
    @TableField("update_time")
    private Date updateTime;


}
