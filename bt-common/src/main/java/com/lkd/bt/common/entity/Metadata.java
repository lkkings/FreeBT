package com.lkd.bt.common.entity;

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
 * Metadata
 * </p>
 *
 * @author lkkings
 * @since 2023-08-26
 */
@Getter
@Setter
@Builder
@TableName("metadata")
@ApiModel(value = "Metadata对象", description = "Metadata")
public class Metadata implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("info_hash")
    @TableField("info_hash")
    private String infoHash;

    @ApiModelProperty("文件信息,json, infos字段")
    @TableField("files_info")
    private String filesInfo;

    @ApiModelProperty("名字")
    @TableField("name")
    private String name;

    @ApiModelProperty("总长度(所有文件相加长度)")
    @TableField("length")
    private Long length;

    @ApiModelProperty("热度")
    @TableField("hot")
    private Integer hot;

    @ApiModelProperty("创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @ApiModelProperty("修改时间")
    @TableField("update_time")
    private Date updateTime;

}
