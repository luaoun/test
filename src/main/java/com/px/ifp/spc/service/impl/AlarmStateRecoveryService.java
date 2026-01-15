package com.px.ifp.spc.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.px.ifp.common.constant.CommonConstants;
import com.px.ifp.common.utils.ThreadLocalUtils;
import com.px.ifp.spc.bo.AlarmSegment;
import com.px.ifp.spc.bo.AlarmState;
import com.px.ifp.spc.entity.SpcAlarmEvent;
import com.px.ifp.spc.entity.SpcAnalysisResult;
import com.px.ifp.spc.service.alarm.SpcAlarmEventService;
import com.px.ifp.spc.service.indicator.SpcPointMetaDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 报警状态恢复服务
 */
@Slf4j
@Service
public class AlarmStateRecoveryService {

    @Autowired
    private SpcAlarmEventService alarmEventService;

    @Autowired
    private SpcPointMetaDataService spcIndicatorService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 系统启动时恢复所有指标的状态
     */
    @Transactional(readOnly = true)
    public void recoverAllAlarmStates() {
        try {
            log.info("开始恢复报警状态...");

            // 获取所有有效的指标ID
            List<Long> allIndicatorIds = spcIndicatorService.getAllActiveIndicatorIds();

            if (allIndicatorIds.isEmpty()) {
                log.info("没有找到有效的指标，跳过状态恢复");
                return;
            }

            // 获取所有活跃的报警事件
            List<SpcAlarmEvent> activeEvents = alarmEventService.getActiveAlarmEvents(allIndicatorIds);

            int recoveredCount = 0;
            for (SpcAlarmEvent event : activeEvents) {
                try {
                    recoverAlarmStateForIndicator(event);
                    recoveredCount++;
                } catch (Exception e) {
                    log.error("恢复指标状态失败: indicatorId={}, segmentId={}",
                            event.getIndicatorId(), event.getSegmentId(), e);
                }
            }

            log.info("报警状态恢复完成: 总指标数={}, 活跃报警事件数={}, 成功恢复数={}",
                    allIndicatorIds.size(), activeEvents.size(), recoveredCount);

        } catch (Exception e) {
            log.error("恢复报警状态失败", e);
        }
    }

    /**
     * 为单个指标恢复状态
     */
    private void recoverAlarmStateForIndicator(SpcAlarmEvent activeEvent) {
        try {

            String cacheKey = activeEvent.getFacCode()+":spc:alarm_state:" + activeEvent.getIndicatorId();

            // 检查Redis中是否已存在状态，并验证一致性
            Object existingState = redisTemplate.opsForValue().get(cacheKey);
            if (existingState != null) {
                try {
                    AlarmState cachedState = convertToAlarmState(existingState);

                    // 获取数据库中的关键字段
                    String dbStatus = activeEvent.getStatus();
                    Date dbUpdateTime = activeEvent.getUpdateTime();

                    // 检查关键字段是否一致
                    boolean statusConsistent = Objects.equals(cachedState.getCurrentState(), dbStatus);
                    boolean timeConsistent = Objects.equals(cachedState.getLastUpdateTime(), dbUpdateTime);

                    if (statusConsistent && timeConsistent) {
                        log.debug("Redis状态与数据库一致，跳过恢复: indicatorId={}", activeEvent.getIndicatorId());
                        return;
                    } else {
                        log.info("Redis状态与数据库不一致，强制重写: indicatorId={}, " +
                                        "status: '{}' vs '{}', updateTime: {} vs {}",
                                activeEvent.getIndicatorId(),
                                cachedState.getCurrentState(), dbStatus,
                                cachedState.getLastUpdateTime(), dbUpdateTime);
                        // 继续执行恢复逻辑
                    }
                } catch (Exception e) {
                    log.warn("Redis中的状态数据格式异常，强制重写: indicatorId={}, 类型={}, 错误={}",
                            activeEvent.getIndicatorId(),
                            existingState.getClass().getSimpleName(),
                            e.getMessage());
                    // 继续执行恢复逻辑
                }
            } else {
                log.debug("Redis中不存在状态，执行恢复: indicatorId={}", activeEvent.getIndicatorId());
            }

            // 从数据库重建AlarmState
            AlarmState recoveredState = buildAlarmStateFromEvent(activeEvent);

            // 写入Redis
            redisTemplate.opsForValue().set(cacheKey, recoveredState, 24, TimeUnit.HOURS);

            log.info("恢复报警状态: indicatorId={}, segmentId={}, state={}",
                    activeEvent.getIndicatorId(), activeEvent.getSegmentId(), recoveredState.getCurrentState());

        } catch (Exception e) {
            log.error("恢复指标{}状态失败", activeEvent.getIndicatorId(), e);
            throw e;
        }
    }

