package com.px.ifp.spc.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 报警统计信息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmStatistics {
    
    /**
     * 最大值
     */
    private BigDecimal maxValue;

    /**
     * 最大值发生时间
     */
    private Date maxValueTime;
    
    /**
     * 最小值
     */
    private BigDecimal minValue;

    /**
     * 最小值发生时间
     */
    private Date minValueTime;
    
    /**
     * 平均值
     */
    private BigDecimal avgValue;

    /**
     * 首值（引发SPC报警事件的第一个值）
     */
    private BigDecimal firstValue;

    /**
     * 首值时间
     */
    private Date firstValueTime;

    /**
     * 最新值
     */
    private BigDecimal lastValue;

    /**
     * 最新值时间
     */
    private Date lastValueTime;
    
    /**
     * 异常点数量
     */
    private Integer abnormalPointCount;
    
    /**
     * 总点数量
     */
    private Integer totalPointCount;
    
    /**
     * 异常率（百分比）
     */
    public Double getAbnormalRate() {
        if (totalPointCount == null || totalPointCount == 0) {
            return 0.0;
        }
        if (abnormalPointCount == null) {
            return 0.0;
        }
        return (abnormalPointCount.doubleValue() / totalPointCount.doubleValue()) * 100.0;
    }

    /**
     * 计算值的变化量（最新值 - 首值）
     */
    public BigDecimal getValueChange() {
        if (firstValue == null || lastValue == null) {
            return null;
        }
        return lastValue.subtract(firstValue);
    }

    /**
     * 计算值的变化率（百分比）
     */
    public BigDecimal getValueChangeRate() {
        if (firstValue == null || lastValue == null || firstValue.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal change = getValueChange();
        if (change == null) {
            return null;
        }
        return change.divide(firstValue, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal(100));
    }

    /**
     * 判断是否有值变化
     */
    public boolean hasValueChanged() {
        if (firstValue == null || lastValue == null) {
            return false;
        }
        return firstValue.compareTo(lastValue) != 0;
    }

    /**
     * 获取值变化趋势描述
     */
    public String getValueTrendDescription() {
        BigDecimal change = getValueChange();
        if (change == null) {
            return "无变化数据";
        }

        if (change.compareTo(BigDecimal.ZERO) > 0) {
            return String.format("上升 %.3f", change);
        } else if (change.compareTo(BigDecimal.ZERO) < 0) {
            return String.format("下降 %.3f", change.abs());
        } else {
            return "无变化";
        }
    }
}
