package com.px.ifp.spc.service.alarm;

import com.baomidou.mybatisplus.extension.service.IService;
import com.px.ifp.spc.bo.AlarmSegment;
import com.px.ifp.spc.entity.SpcAlarmEvent;
import com.px.ifp.spc.entity.SpcAnalysisResult;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * SPC报警事件服务接口
 */
public interface SpcAlarmEventService extends IService<SpcAlarmEvent> {
    
    /**
     * 创建新的报警事件（报警开始时调用）
     */
    SpcAlarmEvent createAlarmEvent(AlarmSegment segment, SpcAnalysisResult firstResult);
    
    /**
     * 更新报警事件（报警持续过程中调用）
     */
    void updateAlarmEventProgress(AlarmSegment segment, SpcAnalysisResult currentResult);
    
    /**
     * 完成报警事件（报警结束时调用）
     */
    void completeAlarmEvent(AlarmSegment segment, SpcAnalysisResult lastResult);
    
    
    /**
     * 查询活跃的报警事件
     */
    List<SpcAlarmEvent> getActiveAlarmEvents(List<Long> indicatorIds);
    
    /**
     * 查询报警事件的所有明细数据
     */
    List<SpcAnalysisResult> getAlarmEventDetails(String segmentId);
    
    /**
     * 查询报警事件的异常点数据
     */
    List<SpcAnalysisResult> getAlarmEventAbnormalPoints(String segmentId);
    
    /**
     * 查询指定时间范围内的报警事件
     */
    List<SpcAlarmEvent> getAlarmEventsByTimeRange(Date startTime, 
                                                  Date endTime,
                                                  String systemCode, 
                                                  String classCode,
                                                  String facCode);
    
    /**
     * 根据指标ID查询最近的报警事件
     */
    List<SpcAlarmEvent> getRecentAlarmEventsByIndicatorId(Long indicatorId, Integer limit,String facCode);
    
    /**
     * 统计指定时间范围内的报警事件数量
     */
    Long countAlarmEventsByTimeRange(Date startTime, Date endTime, String alarmType,String facCode);

    /**
     * 更新首值信息
     */
    void updateFirstValue(String segmentId, BigDecimal firstValue, Date firstValueTime);

    /**
     * 更新最新值信息
     */
    void updateLastValue(String segmentId, BigDecimal lastValue, Date lastValueTime);

    /**
     * 查询指定时间范围内的首值和最新值统计
     */
    Map<String, Object> getValueStatisticsByTimeRange(List<String> jobIdList,Date startTime, Date endTime);

    /**
     * 查询指定时间范围内的报警事件明细（带完整统计信息）
     */
    List<Map<String, Object>> getAlarmEventDetails(Date startTime, Date endTime,
                                                   String severityLevel, String status,
                                                   String indicatorName, String point,
                                                   String alarmType, Integer limit, Integer offset);

    /**
     * 统计指定时间范围内的报警事件汇总信息
     */
    Map<String, Object> getAlarmEventSummary(Date startTime, Date endTime);

    /**
     * 按指标分组统计报警事件
     */
    List<Map<String, Object>> getAlarmEventsByIndicator(Date startTime, Date endTime);

    /**
     * 按日期分组统计报警事件趋势
     */
    List<Map<String, Object>> getAlarmEventsTrend(Date startTime, Date endTime);

    void updateAnalysisResultSegmentId(Long analysisResultId, String segmentId);

    /**
     * 获取报警事件的最新数据库时间戳（用于同步Redis缓存）
     * @param segmentId 报警段ID
     * @return 数据库中的最新 update_time
     */
    Date getLatestUpdateTime(String segmentId);

    /**
     * 生成事件段ID
     */
    public String generateSegmentId() ;
}
