package com.px.ifp.spc.dto.publish.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SpcIndicatorDataSummaryDTO {

    @Schema(description = "作业id")
    private String jobId;

    @Schema(description = "点位")
    private String point;

    @Schema(description = "发生异常点位数据")
    private BigDecimal pointValue;

    @Schema(description = "oos报警")
    private Integer oos;

    @Schema(description = "oow报警")
    private Integer oow;

    @Schema(description = "ooc报警")
    private Integer ooc;

    @Schema(description = "oo3报警")
    private Integer oo3;

    @Schema(description = "异常报警类型(ooc/oos/oow/oo3)")
    private String alarmType;

    @Schema(description = "发生异常的时间")
    private Date eventTime;
}
