package com.px.ifp.spc.remote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-06-27
 */
@Data
public class QuerySystemCategoryDTO implements Serializable {
    @Schema(description = "厂区")
    private String factoryArea;

    @Schema(description = "课室")
    private String className;
}
