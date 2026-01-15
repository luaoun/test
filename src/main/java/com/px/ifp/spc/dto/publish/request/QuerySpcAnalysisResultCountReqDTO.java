package com.px.ifp.spc.dto.publish.request;

import com.px.ifp.common.bean.common.BaseRequest;
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
public class QuerySpcAnalysisResultCountReqDTO extends BaseRequest {
    @Schema(description = "作业id")
    private List<String> jobIdList;

    @Schema(description = "点位列表")
    private List<String> pointList;

    @Schema(description = "所属课室(WATER-水课,ELECTRICTITY-电课,GAS-气课,MACHINE-机械课)")
    private String classCode;

    @Schema(description = "指标级别")
    private String indicatorLevel;
    
    @Schema(description = "开始日期")
    @NotNull(message = "开始日期不能为空")
    private Date startTime;

    @Schema(description = "结束日期")
    @NotNull(message = "结束日期不能为空")
    private Date endTime;

    @Schema(description = "忽略厂区（查询全厂范围数据）")
    private Boolean ignoreFacCode = false;
}
