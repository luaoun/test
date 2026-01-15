package com.px.ifp.spc.bo;

import com.px.ifp.common.utils.DateUtils;
import com.px.ifp.spc.bo.SpcAlarmNoteBO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-22
 */
@Data
@Builder
@AllArgsConstructor
public class CommonTimeStat implements Serializable {

    public CommonTimeStat(){

    }

    public CommonTimeStat(String alias,long time,BigDecimal value){
        this.alias = alias;
        this.time  = time;
        this.value = value;
        this.formattedTime = DateUtils.format(new Date(time));
    }

    @Schema(description = "时间戳")
    private Long time;

    @Schema(description = "时间戳格式化")
    private String formattedTime;

    @Schema(description = "统计值")
    private BigDecimal value;

    @Schema(description = "point点位")
    private String point;

    @Schema(description = "spc最新报警批注信息")
    private SpcAlarmNoteBO spcAlarmNote;

    @Schema(description = "是否需要标记")
    private boolean tag;

    @Schema(description = "查询字段别名(函数名_value)")
    private String alias;

    @Schema(description = "是否为报警开始点")
    private Boolean isAlarmStart;

    @Schema(description = "是否为报警结束点")
    private Boolean isAlarmEnd;

}
