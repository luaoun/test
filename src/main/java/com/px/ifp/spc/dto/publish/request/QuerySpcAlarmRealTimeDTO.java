package com.px.ifp.spc.dto.publish.request;

import com.px.ifp.common.bean.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class QuerySpcAlarmRealTimeDTO extends BaseRequest {
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
    private List<String> point;
    @Schema(description = "指标jobId")
    private List<String> jobId;
    @Schema(description = "忽略厂区（查询全厂范围数据）")
    private Boolean ignoreFacCode = false;
    @Schema(description = "配置类型(USER/SYSTEM)，不传查询全部")
    private String configType;
}
