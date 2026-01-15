package com.px.ifp.spc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * SPC报警事件表
 * </p>
 *
 * @author liuxj
 * @since 2025-08-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("spc_alarm_event")
@Schema(name = "SpcAlarmEvent", description = "SPC报警事件表")
public class SpcAlarmEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "事件段ID")
    @TableField("SEGMENT_ID")
    private String segmentId;

    @Schema(description = "SPC指标表ID")
    @TableField("INDICATOR_ID")
    private Long indicatorId;

    @Schema(description = "所属科室")
    @TableField("CLASS_CODE")
    private String classCode;

    @Schema(description = "作业ID")
    @TableField("JOB_ID")
    private String jobId;

    @Schema(description = "指标名称")
    @TableField("INDICATOR_NAME")
    private String indicatorName;

    @Schema(description = "点位")
    @TableField("POINT")
    private String point;

    @Schema(description = "目标值")
    @TableField("target_value")
    private BigDecimal targetValue;

    @Schema(description = "控制线上限值")
    @TableField("ucl_value")
    private BigDecimal uclValue;

    @Schema(description = "控制线下限值")
    @TableField("lcl_value")
    private BigDecimal lclValue;

    @Schema(description = "警告线上限值")
    @TableField("uwl_value")
    private BigDecimal uwlValue;

    @Schema(description = "警告线下限值")
    @TableField("lwl_value")
    private BigDecimal lwlValue;

    @Schema(description = "规格线上限值")
    @TableField("usl_value")
    private BigDecimal uslValue;

    @Schema(description = "规格线下限值")
    @TableField("lsl_value")
    private BigDecimal lslValue;

    @Schema(description = "3σ上限值")
    @TableField("u3l_value")
    private BigDecimal u3lValue;

    @Schema(description = "3σ下限值")
    @TableField("l3l_value")
    private BigDecimal l3lValue;

    @Schema(description = "自控线上限")
    @TableField("uacl_value")
    private BigDecimal uaclValue;

    @Schema(description = "自控线下限值")
    @TableField("lacl_value")
    private BigDecimal laclValue;

    // 优化：将 alarm_type 拆分为 4个 bit(1) 字段
    @TableField("OOS")
    private Boolean oos;

    @TableField("OOW")
    private Boolean oow;

    @TableField("OOC")
    private Boolean ooc;

    @TableField("OO3")
    private Boolean oo3;

    @TableField("NORMAL")
    private Boolean normal;

    @Schema(description = "指标级别")
    @TableField("INDICATOR_LEVEL")
    private String indicatorLevel;

    @Schema(description = "报警开始时间")
    @TableField("START_TIME")
    private Date startTime;

    @Schema(description = "报警结束时间")
    @TableField("END_TIME")
    private Date endTime;

    @Schema(description = "持续时长(秒)")
    @TableField("DURATION_SECONDS")
    private Integer durationSeconds;

    @Schema(description = "异常点数量")
    @TableField("POINT_COUNT")
    private Integer pointCount;

    @Schema(description = "最大异常值")
    @TableField("MAX_VALUE")
    private BigDecimal maxValue;

    @Schema(description = "最大异常值发生时间")
    @TableField("max_value_time")
    private Date maxValueTime;

    @Schema(description = "最小异常值")
    @TableField("MIN_VALUE")
    private BigDecimal minValue;

    @Schema(description = "最小异常值发生时间")
    @TableField("min_value_time")
    private Date minValueTime;

    @Schema(description = "平均异常值")
    @TableField("AVG_VALUE")
    private BigDecimal avgValue;

    @Schema(description = "峰值")
    @TableField("peek_value")
    private BigDecimal peekValue;

    @Schema(description = "峰值时间")
    @TableField("peek_value_time")
    private Date peekValueTime;

    @Schema(description = "第一个异常值")
    @TableField("first_value")
    private BigDecimal firstValue;

    @Schema(description = "第一个异常值时间")
    @TableField("first_value_time")
    private Date firstValueTime;

    @Schema(description = "最后一个异常值")
    @TableField("last_value")
    private BigDecimal lastValue;

    @Schema(description = "最后一个异常值时间")
    @TableField("last_value_time")
    private Date lastValueTime;

    @TableField("end_value")
    private BigDecimal endValue;

    @TableField("end_value_time")
    private Date endValueTime;

    @Schema(description = "严重级别")
    @TableField("SEVERITY_LEVEL")
    private String severityLevel;

    @Schema(description = "状态(ACTIVE/RESOLVED/IGNORED/TIMEOUT)")
    @TableField("STATUS")
    private String status;

    @Schema(description = "是否已发送通知")
    @TableField("NOTIFICATION_SENT")
    private Boolean notificationSent;

    @Schema(description = "处理人")
    @TableField("HANDLED_BY")
    private String handledBy;

    @Schema(description = "处理时间")
    @TableField("HANDLE_TIME")
    private Date handleTime;

    @Schema(description = "处理备注")
    @TableField("HANDLE_NOTE")
    private String handleNote;

    @Schema(description = "创建时间")
    @TableField("CREATE_TIME")
    private Date createTime;

    @Schema(description = "更新时间")
    @TableField("UPDATE_TIME")
    private Date updateTime;

    @Schema(description = "厂区")
    @TableField("FAC_CODE")
    private String facCode;

    /**
     * 判断事件是否活跃
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /**
     * 判断事件是否已解决
     */
    public boolean isResolved() {
        return "RESOLVED".equals(status);
    }

    /**
     * 判断是否高优先级报警
     */
    public boolean isHighPriority() {
        return "HIGH".equals(severityLevel);
    }

    // ==================== 新增：报警类型相关辅助方法 ====================

    /**
     * 获取报警类型字符串（向后兼容）
     */
    public String getAlarmType() {
        if (Boolean.TRUE.equals(oos)) return "OOS";
        if (Boolean.TRUE.equals(oow)) return "OOW";
        if (Boolean.TRUE.equals(ooc)) return "OOC";
        if (Boolean.TRUE.equals(oo3)) return "OO3";
        if (Boolean.TRUE.equals(normal)) return "NORMAL";
        return null;
    }

    /**
     * 设置报警类型（向后兼容）
     */
    public void setAlarmType(String alarmType) {
        // 先重置所有标志
        this.oos = false;
        this.oow = false;
        this.ooc = false;
        this.oo3 = false;
        this.normal = false;

        // 根据类型设置对应标志
        if ("OOS".equals(alarmType)) {
            this.oos = true;
        } else if ("OOW".equalsIgnoreCase(alarmType)) {
            this.oow = true;
        } else if ("OOC".equalsIgnoreCase(alarmType)) {
            this.ooc = true;
        } else if ("OO3".equalsIgnoreCase(alarmType)) {
            this.oo3 = true;
        } else if("NORMAL".equalsIgnoreCase(alarmType)){
            this.normal = true;
        }
    }

    /**
     * 创建指定类型的报警事件
     */
    public static SpcAlarmEvent createWithAlarmType(String alarmType) {
        SpcAlarmEvent event = new SpcAlarmEvent();
        event.setAlarmType(alarmType);
        return event;
    }

    /**
     * 判断是否有任何报警类型被设置
     */
    public boolean hasAnyAlarmType() {
        return Boolean.TRUE.equals(oos) || Boolean.TRUE.equals(oow) ||
                Boolean.TRUE.equals(ooc) || Boolean.TRUE.equals(oo3) ||
                Boolean.TRUE.equals(normal);
    }

    /**
     * 判断是否为高危报警类型（OOS/OOC）
     */
    public boolean isHighRiskAlarmType() {
        return Boolean.TRUE.equals(oos) || Boolean.TRUE.equals(ooc);
    }

    /**
     * 判断是否为中危报警类型（OOW）
     */
    public boolean isMediumRiskAlarmType() {
        return Boolean.TRUE.equals(oow);
    }

    /**
     * 判断是否为低危报警类型（OO3）
     */
    public boolean isLowRiskAlarmType() {
        return Boolean.TRUE.equals(oo3);
    }

    /**
     * 判断是否为自定义标记（normal)
     * @return
     */
    public boolean isNormalAlarmType(){ return Boolean.TRUE.equals(normal);}

    // ==================== 新增：峰值相关辅助方法 ====================

    /**
     * 判断是否为上限报警类型（应该取最大值作为峰值）
     */
    public boolean isUpperLimitAlarm() {
        // TODO: 需要结合方向信息，这里先判断报警类型
        // 注意：这个方法需要结合具体的报警方向来判断
        // 暂时返回true，需要在具体使用时结合方向信息
        return true;
    }

    /**
     * 判断是否为下限报警类型（应该取最小值作为峰值）
     */
    public boolean isLowerLimitAlarm() {
        // TODO: 需要结合方向信息来判断
        return false;
    }

    /**
     * 智能更新峰值：根据报警类型和新值自动选择最大值或最小值
     * @param newValue 新的数据值
     * @param newTime 新的数据时间
     * @param isUpperLimit 是否为上限报警（true=上限取最大值，false=下限取最小值）
     */
    public void updatePeekValue(BigDecimal newValue, Date newTime, boolean isUpperLimit) {
        if (newValue == null) {
            return;
        }

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
            this.peekValueTime = newTime;
        }
    }

    /**
     * 根据当前的max/min值智能设置峰值
     * @param directionalAlarmType 方向性报警类型（如 "UWL_UPPER", "LWL_LOWER" 等）
     */
    public void calculatePeekValueFromMaxMin(String directionalAlarmType) {
        if (directionalAlarmType == null) {
            return;
        }

        boolean isUpperLimit = directionalAlarmType.endsWith("_UPPER");

        if (isUpperLimit) {
            // 上限报警：使用最大值作为峰值
            this.peekValue = this.maxValue;
            this.peekValueTime = this.maxValueTime;
        } else if (directionalAlarmType.endsWith("_LOWER")) {
            // 下限报警：使用最小值作为峰值
            this.peekValue = this.minValue;
            this.peekValueTime = this.minValueTime;
        }
    }

    /**
     * 获取峰值描述信息
     */
    public String getPeekValueDescription() {
        if (peekValue == null) {
            return "无峰值";
        }
        return String.format("峰值: %s (时间: %s)", peekValue, peekValueTime);
    }

    // ==================== 新增：首值和最新值相关辅助方法 ====================

    /**
     * 设置首值信息（引发SPC报警事件的第一个值）
     * @param value 首值
     * @param time 首值时间
     */
    public void setFirstValueInfo(BigDecimal value, Date time) {
        this.firstValue = value;
        this.firstValueTime = time;
    }

    /**
     * 更新最新值信息
     * @param value 最新值
     * @param time 最新值时间
     */
    public void updateLastValue(BigDecimal value, Date time) {
        this.lastValue = value;
        this.lastValueTime = time;
    }

    /**
     * 获取首值描述信息
     */
    public String getFirstValueDescription() {
        if (firstValue == null) {
            return "无首值";
        }
        return String.format("首值: %s (时间: %s)", firstValue, firstValueTime);
    }

    /**
     * 获取最新值描述信息
     */
    public String getLastValueDescription() {
        if (lastValue == null) {
            return "无最新值";
        }
        return String.format("最新值: %s (时间: %s)", lastValue, lastValueTime);
    }

    /**
     * 判断是否有值变化（首值和最新值不同）
     */
    public boolean hasValueChanged() {
        if (firstValue == null || lastValue == null) {
            return false;
        }
        return firstValue.compareTo(lastValue) != 0;
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
        return change.divide(firstValue, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100));
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

    // ==================== 新增：结束值相关辅助方法 ====================

    /**
     * 设置结束值信息（触发报警事件结束的正常值）
     * @param value 结束值
     * @param time 结束值时间
     */
    public void setEndValueInfo(BigDecimal value, Date time) {
        this.endValue = value;
        this.endValueTime = time;
    }

    /**
     * 获取结束值描述信息
     */
    public String getEndValueDescription() {
        if (endValue == null) {
            return "报警事件尚未结束";
        }
        return String.format("结束值: %s (时间: %s)", endValue, endValueTime);
    }

    /**
     * 判断是否有结束值（即事件是否已经结束）
     */
    public boolean hasEndValue() {
        return endValue != null && endValueTime != null;
    }

    /**
     * 获取从首值到结束值的变化量
     */
    public BigDecimal getFirstToEndChange() {
        if (firstValue == null || endValue == null) {
            return null;
        }
        return endValue.subtract(firstValue);
    }

    /**
     * 获取从峰值到结束值的恢复量（显示恢复程度）
     */
    public BigDecimal getPeekToEndRecovery() {
        if (peekValue == null || endValue == null) {
            return null;
        }
        return endValue.subtract(peekValue);
    }

    /**
     * 计算恢复率（从峰值到结束值的恢复程度百分比）
     */
    public BigDecimal getRecoveryRate() {
        BigDecimal recovery = getPeekToEndRecovery();
        if (recovery == null || firstValue == null || peekValue == null) {
            return null;
        }

        BigDecimal totalDeviation = peekValue.subtract(firstValue);
        if (totalDeviation.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal(100); // 如果没有偏离，则认为完全恢复
        }

        return recovery.divide(totalDeviation, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100));
    }

    /**
     * 获取事件恢复描述
     */
    public String getRecoveryDescription() {
        if (!hasEndValue()) {
            return "事件尚未结束";
        }

        BigDecimal recoveryRate = getRecoveryRate();
        if (recoveryRate == null) {
            return "恢复数据不完整";
        }

        return String.format("恢复率: %.2f%%, 结束值: %s", recoveryRate, endValue);
    }
}
