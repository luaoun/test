package com.px.ifp.spc.dto.publish.request;

import com.px.ifp.common.bean.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuerySpcIndicatorListReqDTO extends PageRequest {
    @Schema(description = "指标名称(支持模糊搜索)")
    private String indicatorName;
    @Schema(description = "所属课室")
    @NotNull(message = "请填写所属科室编码")
    private String classCode;
    @Schema(description = "所属系统")
    private List<String> systemCode;
    @Schema(description = "指标级别")
    private String indicatorLevel;
    @Schema(description = "点位")
    private String point;
    @Schema(description = "指标jobId")
    private List<String> jobId;
    @Schema(description = "开始时间")
    private Date startTime;
    @Schema(description = "结束时间")
    private Date endTime;
    @Schema(description = "是否查询SPC跳点数量")
    private Boolean querySpcCount;
    @Schema(description = "忽略厂区（查询全厂范围数据）")
    private Boolean ignoreFacCode = false;
    @Schema(description = "配置类型(USER/SYSTEM)，不传查询全部")
    private String configType;

}
