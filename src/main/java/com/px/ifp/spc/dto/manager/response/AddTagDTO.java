package com.px.ifp.spc.dto.manager.response;

import com.px.ifp.common.bean.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AddTagDTO extends BaseRequest {
    @Schema(description = "businessKey")
    @NotNull(message = "报警ID不能为空")
    private  Long id;

    @Schema(description = "是否锁定批注")
    @NotNull(message = "是否锁定批注不能为空")
    private Boolean tag;

}
