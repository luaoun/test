package com.px.ifp.spc.bo;

import com.px.ifp.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-16
 */
@Data
public class SpcIndicatorExcelBO implements Serializable {
    @Excel(name = "指标名称")
    private String indicatorName;
    @Excel(name = "科室(气化科/水科/机械科/电科)")
    private String classCode;
    @Excel(name = "系统")
    private String systemCode;
    @Excel(name = "指标级别(Key-key/key/non-key)")
    private String indicatorLevel;
    @Excel(name = "绑定点位")
    private String point;
    @Excel(name = "点位单位")
    private String pointUnit;
    @Excel(name = "Y轴起始值")
    private BigDecimal startValue;
    @Excel(name = "Y轴最大值")
    private BigDecimal endValue;
    @Excel(name = "Y轴步长")
    private BigDecimal step;
    @Excel(name = "T")
    private BigDecimal targetValue;
    @Excel(name = "UWL")
    private BigDecimal uwlValue;
    @Excel(name = "LWL")
    private BigDecimal lwlValue;
    @Excel(name = "UCL")
    private BigDecimal uclValue;
    @Excel(name = "LCL")
    private BigDecimal lclValue;
    @Excel(name = "USL")
    private BigDecimal uslValue;
    @Excel(name = "LSL")
    private BigDecimal lslValue;
}
