package com.px.ifp.spc.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.px.ifp.common.constant.CommonConstants;
import com.px.ifp.common.utils.ThreadLocalUtils;
import com.px.ifp.spc.bo.AlarmSegment;
import com.px.ifp.spc.bo.AlarmStatistics;
import com.px.ifp.spc.entity.SpcAlarmEvent;
import com.px.ifp.spc.entity.SpcAnalysisResult;
import com.px.ifp.spc.mapper.SpcAlarmEventMapper;
import com.px.ifp.spc.mapper.SpcAnalysisResultMapper;
import com.px.ifp.spc.service.alarm.SpcAlarmEventService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * SPC报警事件服务实现类
 */
@Slf4j
@Service
public class SpcAlarmEventServiceImpl extends ServiceImpl<SpcAlarmEventMapper, SpcAlarmEvent> implements SpcAlarmEventService {
    
    @Autowired
    private SpcAlarmEventMapper alarmEventMapper;
    
    @Autowired
    private SpcAnalysisResultMapper analysisResultMapper;
    
    @Override
    public SpcAlarmEvent createAlarmEvent(AlarmSegment segment, SpcAnalysisResult firstResult) {
        // 1. 创建报警事件主记录（使用优化的构造方式）
        SpcAlarmEvent alarmEvent = new SpcAlarmEvent();
        try {
            alarmEvent.setSegmentId(segment.getSegmentId());
            alarmEvent.setIndicatorId(firstResult.getIndicatorId());
            alarmEvent.setJobId(firstResult.getJobId());
            alarmEvent.setIndicatorName(firstResult.getIndicatorName());
            alarmEvent.setClassCode(firstResult.getClassCode());
            alarmEvent.setIndicatorLevel(firstResult.getIndicatorLevel());
            alarmEvent.setPoint(firstResult.getPoint());
            alarmEvent.setTargetValue(firstResult.getTargetValue());
            alarmEvent.setUclValue(firstResult.getUclValue());
            alarmEvent.setLclValue(firstResult.getLclValue());
            alarmEvent.setUwlValue(firstResult.getUwlValue());
            alarmEvent.setLwlValue(firstResult.getLwlValue());
            alarmEvent.setUslValue(firstResult.getUslValue());
            alarmEvent.setLslValue(firstResult.getLslValue());
            alarmEvent.setU3lValue(firstResult.getU3lValue());
            alarmEvent.setL3lValue(firstResult.getL3lValue());
            alarmEvent.setUaclValue(firstResult.getUaclValue());
            alarmEvent.setLaclValue(firstResult.getLaclValue());
            alarmEvent.setAlarmType(segment.getAlarmType()); // 内部自动转换为bit字段
            alarmEvent.setStartTime(segment.getStartTime());
            alarmEvent.setEndTime(null);
            alarmEvent.setDurationSeconds(null);
            alarmEvent.setPointCount(1);
            alarmEvent.setMaxValue(firstResult.getPointValue());
            alarmEvent.setMaxValueTime(firstResult.getEventTime());
            alarmEvent.setMinValue(firstResult.getPointValue());
            alarmEvent.setMinValueTime(firstResult.getEventTime());
            alarmEvent.setAvgValue(firstResult.getPointValue());
            // 设置首值信息（引发SPC报警事件的第一个值）
            alarmEvent.setFirstValue(firstResult.getPointValue());
            alarmEvent.setFirstValueTime(firstResult.getEventTime());
            // 设置最新值信息（初始时与首值相同）
            alarmEvent.setLastValue(firstResult.getPointValue());
            alarmEvent.setLastValueTime(firstResult.getEventTime());
            // 设置初始峰值
            alarmEvent.setPeekValue(segment.getPeekValue());
            alarmEvent.setPeekValueTime(segment.getPeekTime());
            // 使用最高严重程度作为severityLevel
            Integer maxSeverityLevel = segment.getMaxSeverityLevel();
            if (maxSeverityLevel == null) {
                maxSeverityLevel = segment.getSeverityLevel();
            }
            alarmEvent.setSeverityLevel(String.valueOf(maxSeverityLevel));

            // 设置对应最高等级的报警类型标记
            setAlarmTypeMarkersByMaxSeverity(alarmEvent, maxSeverityLevel);
            alarmEvent.setStatus("ACTIVE");
            alarmEvent.setNotificationSent(false);

            // normal标记的数据 用normal数据的时间
            if("NORMAL".equalsIgnoreCase(segment.getAlarmType()))
                alarmEvent.setCreateTime(segment.getStartTime());
            else
                alarmEvent.setCreateTime(new Date());
            alarmEvent.setFacCode(firstResult.getFacCode());
                
            alarmEventMapper.insert(alarmEvent);
            
            // 2. 更新 spc_analysis_result 中的 segment_id（关联到报警事件）
            updateAnalysisResultSegmentId(firstResult.getId(), segment.getSegmentId());
            
        } catch (Exception e) {
            log.error("创建报警事件失败: {}", segment.getSegmentId(), e);
            // 报警事件创建失败不应影响实时处理，记录日志即可
        }

        return alarmEvent;

    }
    
