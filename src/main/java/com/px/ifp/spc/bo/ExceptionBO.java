package com.px.ifp.spc.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ExceptionBO {
    @Schema(description = "报警类型(oos/oow/ooc/oo3)")
    private String alermType;
    @Schema(description = "异常值")
    private BigDecimal pointValue;
    @Schema(description = "事件发生时间")
    private Date eventTime;
    @Schema(description = "异常批注内容")
    private String content;
}
