package com.px.ifp.spc.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 报警状态模型 - 用于Redis缓存的状态机
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmState {
    
    /**
     * 当前状态：INIT 或 IN_ALARM
     */
    private String currentState;
    
    /**
     * 当前活跃的报警段
     */
    private AlarmSegment alarmSegment;
    
    /**
     * 最近的数据点（用于趋势分析）
     */
    @Builder.Default
    private List<RecentPoint> recentPoints = new ArrayList<>();
    
    /**
     * 状态更新时间
     */
    private Date lastUpdateTime;
    
    /**
     * 指标ID
     */
    private Long indicatorId;
    
    /**
     * 添加最近的数据点
     */
    public void addRecentPoint(RecentPoint point) {
        recentPoints.add(point);
        // 保持最近50个点
        if (recentPoints.size() > 50) {
            recentPoints.remove(0);
        }
        lastUpdateTime = new Date();
    }
    
    /**
     * 判断是否处于报警状态
     */
    public boolean isInAlarm() {
        return "IN_ALARM".equals(currentState);
    }
    
    /**
     * 重置状态
     */
    public void reset() {
        currentState = "INIT";
        alarmSegment = null;
        lastUpdateTime = new Date();
    }
}