    @Override
    public void updateAlarmEventProgress(AlarmSegment segment, SpcAnalysisResult currentResult) {
        try {
            // 1. 更新当前分析结果的segment_id
            updateAnalysisResultSegmentId(currentResult.getId(), segment.getSegmentId());

            // 2. 异步更新报警事件统计信息（直接调用@Async方法，无需额外包装）
            updateAlarmEventStatistics(segment);

            // 3. 同步当前值到最新值（将AlarmSegment的currentValue同步到数据库的lastValue）
            if (segment.getCurrentValue() != null) {
                updateLastValue(segment.getSegmentId(), segment.getCurrentValue(), currentResult.getEventTime());
            }

        } catch (Exception e) {
            log.error("更新报警事件进度失败: {}", segment.getSegmentId(), e);
        }
    }
    
    @Override
    public void completeAlarmEvent(AlarmSegment segment, SpcAnalysisResult lastResult) {
        try {
            Date endTime = lastResult.getEventTime();
            long durationSeconds = (endTime.getTime() - segment.getStartTime().getTime()) / 1000;
            
            // 1. 标记恢复点 - 更新最后一个正常点的segment_id
            updateAnalysisResultSegmentId(lastResult.getId(), segment.getSegmentId());

            // 2. 确保最终存储使用最高严重程度和对应的报警类型标记
            Integer maxSeverityLevel = segment.getMaxSeverityLevel();
            if (maxSeverityLevel == null) {
                maxSeverityLevel = segment.getSeverityLevel();
            }
            updateSeverityLevelAndMarkers(segment.getSegmentId(), maxSeverityLevel);

            // 3. 计算最终统计值
            AlarmStatistics statistics = calculateFinalStatistics(segment.getSegmentId());
            
            // 4. 更新主表 - 标记事件结束
            alarmEventMapper.updateAlarmEventOnEnd(
                segment.getSegmentId(),
                endTime,
                (int) durationSeconds,
                segment.getPointCount(),
                statistics.getMaxValue(),
                statistics.getMaxValueTime(),
                statistics.getMinValue(),
                statistics.getMinValueTime(),
                statistics.getAvgValue(),
                statistics.getFirstValue(),
                statistics.getFirstValueTime(),
                statistics.getLastValue(),
                statistics.getLastValueTime(),
                segment.getEndValue(),
                segment.getEndValueTime(),
                segment.getPeekValue(),
                segment.getPeekTime()
            );

        } catch (Exception e) {
            log.error("完成报警事件失败: {}", segment.getSegmentId(), e);
        }
    }
    
    
    @Override
    public List<SpcAlarmEvent> getActiveAlarmEvents(List<Long> indicatorIds) {
        String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);
        return alarmEventMapper.selectActiveAlarmEvents(indicatorIds,facCode);
    }
    
    @Override
    public List<SpcAnalysisResult> getAlarmEventDetails(String segmentId) {
        try {
            List<SpcAnalysisResult> results = analysisResultMapper.selectBySegmentId(segmentId);
            return results;
        } catch (Exception e) {
            log.error("查询报警事件详情失败: segmentId={}", segmentId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<SpcAnalysisResult> getAlarmEventAbnormalPoints(String segmentId) {
        try {
            List<SpcAnalysisResult> abnormalResults = analysisResultMapper.selectAbnormalPointsBySegmentId(segmentId);
            return abnormalResults;
        } catch (Exception e) {
            log.error("查询报警事件异常点失败: segmentId={}", segmentId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<SpcAlarmEvent> getAlarmEventsByTimeRange(Date startTime, 
                                                         Date endTime,
                                                         String systemCode, 
                                                         String classCode,
                                                         String facCode) {

        return alarmEventMapper.selectAlarmEventsByTimeRange(startTime, endTime, systemCode, classCode,facCode);
    }
    
    @Override
    public List<SpcAlarmEvent> getRecentAlarmEventsByIndicatorId(Long indicatorId, Integer limit,String facCode) {
        return alarmEventMapper.selectRecentAlarmEventsByIndicatorId(indicatorId, limit,facCode);
    }
    
    @Override
    public Long countAlarmEventsByTimeRange(Date startTime, Date endTime, String alarmType,String facCode) {
        return alarmEventMapper.countAlarmEventsByTimeRange(startTime, endTime, alarmType,facCode);
    }

    @Override
    public void updateFirstValue(String segmentId, BigDecimal firstValue, Date firstValueTime) {
        try {
            int result = alarmEventMapper.updateFirstValue(segmentId, firstValue, firstValueTime);
            if (result > 0) {
               //
            } else {
                log.warn("更新首值信息失败，未找到对应记录: segmentId={}", segmentId);
            }
        } catch (Exception e) {
            log.error("更新首值信息异常: segmentId={}, firstValue={}", segmentId, firstValue, e);
        }
    }

    @Override
    public void updateLastValue(String segmentId, BigDecimal lastValue, Date lastValueTime) {
        try {
            int result = alarmEventMapper.updateLastValue(segmentId, lastValue, lastValueTime);
            if (result > 0) {
               //
            } else {
                log.warn("更新最新值信息失败，未找到对应记录: segmentId={}", segmentId);
            }
        } catch (Exception e) {
            log.error("更新最新值信息异常: segmentId={}, lastValue={}", segmentId, lastValue, e);
        }
    }

    @Override
    public Map<String, Object> getValueStatisticsByTimeRange(List<String> jobIdList,Date startTime, Date endTime) {
        try {
            String facCode = (String)ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);
            Map<String, Object> statistics = alarmEventMapper.selectValueStatisticsByTimeRange(jobIdList, startTime, endTime,facCode);
            return statistics;
        } catch (Exception e) {
            log.error("查询值统计信息失败: startTime={}, endTime={}", startTime, endTime, e);
            return Collections.unmodifiableMap(new HashMap<>());
        }
    }

    @Override
    public List<Map<String, Object>> getAlarmEventDetails(Date startTime, Date endTime,
                                                          String severityLevel, String status,
                                                          String indicatorName, String point,
                                                          String alarmType, Integer limit, Integer offset) {
        try {
            List<Map<String, Object>> details = alarmEventMapper.selectAlarmEventDetails(
                    startTime, endTime, severityLevel, status, indicatorName, point, alarmType, limit, offset);
            return details;
        } catch (Exception e) {
            log.error("查询报警事件明细失败: startTime={}, endTime={}", startTime, endTime, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> getAlarmEventSummary(Date startTime, Date endTime) {
        try {
            Map<String, Object> summary = alarmEventMapper.selectAlarmEventSummary(startTime, endTime);
            return summary;
        } catch (Exception e) {
            log.error("查询报警事件汇总失败: startTime={}, endTime={}", startTime, endTime, e);
            return Collections.unmodifiableMap(new HashMap<>());
        }
    }

    @Override
    public List<Map<String, Object>> getAlarmEventsByIndicator(Date startTime, Date endTime) {
        try {
            List<Map<String, Object>> indicatorStats = alarmEventMapper.selectAlarmEventsByIndicator(startTime, endTime);
            return indicatorStats;
        } catch (Exception e) {
            log.error("按指标统计报警事件失败: startTime={}, endTime={}", startTime, endTime, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getAlarmEventsTrend(Date startTime, Date endTime) {
        try {
            List<Map<String, Object>> trend = alarmEventMapper.selectAlarmEventsTrend(startTime, endTime);
            return trend;
        } catch (Exception e) {
            log.error("查询报警事件趋势失败: startTime={}, endTime={}", startTime, endTime, e);
            return Collections.emptyList();
        }
    }

    @Override
    public String generateSegmentId() {
        return "ALM_" + System.currentTimeMillis() + "_" +
                RandomStringUtils.randomNumeric(6);
    }

    /**
     * 更新分析结果的segment_id
     */
    @Override
    public void updateAnalysisResultSegmentId(Long analysisResultId, String segmentId) {
        try {
            int result = analysisResultMapper.updateSegmentId(analysisResultId, segmentId);
            if (result > 0) {
                //
            } else {
                log.warn("更新分析结果segment_id失败，未找到对应记录: id={}, segmentId={}", analysisResultId, segmentId);
            }
        } catch (Exception e) {
            log.error("更新分析结果segment_id异常: id={}, segmentId={}", analysisResultId, segmentId, e);
        }
    }
    
    /**
     * 计算最终统计数据（基于 spc_analysis_result 表）
     */
    private AlarmStatistics calculateFinalStatistics(String segmentId) {
        try {
            // 查询该事件段的所有异常分析结果
            List<SpcAnalysisResult> abnormalResults = analysisResultMapper.selectAbnormalPointsBySegmentId(segmentId);
            if (CollectionUtils.isEmpty(abnormalResults)) {
                AlarmStatistics statistics = new AlarmStatistics();
                statistics.setMaxValue(BigDecimal.ZERO);
                statistics.setMaxValueTime(null);
                statistics.setMinValue(BigDecimal.ZERO);
                statistics.setMinValueTime(null);
                statistics.setAvgValue(BigDecimal.ZERO);
                statistics.setFirstValue(null);
                statistics.setFirstValueTime(null);
                statistics.setLastValue(null);
                statistics.setLastValueTime(null);
                return statistics;
            }

            // 按时间排序以获取首值和最新值
            abnormalResults.sort(Comparator.comparing(SpcAnalysisResult::getEventTime));

            // 计算统计值
            SpcAnalysisResult maxResult = abnormalResults.stream()
                    .max(Comparator.comparing(SpcAnalysisResult::getPointValue))
                    .orElse(null);

            SpcAnalysisResult minResult = abnormalResults.stream()
                    .min(Comparator.comparing(SpcAnalysisResult::getPointValue))
                    .orElse(null);

            // 获取首值和最新值（按时间顺序）
            SpcAnalysisResult firstResult = abnormalResults.get(0);
            SpcAnalysisResult lastResult = abnormalResults.get(abnormalResults.size() - 1);

            BigDecimal sum = abnormalResults.stream()
                    .map(SpcAnalysisResult::getPointValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avgValue = sum.divide(BigDecimal.valueOf(abnormalResults.size()), 3, RoundingMode.HALF_UP);

            AlarmStatistics statistics = new AlarmStatistics();

            if (maxResult != null) {
                statistics.setMaxValue(maxResult.getPointValue());
                statistics.setMaxValueTime(maxResult.getEventTime());
            } else {
                statistics.setMaxValue(BigDecimal.ZERO);
                statistics.setMaxValueTime(null);
            }

            if (minResult != null) {
                statistics.setMinValue(minResult.getPointValue());
                statistics.setMinValueTime(minResult.getEventTime());
            } else {
                statistics.setMinValue(BigDecimal.ZERO);
                statistics.setMinValueTime(null);
            }

            // 设置首值和最新值
            statistics.setFirstValue(firstResult.getPointValue());
            statistics.setFirstValueTime(firstResult.getEventTime());
            statistics.setLastValue(lastResult.getPointValue());
            statistics.setLastValueTime(lastResult.getEventTime());

            statistics.setAvgValue(avgValue);
            statistics.setAbnormalPointCount(abnormalResults.size());
            return statistics;
        } catch (Exception e) {
            log.error("计算最终统计数据失败: segmentId={}", segmentId, e);
            AlarmStatistics statistics = new AlarmStatistics();
            statistics.setMaxValue(BigDecimal.ZERO);
            statistics.setMaxValueTime(null);
            statistics.setMinValue(BigDecimal.ZERO);
            statistics.setMinValueTime(null);
            statistics.setAvgValue(BigDecimal.ZERO);
            statistics.setFirstValue(null);
            statistics.setFirstValueTime(null);
            statistics.setLastValue(null);
            statistics.setLastValueTime(null);
            statistics.setAbnormalPointCount(0);
            return statistics;
        }
    }
    
    /**
     * 异步更新数据库中的统计信息
     */
    protected void updateAlarmEventStatistics(AlarmSegment segment) {
        try {
            // 获取首值和最新值信息（需要从segment或数据库查询）
            AlarmStatistics currentStats = calculateFinalStatistics(segment.getSegmentId());

            // 使用最高严重程度更新severityLevel和对应的报警类型标记
            Integer maxSeverityLevel = segment.getMaxSeverityLevel();
            if (maxSeverityLevel == null) {
                maxSeverityLevel = segment.getSeverityLevel();
            }

            // 更新数据库中的 severityLevel 和报警类型标记
            updateSeverityLevelAndMarkers(segment.getSegmentId(), maxSeverityLevel);

            int result = alarmEventMapper.updateEventStatistics(
                    segment.getSegmentId(),
                    segment.getPointCount(),
                    segment.getMaxValue(),
                    segment.getMaxValueTime(),
                    segment.getMinValue(),
                    segment.getMinValueTime(),
                    currentStats.getAvgValue(),
                    currentStats.getFirstValue(),
                    currentStats.getFirstValueTime(),
                    currentStats.getLastValue(),
                    currentStats.getLastValueTime(),
                    segment.getPeekValue(),
                    segment.getPeekTime(),
                    segment.getAlarmType(),
                    String.valueOf(segment.getSeverityLevel())
            );

            if (result > 0) {
                //
            } else {
                log.warn("更新报警事件统计信息失败，未找到对应记录: segmentId={}", segment.getSegmentId());
            }
        } catch (Exception e) {
            log.error("更新报警事件统计信息异常: segmentId={}, alarmType={}",
                    segment.getSegmentId(), segment.getAlarmType(), e);
        }
    }

    /**
     * 更新 severityLevel 和对应的报警类型标记
     */
    private void updateSeverityLevelAndMarkers(String segmentId, Integer maxSeverityLevel) {
        try {
            // 创建临时对象来设置标记
            SpcAlarmEvent tempEvent = new SpcAlarmEvent();
            setAlarmTypeMarkersByMaxSeverity(tempEvent, maxSeverityLevel);

            // 更新数据库
            int result = alarmEventMapper.updateSeverityLevelAndMarkers(
                    segmentId,
                    String.valueOf(maxSeverityLevel),
                    tempEvent.getOoc(),
                    tempEvent.getOos(),
                    tempEvent.getOow(),
                    tempEvent.getOo3()
            );

            if (result > 0) {
                //
            } else {
                log.warn("更新严重程度和标记失败，未找到对应记录: segmentId={}", segmentId);
            }
        } catch (Exception e) {
            log.error("更新严重程度和标记异常: segmentId={}, maxSeverityLevel={}", segmentId, maxSeverityLevel, e);
        }
    }

    /**
     * 根据最高严重程度设置报警类型标记
     */
    private void setAlarmTypeMarkersByMaxSeverity(SpcAlarmEvent alarmEvent, Integer maxSeverityLevel) {
        if (maxSeverityLevel == null) {
            return;
        }

        // 重置所有标记
        alarmEvent.setOoc(false);
        alarmEvent.setOos(false);
        alarmEvent.setOow(false);
        alarmEvent.setOo3(false);

        // 根据最高严重程度设置对应标记
        // 严重程度映射：4=OOS, 3=OOC, 2=OOW, 1=OO3
        if (maxSeverityLevel >= 4) {
            alarmEvent.setOos(true);  // Out of Specification (最高优先级)
        } else if (maxSeverityLevel >= 3) {
            alarmEvent.setOoc(true);  // Out of Control
        } else if (maxSeverityLevel >= 2) {
            alarmEvent.setOow(true);  // Out of Warning
        } else if (maxSeverityLevel >= 1) {
            alarmEvent.setOo3(true);  // Out of 3-sigma (最低优先级)
        }
    }

    @Override
    public Date getLatestUpdateTime(String segmentId) {
        try {
            Date updateTime = alarmEventMapper.selectLatestUpdateTime(segmentId);
            return updateTime;
        } catch (Exception e) {
            log.error("获取最新数据库时间失败: segmentId={}", segmentId, e);
            return new Date(); // 回退到当前时间
        }
    }
}
