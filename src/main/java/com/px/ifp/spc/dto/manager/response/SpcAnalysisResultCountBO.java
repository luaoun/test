package com.px.ifp.spc.dto.manager.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-07-10
 */
@Data
public class SpcAnalysisResultCountBO implements Serializable {
    @Schema(description = "作业id")
    private String jobId;

    @Schema(description = "SPC指标表ID")
    private Long indicatorId;

    @Schema(description = "oow数量")
    private Integer oowCount = 0;

    @Schema(description = "ooc数量")
    private Integer oocCount = 0;

    @Schema(description = "oos数量")
    private Integer oosCount = 0;

    @Schema(description = "oo3数量")
    private Integer oo3Count = 0;

    @Schema(description = "normal数量")
    private Integer normalCount = 0;
}
