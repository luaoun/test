package com.px.ifp.spc.bo;

import com.px.ifp.spc.entity.SpcAlarmEvent;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class AlarmBoundary {
    private final SpcAlarmEvent alarmEvent;  // 报警事件引用
    private final boolean isStart;           // 是否为开始点
    private final boolean isEnd;             // 是否为结束点
    private final boolean isOngoing;         // 是否为正在进行中的报警
}
