package com.px.ifp.spc.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 报警段模型 - 用于状态机处理和Redis缓存
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmSegment {
    
    /**
     * 事件段ID
     */
    private String segmentId;
    
    /**
     * 开始时间
     */
    private Date startTime;
    
    /**
     * 结束时间
     */
    private Date endTime;
    
    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;
    
    /**
     * 报警类型（OOC、OOS、OOW、OO3、NORMAL）
     */
    private String alarmType;
    
    /**
     * 异常点数量
     */
    private Integer pointCount;
    
    /**
     * 最大异常值
     */
    private BigDecimal maxValue;

    /**
     * 最大异常值发生时间
     */
    private Date maxValueTime;
    
    /**
     * 最小异常值
     */
    private BigDecimal minValue;

    /**
     * 最小异常值发生时间
     */
    private Date minValueTime;

    /**
     * 峰值（根据报警方向智能选择最大值或最小值）
     */
    private BigDecimal peekValue;

    /**
     * 峰值发生时间
     */
    private Date peekTime;

    /**
     * 峰值方向类型（用于确定峰值选择策略）
     */
    private String peekDirectionType;

    /**
     * 当前值
     */
    private BigDecimal currentValue;


    /**
     * 结束值（触发报警事件结束的正常值）
     */
    private BigDecimal endValue;

    /**
     * 结束值时间（触发报警事件结束值对应的时间）
     */
    private Date endValueTime;
    
    /**
     * 指标ID
     */
    private Long indicatorId;
    
    /**
     * 指标名称
     */
    private String indicatorName;
    
    /**
     * 点位
     */
    private String point;

    /**
     * 严重程度级别（1-4，数值越大越严重）
     */
    private Integer severityLevel;

    /**
     * 最高严重程度（记录过程中出现的最高严重程度）
     */
    private Integer maxSeverityLevel;


    /**
     * 报警方向（UPPER-上限报警，LOWER-下限报警）
     */
    private String alarmDirection;

    /**
     * OOC标记（Out of Control）- 0或1
     */
    private Integer ooc;

    /**
     * OOS标记（Out of Specification）- 0或1
     */
    private Integer oos;

    /**
     * OOW标记（Out of Warning）- 0或1
     */
    private Integer oow;

    /**
     * OO3标记（Out of 3-sigma）- 0或1
     */
    private Integer oo3;

    /**
     * 获取持续时长（秒）
     */
    public long getDurationSeconds() {
        if (startTime == null) return 0;
        Date endOrCurrent = endTime != null ? endTime : new Date();
        long duration = (endOrCurrent.getTime() - startTime.getTime()) / 1000;
        // 防止负数：如果计算结果为负数，返回0（可能是时间乱序或系统时间回退）
        return Math.max(0, duration);
    }
    
    /**
     * 判断是否为活跃状态
     */
    public boolean isActive() {
        return endTime == null;
    }

    private String extractBaseAlarmType(String directionalType) {
        if (directionalType == null) return null;

        if (directionalType.contains("SL_")) return "OOC";  // Spec Limit
        if (directionalType.contains("CL_")) return "OOS";  // Control Limit
        if (directionalType.contains("WL_")) return "OOW";  // Warning Limit
        if (directionalType.contains("O3_")) return "OO3";  // 3-sigma

        return "UNKNOWN";
    }

    private String extractDirection(String directionalType) {
        if (directionalType == null) return "NORMAL";

        if (directionalType.endsWith("_UPPER")) return "UPPER";
        if (directionalType.endsWith("_LOWER")) return "LOWER";

        return "NORMAL";
    }

    // ==================== 峰值相关辅助方法 ====================

    /**
     * 智能更新峰值：根据方向类型自动选择最大值或最小值
     * @param newValue 新的数据值
     * @param newTime 新的数据时间
     * @param directionalType 方向性报警类型（如 "UWL_UPPER", "LWL_LOWER" 等）
     */
    public void updatePeekValue(BigDecimal newValue, Date newTime, String directionalType) {
        if (newValue == null || directionalType == null) {
            return;
        }

        // 记录方向类型
        this.peekDirectionType = directionalType;

        boolean isUpperLimit = directionalType.endsWith("_UPPER");
        boolean shouldUpdate = false;

        if (this.peekValue == null) {
            // 如果还没有峰值，直接设置
            shouldUpdate = true;
        } else if (isUpperLimit) {
            // 上限报警：选择最大值
            shouldUpdate = newValue.compareTo(this.peekValue) > 0;
        } else {
            // 下限报警：选择最小值
            shouldUpdate = newValue.compareTo(this.peekValue) < 0;
        }

        if (shouldUpdate) {
            this.peekValue = newValue;
            this.peekTime = newTime;
        }
    }

    /**
     * 从当前的max/min值中计算并设置峰值
     */
    public void calculatePeekFromMaxMin() {
        if (peekDirectionType == null) {
            return;
        }

        boolean isUpperLimit = peekDirectionType.endsWith("_UPPER");

        if (isUpperLimit && maxValue != null) {
            // 上限报警：使用最大值作为峰值
            this.peekValue = this.maxValue;
            this.peekTime = this.maxValueTime;
        } else if (!isUpperLimit && minValue != null) {
            // 下限报警：使用最小值作为峰值
            this.peekValue = this.minValue;
            this.peekTime = this.minValueTime;
        }
    }

    /**
     * 更新最大值，同时检查是否需要更新峰值
     */
    public void updateMaxValue(BigDecimal newValue, Date newTime) {
        if (newValue == null) {
            return;
        }

        // 更新最大值
        if (this.maxValue == null || newValue.compareTo(this.maxValue) > 0) {
            this.maxValue = newValue;
            this.maxValueTime = newTime;

            // 如果是上限报警，同时更新峰值
            if (peekDirectionType != null && peekDirectionType.endsWith("_UPPER")) {
                this.peekValue = this.maxValue;
                this.peekTime = this.maxValueTime;
            }
        }
    }

    /**
     * 更新最小值，同时检查是否需要更新峰值
     */
    public void updateMinValue(BigDecimal newValue, Date newTime) {
        if (newValue == null) {
            return;
        }

        // 更新最小值
        if (this.minValue == null || newValue.compareTo(this.minValue) < 0) {
            this.minValue = newValue;
            this.minValueTime = newTime;

            // 如果是下限报警，同时更新峰值
            if (peekDirectionType != null && peekDirectionType.endsWith("_LOWER")) {
                this.peekValue = this.minValue;
                this.peekTime = this.minValueTime;
            }
        }
    }

    /**
     * 获取峰值描述信息
     */
    public String getPeekValueDescription() {
        if (peekValue == null) {
            return "无峰值";
        }

        String direction = "";
        if (peekDirectionType != null) {
            direction = peekDirectionType.endsWith("_UPPER") ? "(上限峰值)" : "(下限峰值)";
        }

        return String.format("峰值: %s %s (时间: %s)", peekValue, direction, peekTime);
    }

    /**
     * 判断是否为上限报警
     */
    public boolean isUpperLimitAlarm() {
        return peekDirectionType != null && peekDirectionType.endsWith("_UPPER");
    }

    /**
     * 判断是否为下限报警
     */
    public boolean isLowerLimitAlarm() {
        return peekDirectionType != null && peekDirectionType.endsWith("_LOWER");
    }
}
