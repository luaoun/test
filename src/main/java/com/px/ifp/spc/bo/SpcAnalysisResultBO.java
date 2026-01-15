package com.px.ifp.spc.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-07-10
 */
@Data
public class SpcAnalysisResultBO implements Serializable {
    @Schema(description = "作业id")
    private String jobId;

    @Schema(description = "oos数量")
    private Integer oosCount;

    @Schema(description = "oow数量")
    private Integer oowCount;

    @Schema(description = "ooc数量")
    private Integer oocCount;

    @Schema(description = "oo3数量")
    private Integer oo3Count;

    @Schema(description = "normal数量")
    private Integer normalCount;
}
