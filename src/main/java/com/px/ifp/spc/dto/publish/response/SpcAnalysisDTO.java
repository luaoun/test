package com.px.ifp.spc.dto.publish.response;

import com.px.ifp.common.bean.common.BaseBean;
import com.px.ifp.spc.bo.AlarmXis;
import com.px.ifp.spc.bo.CommonStat;
import com.px.ifp.spc.bo.CommonTimeStat;
import com.px.ifp.spc.dto.manager.request.SamplingStrategyDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpcAnalysisDTO extends BaseBean {
    @Schema(description = "指标id")
    private Long id;

    @Schema(description = "spc编码（可使用数组字典的字典项编码，与spcJob做映射使用）")
    private String spcCode;

    @Schema(description = "作业id")
    private String jobId;

    @Schema(description = "科室编码")
    private String classCode;

    @Schema(description = "科室名称")
    private String className;

    @Schema(description = "子系统编码")
    private String systemCode;

    @Schema(description = "子系统名称")
    private String systemName;

    @Schema(description = "指标级别")
    private String indicatorLevel;

    @Schema(description = "指标ID")
    private Long indicatorId;

    @Schema(description = "指标名称")
    private String indicatorName;

    @Deprecated
    @Schema(description = "点位")
    private String point;

    @Schema(description = "指标编码")
    private String measureCode;

    @Deprecated
    @Schema(description = "点位名称")
    private String pointName;

    @Schema(description = "指标名称")
    private String measureName;

    @Schema(description = "点位单位")
    private String pointUnit;

    @Deprecated
    @Schema(description = "步长")
    private BigDecimal step;

    @Schema(description = "步长")
    private BigDecimal yAxisStep;

    @Deprecated
    @Schema(description = "Y轴起始值")
    private BigDecimal startValue;

    @Schema(description = "Y轴起始值")
    private BigDecimal yAxisMin;

    @Deprecated
    @Schema(description = "Y轴结束值")
    private BigDecimal endValue;

    @Schema(description = "Y轴结束值")
    private BigDecimal yAxisMax;

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

    @Schema(description = "最大值")
    private BigDecimal maxValue;

    @Schema(description = "最小值")
    private BigDecimal minValue;

    @Schema(description = "平均值")
    private BigDecimal avgValue;

    @Schema(description = "cp值")
    private BigDecimal cpValue;

    @Schema(description = "cpk值")
    private BigDecimal cpkValue;

    @Schema(description = "3σ值")
    private BigDecimal sigma3Value;

    @Schema(description = "oow数量")
    private Integer oowCount = 0;

    @Schema(description = "ooc数量")
    private Integer oocCount = 0;

    @Schema(description = "oos数量")
    private Integer oosCount = 0;

    @Schema(description = "oo3数量")
    private Integer oo3Count = 0;

    @Schema(description = "点位值列表")
    private List<CommonTimeStat> pointValues = new ArrayList<>();

    @Schema(description = "点位值列表（日期已格式化）")
    private List<CommonStat<BigDecimal>> formatPointValues = new ArrayList<>();

    @Schema(description = "是否禁用")
    private Boolean status;

    List<AlarmXis> alarmTags;

    @Schema(description = "采样策略配置列表")
    private List<SamplingStrategyDTO> samplingStrategies;
}
