package com.px.ifp.spc.bo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 最近数据点模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentPoint {

    /**
     * 数据值
     */
    private BigDecimal value;

    /**
     * 数据时间
     */
    private Date time;

    /**
     * 异常状态
     */
    private String status;

    /**
     * 是否异常
     */
    private Boolean isAbnormal;
}
