package com.px.ifp.spc.dto.manager.request;

import com.px.ifp.common.bean.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-07-10
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QuerySpcAnalysisResultListReqDTO extends PageRequest {
    @Schema(description = "作业id")
    private List<String> jobIdList;

    @Schema(description = "指标编码列表（通过指标编码反查JobId)")
    private List<String> pointList;

    @Schema(description = "告警类型(ooc/oow/oos/oo3/normal)")
    private String alarmType;

    @Schema(description = "开始日期")
    @NotNull(message = "开始日期不能为空")
    private Date startTime;

    @Schema(description = "结束日期")
    @NotNull(message = "结束日期不能为空")
    private Date endTime;

    @Schema(description = "忽略厂区（查询全厂范围数据）")
    private Boolean ignoreFacCode = false;

    @Schema(description = "配置类型(USER/SYSTEM)，不传查询全部")
    private String configType;
}
