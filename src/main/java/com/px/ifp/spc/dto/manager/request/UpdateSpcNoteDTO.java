package com.px.ifp.spc.dto.manager.request;

import com.px.ifp.common.bean.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateSpcNoteDTO extends BaseRequest {

    @Schema(description = "主键id")
    private Long id;

    @Schema(description = "内容")
    private String content;

}
