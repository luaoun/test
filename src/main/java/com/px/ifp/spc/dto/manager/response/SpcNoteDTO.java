package com.px.ifp.spc.dto.manager.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-07-10
 */
@Data
public class SpcNoteDTO implements Serializable {
    @Schema(description = "主键id")
    private Long id;

    @Schema(description = "业务key")
    private String businessKey;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

    @Schema(description = "批注是否要锁定到SPC曲线上")
    private Boolean tag;
}
