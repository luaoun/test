package com.px.ifp.spc.dto;

import com.px.ifp.common.bean.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdReqDTO extends BaseRequest {

    @Schema(description = "id")
    @NotNull(message = "id不能为空")
    private Long id;

    @Schema(description = "额外的参数。例如检测类别")
    private String extra;

    @Schema(description = "提醒内容")
    private String reminderContent;

    @Schema(description = "记日志")
    private String labelName;
}
