package com.px.ifp.spc.dto.manager.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-07-10
 */
@Data
public class SpcAnalysisResultDTO implements Serializable {
    @Schema(description = "报警事件表的ID")
    private Long id;

    @Schema(description = "jobId")
    private String jobId;

    @Schema(description = "SPC指标表ID")
    private Long indicatorId;

    @Schema(description = "指标名称")
    private String indicatorName;

    @Schema(description = "点位")
    private String point;

    @Schema(description = "子系统编码")
    private String systemCode;

    @Schema(description = "子系统名称")
    private String systemName;

    @Schema(description = "关联报警事件段ID")
    private String segmentId;

    @Schema(description = "持续时长(秒)")
    private Integer durationSeconds;

    @Schema(description = "持续时长")
    private String duration;

    @Schema(description = "异常点数量")
    private Integer pointCount;

    @Schema(description = "告警类型")
    private String alarmType;

    @Schema(description = "点位值")
    private BigDecimal pointValue;

    @Schema(description = "最大异常值")
    private BigDecimal maxValue;

    @Schema(description = "最大异常值发生时间")
    private Date maxValueTime;

    @Schema(description = "最小异常值")
    private BigDecimal minValue;

    @Schema(description = "最小异常值发生时间")
    private Date minValueTime;

    @Schema(description = "平均异常值")
    private BigDecimal avgValue;

    @Schema(description = "峰值")
    private BigDecimal peekValue;

    @Schema(description = "峰值时间")
    private Date peekValueTime;

    @Schema(description = "第一个异常值")
    private BigDecimal firstValue;

    @Schema(description = "第一个异常值时间")
    private Date firstValueTime;

    @Schema(description = "最后一个异常值")
    private BigDecimal lastValue;

    @Schema(description = "最后一个异常值时间")
    private Date lastValueTime;

    @Schema(description = "报警事件创建时间")
    private Date createTime;

    @Schema(description = "报警事件的事件点(用报警峰值的时间来标记，每个报警事件只有一个事件点)")
    private Date eventTime;

    @Schema(description = "报警事件结束时间")
    private Date eventEndTime;

    @Schema(description = "spcNote最新一条记录")
    private SpcNoteDTO lastSpcNote;
}
