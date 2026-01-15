package com.px.ifp.spc.dto.publish.response;

import com.px.ifp.spc.bo.ExceptionBO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class SpcAnalysisResultNoteDTO {
    @Schema(description = "指标分析结果id")
    private int id;

    @Schema(description = "作业id")
    private String jobId;

    @Schema(description = "子系统编码")
    private String systemCode;

    @Schema(description = "子系统名称")
    private String systemName;

    @Schema(description = "spc指标主键ID")
    private int indicatorId;

    @Schema(description = "spc指标名称")
    private String indicatorName;

    @Schema(description = "点位")
    private String point;

    @Schema(description = "科室编码")
    private String classCode;

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

    @Schema(description = "oos数量")
    private Integer oos;

    @Schema(description = "ooc数量")
    private Integer ooc;

    @Schema(description = "oow数量")
    private Integer oow;

    @Schema(description = "oo3数量")
    private Integer oo3;

    @Schema(description = "批注内容")
    private String content;

    @Schema(description = "指标级别")
    private String indicatorLevel;

    @Schema(description = "点位单位")
    private String pointUnit;
    @Schema(description = "Y轴起始值")
    private BigDecimal startValue;
    @Schema(description = "y轴步长")
    private BigDecimal step;
    @Schema(description = "目标值")
    private BigDecimal targetValue;
    @Schema(description = "Y轴最大值")
    private BigDecimal endValue;
    @Schema(description = "事件发生时间")
    private Date eventTime;
    @Schema(description = "SPC异常内容（异常值、异常发生时间、批注内容）")
    private List<ExceptionBO> exceptionList;

}
