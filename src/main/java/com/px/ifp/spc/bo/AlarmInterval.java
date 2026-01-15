package com.px.ifp.spc.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmInterval {
    /**
     * 报警事件ID
     */
    private Long alarmEventId;

    /**
     * 报警分段ID
     */
    private String segmentId;

    /**
     * 报警类型 (OOW/OOC/OOS)
     */
    private String alarmType;

    /**
     * 区间开始时间 (时间戳，毫秒)
     */
    private Long startTime;

    /**
     * 区间结束时间 (时间戳，毫秒)
     * 如果为null，表示报警正在进行中
     */
    private Long endTime;
}