    private AlarmState convertToAlarmState(Object obj) throws Exception {
        if (obj == null) {
            return null;
        }

        // 情况1：已经是 AlarmState 类型
        if (obj instanceof AlarmState) {
            return (AlarmState) obj;
        }

        // 情况2：LinkedHashMap 或其他 Map 类型（最常见）
        if (obj instanceof Map) {
            return objectMapper.convertValue(obj, AlarmState.class);
        }

        // 情况3：String 类型（JSON 字符串）
        if (obj instanceof String) {
            return objectMapper.readValue((String) obj, AlarmState.class);
        }

        // 未知类型，尝试转换
        return objectMapper.convertValue(obj, AlarmState.class);
    }

    /**
     * 从数据库事件记录重建AlarmState
     *
     * 说明：
     * - currentState: 使用数据库的status字段而不是硬编码"IN_ALARM"
     * - lastUpdateTime: 使用数据库的update_time字段而不是当前时间
     * - alarmDirection: 由于数据库中没有此字段，仍需从异常点数据获取
     * - 其他字段: 直接从数据库event记录映射，确保数据一致性
     */
    private AlarmState buildAlarmStateFromEvent(SpcAlarmEvent event) {
        // 构建AlarmSegment
        AlarmSegment segment = AlarmSegment.builder()
                .segmentId(event.getSegmentId())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .alarmType(event.getAlarmType())
                .alarmDirection(determineAlarmDirection(event))
                .severityLevel(parseSeverityLevel(event.getSeverityLevel()))
                .maxSeverityLevel(parseSeverityLevel(event.getSeverityLevel()))
                .pointCount(event.getPointCount())
                .maxValue(event.getMaxValue())
                .maxValueTime(event.getMaxValueTime())
                .minValue(event.getMinValue())
                .minValueTime(event.getMinValueTime())
                .currentValue(event.getLastValue()) // 使用lastValue作为当前值
                .lastUpdateTime(event.getLastValueTime())
                .indicatorId(event.getIndicatorId())
                .indicatorName(event.getIndicatorName())
                .point(event.getPoint())
                .peekValue(event.getPeekValue())
                .peekTime(event.getPeekValueTime())
                .oos(event.getOos() != null && event.getOos() ? 1 : 0)
                .ooc(event.getOoc() != null && event.getOoc() ? 1 : 0)
                .oow(event.getOow() != null && event.getOow() ? 1 : 0)
                .oo3(event.getOo3() != null && event.getOo3() ? 1 : 0)
                .endValue(event.getEndValue())
                .endValueTime(event.getEndValueTime())
                // 恢复峰值方向类型（根据报警类型推断）
                .peekDirectionType(inferPeekDirectionType(event.getAlarmType(), determineAlarmDirection(event)))
                .build();

        // 构建AlarmState
        AlarmState state = AlarmState.builder()
                .currentState(translateDbStatusToStateValue(event.getStatus()))  // 使用数据库status字段
                .indicatorId(event.getIndicatorId())
                .alarmSegment(segment)
                .lastUpdateTime(event.getUpdateTime() != null ? event.getUpdateTime() : new Date())  // 使用数据库update_time字段
                .build();

        log.debug("重建状态: indicatorId={}, segmentId={}, alarmType={}, direction={}",
                event.getIndicatorId(), event.getSegmentId(), event.getAlarmType(), segment.getAlarmDirection());

        return state;
    }


    /**
     * 将数据库status值转换为标准的AlarmState状态值
     * 数据库可能存储各种status值，统一转换为：IN_ALARM 或 INIT
     */
    private String translateDbStatusToStateValue(String dbStatus) {
        if (dbStatus == null) {
            return "IN_ALARM";  // 默认为报警状态（活跃事件）
        }

        // 标准化状态值转换
        switch (dbStatus.toUpperCase()) {
            case "IN_ALARM":
                return "IN_ALARM";
            case "INIT":
            case "NORMAL":
            case "RECOVERED":
            case "ENDED":
                return "INIT";
            default:
                // 对于活跃的报警事件（从getActiveAlarmEvents获取），默认应该是IN_ALARM状态
                log.warn("未识别的数据库状态值: {}, 默认设置为IN_ALARM", dbStatus);
                return "IN_ALARM";
        }
    }

    /**
     * 解析严重程度字符串为整数
     * 数据库存储的是数字字符串格式："1","2","3","4"
     * 对应：1=OO3(轻微), 2=OOW(中等), 3=OOC(较严重), 4=OOS(最严重)
     */
    private Integer parseSeverityLevel(String severityLevel) {
        if (severityLevel == null) return 0;

        // 数据库存储的是数字字符串，直接解析
        return Integer.parseInt(severityLevel);
    }

