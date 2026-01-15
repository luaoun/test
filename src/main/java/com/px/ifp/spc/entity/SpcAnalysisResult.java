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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-09
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("spc_analysis_result")
@Schema(name = "SpcAnalysisResult", description = "SPC分析结果表")
public class SpcAnalysisResult implements Serializable {
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "SPC指标表ID")
    @TableField("indicator_id")
    private Long indicatorId;

    @Schema(description = "作业id")
    @TableField("job_id")
    private String jobId;

    @Schema(description = "指标名称")
    @TableField("indicator_name")
    private String indicatorName;

    @Schema(description = "所属科室")
    @TableField("class_code")
    private String classCode;

    @Schema(description = "指标级别")
    @TableField("indicator_level")
    private String indicatorLevel;

    @Schema(description = "绑定点位")
    @TableField("point")
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

    @Schema(description = "点位值")
    @TableField("point_value")
    private BigDecimal pointValue;

    // 新增字段：关联报警事件段ID
    @Schema(description = "关联报警事件段ID")
    @TableField("segment_id")
    private String segmentId;

    @Schema(description = "OOS")
    @TableField("oos")
    private Boolean oos;

    @Schema(description = "OOW")
    @TableField("oow")
    private Boolean oow;

    @Schema(description = "OOC")
    @TableField("ooc")
    private Boolean ooc;

    @Schema(description = "OO3")
    @TableField("oo3")
    private Boolean oo3;

    @Schema(description = "normal")
    @TableField("normal")
    private Boolean normal;

    @Schema(description = "事件发生时间")
    @TableField("event_time")
    private Date eventTime;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private Date createTime;

    @Schema(description = "厂区")
    @TableField("FAC_CODE")
    private String facCode;


    // 辅助方法：判断是否有任何异常（不包含OO3）
    public boolean isAnyAbnormal() {
        return Boolean.TRUE.equals(oos) || Boolean.TRUE.equals(oow) ||
                Boolean.TRUE.equals(ooc);
        // 注意：不再检查 Boolean.TRUE.equals(oo3)
    }

    // 辅助方法：获取异常状态字符串（不包含OO3）
    public String getAlarmStatus() {
        List<String> statuses = new ArrayList<>();
        if (Boolean.TRUE.equals(oos)) statuses.add("OOS");
        if (Boolean.TRUE.equals(oow)) statuses.add("OOW");
        if (Boolean.TRUE.equals(ooc)) statuses.add("OOC");
        // 注意：不再包含 OO3 状态
        return String.join(",", statuses);
    }


    // 辅助方法：确定报警类型（最严重的，不包含OO3）
    public String getDominantAlarmType() {
        if (Boolean.TRUE.equals(oos)) return "OOS";  // USL/LSL - 最严重
        if (Boolean.TRUE.equals(ooc)) return "OOC";  // UCL/LCL - 较严重
        if (Boolean.TRUE.equals(oow)) return "OOW";  // UWL/LWL - 中等
        // 注意：不再支持 U3L/L3L - OO3 轻微
        return "NORMAL";
    }

    // 判断报警方向
    public String getAlarmDirection() {
        if (isUpperLimitAlarm()) {
            return "UPPER";
        } else if (isLowerLimitAlarm()) {
            return "LOWER";
        }
        return "NORMAL";
    }

    // 判断是否为上限报警（不包含U3L）
    public boolean isUpperLimitAlarm() {
        // 检查是否超过任一上限（不包含U3L）
        if (pointValue == null) return false;

        boolean exceedsUSL = uslValue != null && pointValue.compareTo(uslValue) > 0;
        boolean exceedsUCL = uclValue != null && pointValue.compareTo(uclValue) > 0;
        boolean exceedsUWL = uwlValue != null && pointValue.compareTo(uwlValue) > 0;
        // 注意：不再检查 U3L 上限

        return exceedsUSL || exceedsUCL || exceedsUWL;
    }

    // 判断是否为下限报警（不包含L3L）
    public boolean isLowerLimitAlarm() {
        // 检查是否低于任一下限（不包含L3L）
        if (pointValue == null) return false;

        boolean belowLSL = lslValue != null && pointValue.compareTo(lslValue) < 0;
        boolean belowLCL = lclValue != null && pointValue.compareTo(lclValue) < 0;
        boolean belowLWL = lwlValue != null && pointValue.compareTo(lwlValue) < 0;
        // 注意：不再检查 L3L 下限

        return belowLSL || belowLCL || belowLWL;
    }

    // 获取上限报警类型（USL > UCL > UWL - 按严重程度排序，移除U3L）
    public String getUpperLimitAlarmType() {
        if (pointValue == null) return null;

        if (uslValue != null && pointValue.compareTo(uslValue) > 0) {
            return "USL_UPPER";  // 对应 OOS
        }
        if (uclValue != null && pointValue.compareTo(uclValue) > 0) {
            return "UCL_UPPER";  // 对应 OOC
        }
        if (uwlValue != null && pointValue.compareTo(uwlValue) > 0) {
            return "UWL_UPPER";  // 对应 OOW
        }
        // 注意：不再支持 U3L_UPPER (对应 OO3)
        return null;
    }

    // 获取下限报警类型（LSL > LCL > LWL - 按严重程度排序，移除L3L）
    public String getLowerLimitAlarmType() {
        if (pointValue == null) return null;

        if (lslValue != null && pointValue.compareTo(lslValue) < 0) {
            return "LSL_LOWER";  // 对应 OOS
        }
        if (lclValue != null && pointValue.compareTo(lclValue) < 0) {
            return "LCL_LOWER";  // 对应 OOC
        }
        if (lwlValue != null && pointValue.compareTo(lwlValue) < 0) {
            return "LWL_LOWER";  // 对应 OOW
        }
        // 注意：不再支持 L3L_LOWER (对应 OO3)
        return null;
    }

    // 获取带方向的报警类型
    public String getDirectionalAlarmType() {
        String upperType = getUpperLimitAlarmType();
        if (upperType != null) {
            return upperType;
        }

        String lowerType = getLowerLimitAlarmType();
        if (lowerType != null) {
            return lowerType;
        }

        return "NORMAL";
    }
}
