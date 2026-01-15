package com.px.ifp.spc.dto.manager.request;

import com.px.ifp.common.bean.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuerySpcAnalysisReqDTO extends BaseRequest {
    @Schema(description = "作业编号")
    private List<String> jobIdList;

    @Schema(description = "点位")
    private List<String> pointList;

    @Schema(description = "时间周期(minutes/hour/day/week/months)")
    @NotNull(message = "时间周期不能为空")
    private String timePeriod;

    @Schema(description = "查询时间的间隔")
    private Integer timeInterval = 1;

    @Schema(description = "统计函数：AVG/MAX/MIN/COUNT/FIRST/LAST/SUM/STDDEV_POP")
    private String statFunction = "LAST";

    @Schema(description = "开始日期")
    @NotNull(message = "开始日期不能为空")
    private Date startTime;

    @Schema(description = "结束日期")
    @NotNull(message = "结束日期不能为空")
    private Date endTime;

    @Schema(description = "是否统计曲线上的最新批注")
    private Boolean requireAlarmNote = false;

    @Schema(description = "需要标记spc报警ID（spcAnalysisResultId)")
    private Long spcAnalysisResultId;

    @Schema(description = "忽略查询时间范围限制")
    private Boolean ignoreTimeValidate = false;

    @Schema(description = "放大数据的倍率（例如：100 放大100倍，-100 缩小100倍）默认为0不处理倍率")
    private Integer radio=0;

    @Schema(description = "自定义数值精度（例如：0不处理精度，大于0 处理保留小数位）默认为0不处理精度")
    private Integer scale=0;

    @Schema(description = "忽略厂区（查询全厂范围数据）")
    private Boolean ignoreFacCode = false;

    @Schema(description = "配置类型(USER/SYSTEM)，为空则查询全部")
    private String configType;
}
