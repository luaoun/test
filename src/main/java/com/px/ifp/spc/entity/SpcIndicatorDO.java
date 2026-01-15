package com.px.ifp.spc.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-08
 */
@Data
@TableName("spc_indicator")
@Schema(description = "SPC指标设定表")
public class SpcIndicatorDO implements Serializable {
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "作业id")
    private String jobId;

    @Schema(description = "指标名称")
    private String indicatorName;

    @Schema(description = "所屬科室")
    private String classCode;

    @Schema(description = "所屬系統")
    private String systemCode;

    @Schema(description = "指标级别")
    private String indicatorLevel;

    @Schema(description = "点位")
    private String point;

    @Schema(description = "点位单位")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String pointUnit;

    @Schema(description = "步长")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal step;

    @Schema(description = "Y轴起始值")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal startValue;

    @Schema(description = "Y轴最大值")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal endValue;

    @Schema(description = "目标值")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal targetValue;

    @Schema(description = "控制线上限值")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal uclValue;

    @Schema(description = "控制线下限值")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal lclValue;

    @Schema(description = "警告线上限值")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal uwlValue;

    @Schema(description = "警告线下限值")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal lwlValue;

    @Schema(description = "规格线上限值")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal uslValue;

    @Schema(description = "规格线下限值")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal lslValue;

    @Schema(description = "3σ上限值")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal u3lValue;

    @Schema(description = "3σ下限值")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal l3lValue;

    @Schema(description = "自控线上限值")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal uaclValue;

    @Schema(description = "自控线下限值")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal laclValue;

    @Schema(description = "绑定OCAP报警流程模版名称")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String ocap;

    @Schema(description = "是否删除")
    private Boolean deleted;

    @Schema(description = "是否禁用")
    private Boolean status;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

    @Schema(description = "厂区")
    @TableField("FAC_CODE")
    private String facCode;

    @Schema(description = "配置类型(USER/SYSTEM)")
    @TableField("config_type")
    private String configType;
}
