package com.px.ifp.spc.dto.publish.request;

import com.px.ifp.common.bean.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class QuerySpcIndicatorTreeDTO extends BaseRequest {
    @Schema(description = "指标名称(支持模糊搜索)")
    private String indicatorName;
    @Schema(description = "所属课室")
    private String classCode;
    @Schema(description = "所属系统")
    private List<String> systemCode;
    @Schema(description = "指标级别")
    private String indicatorLevel;
    @Schema(description = "配置类型(USER/SYSTEM)，不传查询全部")
    private String configType;
}
