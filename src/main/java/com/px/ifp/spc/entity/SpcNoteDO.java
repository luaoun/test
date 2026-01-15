package com.px.ifp.spc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-07-10
 */
@Getter
@Setter
@TableName("spc_note")
@Schema(name = "SpcNote", description = "spc批注表")
public class SpcNoteDO implements Serializable {
    @Schema(description = "主键id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "SPC指标表ID")
    @TableField("INDICATOR_ID")
    private Long indicatorId;

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

    @Schema(description = "更新时间")
    @TableField("update_time")
    private Date updateTime;

    @Schema(description = "是否需要标记")
    @TableField("tag")
    private Boolean tag;

    @Schema(description = "厂区")
    @TableField("FAC_CODE")
    private String facCode;
}