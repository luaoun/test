package com.px.ifp.spc.dto.manager.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
public class QuerySpcIndicatorDTO {
    @Schema(description = "指标id")
    private Long id;

    @Schema(description = "作业id")
    private String jobId;

    @Schema(description = "指标名称")
    private String indicatorName;

    @Schema(description = "所属课室")
    private String classCode;

    @Schema(description = "所属系统")
    private String systemCode;

    @Schema(description = "指标级别")
    private String indicatorLevel;

    @Schema(description = "点位")
    private String point;
}
