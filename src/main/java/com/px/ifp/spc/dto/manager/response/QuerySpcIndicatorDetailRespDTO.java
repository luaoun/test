package com.px.ifp.spc.dto.manager.response;

import com.px.ifp.common.bean.common.BaseBean;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-16
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QuerySpcIndicatorDetailRespDTO extends BaseBean {
    @Schema(description = "指标id")
    private Long id;

    @Schema(description = "作业id")
    private String jobId;

    @Schema(description = "指标名称")
    private String indicatorName;

    @Schema(description = "所属课室")
    private String classCode;

    @Schema(description = "课室名称")
    private String className;

    @Schema(description = "所属系统")
    private String systemCode;

    @Schema(description = "所属系统名称")
    private String systemName;

    @Schema(description = "指标级别")
    private String indicatorLevel;

    @Schema(description = "点位")
    @Deprecated
    private String point;

    @Schema(description = "点位单位")
    private String pointUnit;

    @Schema(description = "Y轴步长值")
    @Deprecated
    private BigDecimal step;

    @Schema(description = "Y轴起始值")
    @Deprecated
    private BigDecimal startValue;

    @Schema(description = "Y轴最大值")
    @Deprecated
    private BigDecimal endValue;

    @Schema(description = "目标值")
    private BigDecimal targetValue;

    @Schema(description = "UCL值")
    private BigDecimal uclValue;

    @Schema(description = "LCL值")
    private BigDecimal lclValue;

    @Schema(description = "UWL值")
    private BigDecimal uwlValue;

    @Schema(description = "LWL值")
    private BigDecimal lwlValue;

    @Schema(description = "USL值")
    private BigDecimal uslValue;

    @Schema(description = "LSL值")
    private BigDecimal lslValue;

    @Schema(description = "U3L值")
    private BigDecimal u3lValue;

    @Schema(description = "L3L值")
    private BigDecimal l3lValue;

    @Schema(description = "自控线上限值")
    private BigDecimal uaclValue;

    @Schema(description = "自控线下限值")
    private BigDecimal laclValue;

    @Schema(description = "绑定ocap流程模版名称")
    @Deprecated
    private String ocap;

    @Schema(description = "创建时间")
    private Date createTime;

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
    private BigDecimal yAxisMin;

    @Schema(description = "Y轴显示最大值")
    private BigDecimal yAxisMax;

    @Schema(description = "Y轴刻度步长")
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

    @Schema(description = "SPC采样策略总开关")
    private Boolean enabledSamplingStatus;

    @Schema(description = "SPC监控总开关")
    private Boolean enabledSpcControlled;

    @Schema(description = "逻辑删除标识")
    private Boolean deleted = false;

    @Schema(description = "扩展属性（自定义字段）")
    private String attributes;

    @Schema(description = "创建人账号")
    private String createdBy;

    @Schema(description = "更新人账号")
    private String updatedBy;
}
