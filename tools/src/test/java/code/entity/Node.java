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
 * node
 * </p>
 *
 * @author lkkings
 * @since 2023-09-03
 */
@Getter
@Setter
@TableName("node")
@ApiModel(value = "Node对象", description = "node")
public class Node implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("node_id")
    @TableField("node_id")
    private String nodeId;

    @ApiModelProperty("ip")
    @TableField("ip")
    private String ip;

    @ApiModelProperty("ports")
    @TableField("port")
    private Integer port;

    @ApiModelProperty("创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @ApiModelProperty("修改时间")
    @TableField("update_time")
    private Date updateTime;


}
