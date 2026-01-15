package com.px.ifp.spc.dto.manager.request;

import com.px.ifp.spc.dto.IdReqDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SpcStatusChangeDTO extends IdReqDTO {
    @Schema(description = "是否禁用")
    private boolean status;
}
