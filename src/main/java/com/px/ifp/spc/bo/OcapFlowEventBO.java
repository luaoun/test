package com.px.ifp.spc.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OcapFlowEventBO {

    @Schema(description = "FFP流程定义名称")
    private String flowName;

    @Schema(description = "spc报警信息描述")
    private String spcDesc;

    @Schema(description = "厂区编码")
    private String facCode;
}
