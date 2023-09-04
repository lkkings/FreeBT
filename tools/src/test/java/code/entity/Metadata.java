package entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

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

    @ApiModelProperty("类型: 0:从peer处获取; 1:从www.zhongzidi.com获取;")
    @TableField("type")
    private Integer type;

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
