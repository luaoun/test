package com.px.ifp.spc.dto;

import com.px.ifp.common.bean.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class IdsReqDTO extends BaseRequest {

    @Schema(description = "id")
    @NotEmpty(message = "id不能为空")
    private List<Long> ids;

    @Schema(description = "额外增补字段")
    @NotEmpty(message = "id不能为空")
    private String extra;
}
