package com.px.ifp.spc.dto.manager.request;

import com.px.ifp.common.bean.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-07-10
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AddSpcNoteReqDTO extends BaseRequest {
    @Schema(description = "业务key")
    @NotBlank(message = "业务key不能为空")
    private String businessKey;

    @Schema(description = "批注内容")
    @NotBlank(message = "批注内容不能为空")
    private String content;

    @Schema(description = "是否锁定")
    private Boolean tag;
}
