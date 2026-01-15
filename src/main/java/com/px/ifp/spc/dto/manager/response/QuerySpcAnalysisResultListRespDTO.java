package com.px.ifp.spc.dto.manager.response;

import com.px.ifp.common.bean.common.BaseBean;
import com.px.ifp.common.bean.common.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-07-11
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QuerySpcAnalysisResultListRespDTO extends BaseBean {
    @Schema(description = "总数量")
    private Integer totalCount = 0;

    @Schema(description = "oow总数量")
    private Integer totalOowCount = 0;

    @Schema(description = "ooc总数量")
    private Integer totalOocCount = 0;

    @Schema(description = "oos总数量")
    private Integer totalOosCount = 0;

    @Schema(description = "oo3总数量")
    private Integer totalOo3Count = 0;

    @Schema(description = "normal总数量")
    private Integer totalNormalCount = 0;

    @Schema(description = "单个指标的统计数量")
    private List<SpcAnalysisResultCountBO> resultCountDTOList;

    @Schema(description = "SPC分析结果列表")
    private PageResponse<SpcAnalysisResultDTO> pageResponse;

}