    /**
     * 推断峰值方向类型（用于AlarmSegment的peekDirectionType字段）
     */
    private String inferPeekDirectionType(String alarmType, String alarmDirection) {
        if (alarmType == null || alarmDirection == null) {
            return null;
        }

        // 根据报警类型和方向组合生成方向性报警类型
        String suffix = "UPPER".equals(alarmDirection) ? "_UPPER" : "_LOWER";

        switch (alarmType) {
            case "OOS":
                return "SL" + suffix;  // USL_UPPER 或 LSL_LOWER
            case "OOC":
                return "CL" + suffix;  // UCL_UPPER 或 LCL_LOWER
            case "OOW":
                return "WL" + suffix;  // UWL_UPPER 或 LWL_LOWER
            case "OO3":
                return "3L" + suffix;  // U3L_UPPER 或 L3L_LOWER
            default:
                return null;
        }
    }

    /**
     * 根据报警类型确定报警方向
     * 注意：SpcAlarmEvent表中没有alarm_direction字段，因此需要从异常点数据中获取
     */
    private String determineAlarmDirection(SpcAlarmEvent event) {
        try {
            // 优化1：先尝试通过报警段的首个和最新值推断方向
            String inferredDirection = inferDirectionFromEventValues(event);
            if (inferredDirection != null) {
                return inferredDirection;
            }

            // 优化2：如果无法推断，则查询最少数量的异常点（限制查询数量和时间范围）
            String queryDirection = queryDirectionFromAbnormalPoints(event);
            if (queryDirection != null) {
                return queryDirection;
            }
        } catch (Exception e) {
            log.warn("获取异常点方向信息失败: segmentId={}", event.getSegmentId(), e);
        }

        // 默认值：如果所有方法都失败，根据报警类型推断默认方向
        return getDefaultDirectionByAlarmType(event.getAlarmType());
    }

    /**
     * 通过事件的统计值推断报警方向（避免查询大量异常点）
     */
    private String inferDirectionFromEventValues(SpcAlarmEvent event) {
        try {
            // 如果事件有峰值信息，可以通过峰值和最大/最小值的关系推断
            if (event.getPeekValue() != null && event.getMaxValue() != null && event.getMinValue() != null) {
                // 峰值更接近最大值说明是上限报警，更接近最小值说明是下限报警
                BigDecimal peekToMax = event.getMaxValue().subtract(event.getPeekValue()).abs();
                BigDecimal peekToMin = event.getPeekValue().subtract(event.getMinValue()).abs();

                if (peekToMax.compareTo(peekToMin) < 0) {
                    return "UPPER";  // 峰值更接近最大值
                } else {
                    return "LOWER";  // 峰值更接近最小值
                }
            }

            // 如果没有峰值，但有首值和最新值，可以通过趋势推断
            if (event.getFirstValue() != null && event.getLastValue() != null) {
                BigDecimal change = event.getLastValue().subtract(event.getFirstValue());
                // 如果值持续增长且最大值较大，可能是上限报警
                // 如果值持续下降且最小值较小，可能是下限报警
                if (change.compareTo(BigDecimal.ZERO) > 0 && event.getMaxValue() != null) {
                    return "UPPER";
                } else if (change.compareTo(BigDecimal.ZERO) < 0 && event.getMinValue() != null) {
                    return "LOWER";
                }
            }

            return null;  // 无法推断
        } catch (Exception e) {
            log.debug("通过事件值推断方向失败: segmentId={}", event.getSegmentId(), e);
            return null;
        }
    }

    /**
     * 根据报警类型获取默认方向（最后的兜底策略）
     */
    private String getDefaultDirectionByAlarmType(String alarmType) {
        if (alarmType == null) {
            return "UPPER";  // 默认上限
        }

        // 根据经验，不同报警类型的常见方向
        switch (alarmType) {
            case "OOS":
            case "OOC":
                return "UPPER";  // 规格线和控制线违反通常是上限
            case "OOW":
                return "UPPER";  // 警告线违反也通常是上限
            case "OO3":
                return "LOWER";  // 3σ违反可能是下限
            default:
                return "UPPER";  // 默认上限
        }
    }

