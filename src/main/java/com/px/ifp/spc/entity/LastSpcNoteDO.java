package com.px.ifp.spc.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("spc_note")
public class LastSpcNoteDO implements Serializable {

    @Schema(description = "指标jobId")
    @TableField("job_id")
    private String jobId;

    @Schema(description = "业务key")
    @TableField("business_key")
    private String businessKey;

    @Schema(description = "内容")
    @TableField("content")
    private String content;

    @Schema(description = "创建者")
    @TableField("creator")
    private String creator;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private Date createTime;

    @Schema(description = "厂区")
    @TableField("FAC_CODE")
    private String facCode;

}
