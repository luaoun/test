package com.px.ifp.spc.dto.manager.request;

import com.px.ifp.common.bean.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class AddSpcNormalNoteReqDTO extends BaseRequest {

    @Schema(description = "spcJobId")
    @NotBlank(message = "spcJobId不能为空")
    private String jobId;

    @Schema(description = "spcNoteId")
    private Long spcNoteId;

    @Schema(description = "要添加批注的点位值")
    @NotBlank(message = "要添加批注的点位值不能为空")
    private BigDecimal pointValue;

    @Schema(description = "pointValue对应的时间")
    @NotBlank(message = "要批注点位的值的时间不能为空")
    private Date ctime;

    @Schema(description = "批注内容")
    @NotBlank(message = "批注内容不能为空")
    private String content;

    @Schema(description = "是否锁定")
    private Boolean tag;

}