    /**
     * 通过查询少量异常点获取方向信息（避免内存溢出）
     */
    private String queryDirectionFromAbnormalPoints(SpcAlarmEvent event) {
        try {
            // 安全检查：如果报警事件持续时间过长，直接跳过查询避免内存溢出
            if (isLongRunningAlarmEvent(event)) {
                log.warn("报警事件持续时间过长，跳过异常点查询避免内存溢出: segmentId={}, duration={}秒",
                        event.getSegmentId(), calculateEventDurationSeconds(event));
                return null;
            }

            // 尝试查询异常点，但加上安全保护
            List<SpcAnalysisResult> allPoints = null;
            try {
                allPoints = alarmEventService.getAlarmEventAbnormalPoints(event.getSegmentId());
            } catch (OutOfMemoryError e) {
                log.error("查询异常点时发生内存溢出: segmentId={}", event.getSegmentId(), e);
                return null;
            } catch (Exception e) {
                log.warn("查询异常点失败: segmentId={}", event.getSegmentId(), e);
                return null;
            }

            if (allPoints != null && !allPoints.isEmpty()) {
                // 如果数据量太大，也跳过处理
                if (allPoints.size() > 10000) {
                    log.warn("异常点数据量过大，跳过处理避免性能问题: segmentId={}, count={}",
                            event.getSegmentId(), allPoints.size());
                    return null;
                }

                // 只取前3个异常点来确定方向（按时间排序）
                List<SpcAnalysisResult> limitedPoints = allPoints.stream()
                        .sorted((a, b) -> a.getEventTime().compareTo(b.getEventTime()))
                        .limit(3)
                        .collect(Collectors.toList());

                if (!limitedPoints.isEmpty()) {
                    // 获取第一个异常点的方向（报警开始时的方向最可靠）
                    SpcAnalysisResult firstPoint = limitedPoints.get(0);

                    String direction = firstPoint.getAlarmDirection();
                    if (direction != null && !"NORMAL".equals(direction)) {
                        log.debug("通过异常点查询获取方向: segmentId={}, direction={}, 总数据量={}, 实际使用=3",
                                event.getSegmentId(), direction, allPoints.size());
                        return direction;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("查询异常点方向失败: segmentId={}", event.getSegmentId(), e);
        }

        return null;
    }

    /**
     * 判断是否为长时间运行的报警事件（避免查询大量数据）
     */
    private boolean isLongRunningAlarmEvent(SpcAlarmEvent event) {
        if (event.getStartTime() == null) {
            return false;
        }

        long durationSeconds = calculateEventDurationSeconds(event);
        // 如果报警持续超过6小时（21600秒），认为是长时间运行的事件
        return durationSeconds > 21600;
    }

    /**
     * 计算事件持续时间（秒）
     */
    private long calculateEventDurationSeconds(SpcAlarmEvent event) {
        if (event.getStartTime() == null) {
            return 0;
        }

        Date endTime = event.getEndTime() != null ? event.getEndTime() : new Date();
        long durationMs = endTime.getTime() - event.getStartTime().getTime();
        return Math.max(0, durationMs / 1000);
    }

    /**
     * 为特定指标恢复状态（按需恢复）
     */
    public void recoverAlarmStateForIndicator(Long indicatorId) {
        try {
            List<SpcAlarmEvent> activeEvents = alarmEventService.getActiveAlarmEvents(Collections.singletonList(indicatorId));

            if (activeEvents.isEmpty()) {
                log.debug("指标{}没有活跃的报警事件", indicatorId);
                return;
            }

            if (activeEvents.size() > 1) {
                log.warn("指标{}存在多个活跃报警事件，使用最新的事件进行恢复", indicatorId);
            }

            // 使用最新的活跃事件
            SpcAlarmEvent latestEvent = activeEvents.stream()
                    .max((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                    .orElse(activeEvents.get(0));

            recoverAlarmStateForIndicator(latestEvent);

        } catch (Exception e) {
            log.error("恢复指标{}状态失败", indicatorId, e);
        }
    }

    /**
     * 清理无效的Redis状态（可选功能）
     */
    public void cleanupInvalidStates() {
        try {
            // 获取所有alarm_state:*的键
            String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);

            String pattern = facCode+":spc:alarm_state:*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                log.info("没有找到需要清理的状态缓存");
                return;
            }

            int cleanedCount = 0;
            for (String key : keys) {
                try {
                    String indicatorIdStr = key.replace(facCode+":spc:alarm_state:", "");
                    Long indicatorId = Long.parseLong(indicatorIdStr);

                    // 检查该指标是否有活跃的报警事件
                    List<SpcAlarmEvent> activeEvents = alarmEventService.getActiveAlarmEvents(Collections.singletonList(indicatorId));

                    if (activeEvents.isEmpty()) {
                        // 没有活跃事件，删除Redis状态
                        redisTemplate.delete(key);
                        cleanedCount++;
                        log.debug("清理无效状态: indicatorId={}", indicatorId);
                    }

                } catch (Exception e) {
                    log.warn("清理状态缓存失败: key={}", key, e);
                }
            }

            log.info("状态清理完成: 总缓存数={}, 清理数={}", keys.size(), cleanedCount);

        } catch (Exception e) {
            log.error("清理无效状态失败", e);
        }
    }
}