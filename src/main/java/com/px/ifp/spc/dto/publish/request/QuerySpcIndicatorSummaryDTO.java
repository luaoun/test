package com.px.ifp.spc.dto.publish.request;

import com.px.ifp.common.bean.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Data
public class QuerySpcIndicatorSummaryDTO extends BaseRequest {

    @Schema(description = "所属系统")
    @NotNull(message = "系统code不能为空")
    private String systemCode;

    @Schema(description = "点位")
    @NotNull(message = "点位不能为空")
    private List<String> points;

    @Schema(description = "开始时间")
    @NotNull(message = "开始时间不能为空")
    private Date startTime;

    @Schema(description = "结束时间")
    @NotNull(message = "结束时间不能为空")
    private Date endTime;
}
