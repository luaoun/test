package com.px.ifp.spc.dto.manager.request;

import com.px.ifp.common.bean.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Data
public class QuerySpcAnalysisResultNotesDTO extends BaseRequest {
    @Schema(description = "作业id")
    private List<String> jobIdList;

    @Schema(description = "点位")
    private List<String> pointList;

    @Schema(description = "所属科室(WATER-水课,ELECTRICTITY-电课,GAS-气课,MACHINE-机械课)")
    @NotNull(message = "科室不能为空")
    private String classCode;

    @Schema(description = "子系统编码")
    private List<String> systemCode;

    @Schema(description = "开始日期")
    @NotNull(message = "开始日期不能为空")
    private Date startTime;

    @Schema(description = "结束日期")
    @NotNull(message = "结束日期不能为空")
    private Date endTime;

    @Schema(description = "限制显示最大异常结果条数(默认15条)")
    private Integer limit = 15;

    @Schema(description = "忽略厂区（查询全厂范围数据）")
    private Boolean ignoreFacCode = false;

    @Schema(description = "配置类型(USER/SYSTEM)，不传查询全部")
    private String configType;
}
