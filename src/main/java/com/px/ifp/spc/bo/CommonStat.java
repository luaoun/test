package com.px.ifp.spc.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-04-16
 */
@Data
@AllArgsConstructor
public class CommonStat<T> implements Serializable {
    @Schema(description = "名称")
    private String name;

    @Schema(description = "数值")
    private T num;
}
