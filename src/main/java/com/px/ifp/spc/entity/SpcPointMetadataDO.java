package com.px.ifp.spc.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * SPC点位元数据实体类 (V2)
 * 对应数据库表: spc_point_metadata
 *
 * 重要说明：
 * 1. 使用 @TableField 注解映射 DB列名 与 Java字段名
 * 2. Java字段名保持不变（与V1 SpcIndicatorDO一致），最小化代码改动
 * 3. 新增字段：pointName, areaCode, enabled, status等
 * 4. 已删除字段：startValue, step, endValue, u3lValue, l3lValue, configSource, configType
 */
@Data
@TableName("spc_point_metadata")
public class SpcPointMetadataDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // ============================================================
    // 指标标识字段（字段名映射）
    // ============================================================

    @TableField("measure_code")
    private String measureCode;

    @TableField("measure_name")
    private String measureName;

    @TableField("fac_code")
    private String facCode;

    @TableField("area_code")
    private String areaCode;

    // ============================================================
    // 业务分类字段
    // ============================================================

    @TableField("job_id")
    private String jobId;

    @TableField("indicator_name")
    private String indicatorName;

    @TableField("indicator_level")
    private String indicatorLevel;

    @TableField("class_code")
    private String classCode;

    @TableField("system_code")
    private String systemCode;

    // ============================================================
    // SPC指标属性字段（字段名映射）
    // ============================================================

    @TableField("unit")              // DB列名: unit
    private String pointUnit;         // Java字段名: pointUnit (保持原名)

    @TableField("data_type")         // DB列名: data_type (新增字段) 枚举：analog模拟量（连续变化的数据）/digital数字量（状态，数字等值）
    private String dataType;

    @TableField("precision_decimal") // DB列名: precision_decimal (新增字段)
    private Integer precisionDecimal;

    @TableField("range_min")         // DB列名: range_min (新增字段)  量程最小范围
    private BigDecimal rangeMin;

    @TableField("range_max")         // DB列名: range_max (新增字段)  量程最大范围
    private BigDecimal rangeMax;

    // ============================================================
    // Y轴显示配置（新增字段）
    // ============================================================

    @TableField("y_axis_min")
    private BigDecimal yAxisMin;

    @TableField("y_axis_max")
    private BigDecimal yAxisMax;

    @TableField("y_axis_step")
    private BigDecimal yAxisStep;

    // ============================================================
    // SPC 开关字段（新增）
    // ============================================================

    @TableField("enable_realtime_alarm")   // DB列名: enable_realtime_alarm
    private Boolean enableRealtimeAlarm;   // 是否启用实时SPC

    @TableField("enable_offline_chart")    // DB列名: enable_offline_chart
    private Boolean enableOfflineChart;    // 是否允许创建离线控制图


    // ============================================================
    // SPC控制限字段（V1告警策略，字段名映射）
    // ============================================================

    @TableField("target_value")
    private BigDecimal targetValue;

    @TableField("ucl_value")
    private BigDecimal uclValue;

    @TableField("lcl_value")
    private BigDecimal lclValue;

    @TableField("uwl_value")
    private BigDecimal uwlValue;

    @TableField("lwl_value")
    private BigDecimal lwlValue;

    @TableField("usl_value")
    private BigDecimal uslValue;

    @TableField("lsl_value")
    private BigDecimal lslValue;

    @TableField("u3l_value")
    private BigDecimal u3lValue;

    @TableField("l3l_value")
    private BigDecimal l3lValue;

    // ============================================================
    // 设备关联字段（新增字段）
    // ============================================================

    @TableField("equipment_id")
    private String equipmentId;

    @TableField("equipment_name")
    private String equipmentName;

    @TableField("ocap_template_id")
    private String ocapTemplateId;

    // ============================================================
    // 状态字段
    // ============================================================

    @TableField("enabled_spc_controlled")            // DB列名: enabled_spc_controlled (新增字段)
    private Boolean enabledSpcControlled;

    @TableLogic
    @TableField("deleted_id")
    private Long deletedId;

    @TableField("tags")
    private String tags;              // 标签（多个标签用逗号分隔）

    // ============================================================
    // 扩展属性（新增字段）
    // ============================================================

    @TableField("attributes")
    private String attributes;        // JSON字符串 属性扩展字段

    // ============================================================
    // 时间戳字段（字段名映射）
    // ============================================================

    @TableField("created_by")        // DB列名: created_by
    private String createdBy;           // Java字段名: creator (保持原名)

    @TableField("created_at")        // DB列名: created_at
    private Date createdAt;          // Java字段名: createTime (保持原名)

    @TableField("updated_at")        // DB列名: updated_at
    private Date updatedAt;          // Java字段名: updateTime (保持原名)

    @TableField("updated_by")        // DB列名: updated_by (新增字段)
    private String updatedBy;

    @TableField("config_type")
    private String configType;

    @TableField("is_default")
    private String isDefault;

    // ============================================================
    // V1字段已删除（不再使用）
    // ============================================================
    // - startValue (起始值) - 图表渲染字段 被yAxisMin 替代
    // - step (步长) - 图表渲染字段 被yAxisStep替代
    // - endValue (结束值) - 图表渲染字段 被yAxisMax 替代
    // - u3lValue (上3σ线) - 被R1规则替代
    // - l3lValue (下3σ线) - 被R1规则替代
    // - configSource (配置来源) - 迁移到 spc_chart_config.limit_calc_mode
    // - configType (配置类型) - 迁移到 spc_chart_config.chart_type
}
