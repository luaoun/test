package com.px.ifp.spc.dto.manager.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.px.ifp.common.bean.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-15
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AddSpcPointMetaReqDTO extends BaseRequest {
    @Schema(description = "指标名称")
    @NotEmpty(message = "指标名称不能为空")
    private String indicatorName;

    @Schema(description = "所属课室")
    @NotEmpty(message = "所属课室不能为空")
    private String classCode;

    @Schema(description = "所属系统")
    @NotEmpty(message = "所属系统不能为空")
    private String systemCode;

    @Schema(description = "指标级别")
    @NotEmpty(message = "指标级别不能为空")
    private String indicatorLevel;

    @Schema(description = "点位（不建议使用）")
    @Deprecated
    private String point;

    @Schema(description = "点位单位（不建议使用）")
    @Deprecated
    private String pointUnit;

    @Schema(description = "步长（不建议使用）")
    @Deprecated
    private BigDecimal step;

    @Schema(description = "Y轴起始值（不建议使用）")
    @Deprecated
    private BigDecimal startValue;

    @Schema(description = "Y轴最大值（不建议使用）")
    @Deprecated
    private BigDecimal endValue;

    @Schema(description = "目标值")
    private BigDecimal targetValue;

    @Schema(description = "控制线上限值")
    private BigDecimal uclValue;

    @Schema(description = "控制线下限值")
    private BigDecimal lclValue;

    @Schema(description = "警告线上限值")
    private BigDecimal uwlValue;

    @Schema(description = "警告线下限值")
    private BigDecimal lwlValue;

    @Schema(description = "规格线上限值")
    private BigDecimal uslValue;

    @Schema(description = "规格线下限值")
    private BigDecimal lslValue;

    @Schema(description = "3σ上限值")
    private BigDecimal u3lValue;

    @Schema(description = "3σ下限值")
    private BigDecimal l3lValue;

    @Schema(description = "自控线上限值")
    private BigDecimal uaclValue;

    @Schema(description = "自控线下值")
    private BigDecimal laclValue;

    @Schema(description = "绑定ocap流程模版名称")
    @Deprecated
    private String ocap;

    @Schema(description = "配置类型(USER/SYSTEM)")
    @NotNull(message = "配置类型必填(USER/SYSTEM)")
    private String configType;

    @Schema(description = "是否禁用")
    @Deprecated
    private Boolean status;

    // ============================================================
    // 新增参数
    // ============================================================
    @Schema(description = "指标编码")
    private String measureCode;

    @Schema(description = "区域编码")
    private String areaCode;

    @Schema(description = "数据类型（模拟量：analog, 数字量：digital）")
    private String dataType;

    @Schema(description = "精度")
    private Integer precisionDecimal;

    @Schema(description = "量程最小值")
    private BigDecimal rangeMin;

    @Schema(description = "量程最大值")
    private BigDecimal rangeMax;

    @Schema(description = "Y轴显示最小值")
    @JsonProperty("yAxisMin")
    private BigDecimal yAxisMin;

    @Schema(description = "Y轴显示最大值")
    @JsonProperty("yAxisMax")
    private BigDecimal yAxisMax;

    @Schema(description = "Y轴刻度步长")
    @JsonProperty("yAxisStep")
    private BigDecimal yAxisStep;

    @Schema(description = "是否启用实时SPC阀值告警")
    private Boolean enableRealtimeAlarm;

    @Schema(description = "是否允许创建离线控制图")
    private Boolean enableOfflineChart;

    @Schema(description = "设备编码")
    private String equipmentId;

    @Schema(description = "设备名称")
    private String equipmentName;

    @Schema(description = "spc指标指标所属部门")
    private String department;

    @Schema(description = "ocap模版ID")
    private String ocapTemplateId;

    @Schema(description = "SPC监控总开关")
    private Boolean enabledSpcControlled;

    @Schema(description = "逻辑删除标识")
    private Boolean deleted = false;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "扩展属性（自定义字段）")
    private String attributes;

    @Schema(description = "创建人账号")
    private String createdBy;

    @Schema(description = "更新人账号")
    private String updatedBy;

    @Schema(description = "采样策略配置列表")
    private List<SamplingStrategyDTO> samplingStrategies;

}
