package com.px.ifp.spc.bo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.px.ifp.common.bean.common.BaseBean;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExportSpcAnalysisResultExcelBO extends BaseBean {

    @ExcelProperty("科室名称")
    @Schema(description = "科室名称")
    private String className;

    @ExcelProperty("子系统名称")
    @Schema(description = "子系统名称")
    private String systemName;

    @ExcelProperty("指标级别")
    @Schema(description = "指标级别")
    private String indicatorLevel;

    @ExcelProperty("Spc指标名称")
    @Schema(description = "Spc指标名称")
    private String indicatorName;

    @ExcelProperty("指标编码")
    @Schema(description = "指标编码")
    private String point;

    @ExcelProperty("指标单位")
    @Schema(description = "指标单位")
    private String pointUnit;

    @ExcelProperty("目标值")
    @Schema(description = "目标值")
    private BigDecimal targetValue;

    @ExcelProperty("UCL值")
    @Schema(description = "UCL值")
    private BigDecimal uclValue;

    @ExcelProperty("LCL值")
    @Schema(description = "LCL值")
    private BigDecimal lclValue;

    @ExcelProperty("UWL值")
    @Schema(description = "UWL值")
    private BigDecimal uwlValue;

    @ExcelProperty("LWL值")
    @Schema(description = "LWL值")
    private BigDecimal lwlValue;

    @ExcelProperty("USL值")
    @Schema(description = "USL值")
    private BigDecimal uslValue;

    @ExcelProperty("LSL值")
    @Schema(description = "LSL值")
    private BigDecimal lslValue;

    @ExcelProperty("U3L值")
    @Schema(description = "U3L值")
    private BigDecimal u3lValue;

    @ExcelProperty("L3L值")
    @Schema(description = "L3L值")
    private BigDecimal l3lValue;

    @ExcelProperty("自控线上限值")
    @Schema(description = "自控线上限值")
    private BigDecimal uaclValue;

    @ExcelProperty("自控线下限值")
    @Schema(description = "自控线下限值")
    private BigDecimal laclValue;

    @ExcelProperty("最大值")
    @Schema(description = "最大值")
    private BigDecimal maxValue;

    @ExcelProperty("最小值")
    @Schema(description = "最小值")
    private BigDecimal minValue;

    @ExcelProperty("平均值")
    @Schema(description = "平均值")
    private BigDecimal avgValue;

    @ExcelProperty("cp值")
    @Schema(description = "cp值")
    private BigDecimal cpValue;

    @ExcelProperty("cpk值")
    @Schema(description = "cpk值")
    private BigDecimal cpkValue;

    @ExcelProperty("oow数量")
    @Schema(description = "oow数量")
    private Integer oowCount = 0;

    @ExcelProperty("ooc数量")
    @Schema(description = "ooc数量")
    private Integer oocCount = 0;

    @ExcelProperty("oos数量")
    @Schema(description = "oos数量")
    private Integer oosCount = 0;

    @ExcelProperty("oo3数量")
    @Schema(description = "oo3数量")
    private Integer oo3Count = 0;
}
