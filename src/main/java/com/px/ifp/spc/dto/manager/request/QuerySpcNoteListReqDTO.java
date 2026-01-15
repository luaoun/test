package com.px.ifp.spc.dto.manager.request;

import com.px.ifp.common.bean.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-07-10
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QuerySpcNoteListReqDTO extends PageRequest {


    @Schema(description = "业务key")
    private String businessKey;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "结束时间")
    private Date endTime;
}
