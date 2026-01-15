package com.px.ifp.spc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.px.ifp.common.constant.CommonConstants;
import com.px.ifp.common.service.redis.RedisService;
import com.px.ifp.common.utils.ThreadLocalUtils;
import com.px.ifp.spc.bo.AlarmSegment;
import com.px.ifp.spc.bo.AlarmState;
import com.px.ifp.spc.bo.RecentPoint;
import com.px.ifp.spc.entity.SpcAlarmEvent;
import com.px.ifp.spc.entity.SpcAnalysisResult;
import com.px.ifp.spc.mapper.SpcAlarmEventMapper;
import com.px.ifp.spc.service.alarm.SpcAlarmEventService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 报警事件状态机处理器
 */
@Slf4j
@Service
public class AlarmEventProcessor {

    @Autowired
    private SpcAlarmEventService alarmEventService;

    @Autowired
    private SpcAlarmEventMapper alarmEventMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private AlarmNotificationService alarmNotificationService;
    @Autowired
    private ObjectMapper objectMapper;

    private final Interner<String> lockKeyInterner = Interners.newWeakInterner();


    /**
     * 处理报警事件
     */
    public void processAlarmEvent(SpcAnalysisResult analysisResult) {
        String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);

        String cacheKey = facCode+":spc:alarm_state:" + analysisResult.getIndicatorId();

        AlarmState currentState = getAlarmStateFromCache(cacheKey);

        // 记录原始状态用于比较
        String originalState = null;
        boolean hasCachedState = (currentState != null);

        if (currentState == null) {
            currentState = AlarmState.builder()
                    .currentState("INIT")
                    .indicatorId(analysisResult.getIndicatorId())
                    .lastUpdateTime(new Date())
                    .build();
            originalState = "INIT";
        } else {
            originalState = currentState.getCurrentState();
        }

        // kafka乱序数据检查：如果当前处于报警状态，且数据时间早于报警开始时间，丢弃数据
        if ("IN_ALARM".equals(currentState.getCurrentState()) &&
                currentState.getAlarmSegment() != null &&
                currentState.getAlarmSegment().getStartTime() != null &&
                analysisResult.getEventTime() != null) {

            Date alarmStartTime = currentState.getAlarmSegment().getStartTime();
            Date dataEventTime = analysisResult.getEventTime();

            if (dataEventTime.before(alarmStartTime)) {
                long timeDiffSeconds = (alarmStartTime.getTime() - dataEventTime.getTime()) / 1000;
                //更新异常数据的报警事件段ID,但是不更新redis缓存的最近
                alarmEventService.updateAnalysisResultSegmentId(analysisResult.getId(), currentState.getAlarmSegment().getSegmentId());
                return; // 直接返回，不处理这条数据
            }
        }

        // 状态机处理
        AlarmState newState = processStateMachine(currentState, analysisResult);

        // 缓存更新策略：
        // 1. 异常数据：总是缓存（开始或继续报警状态，需要更新报警段统计）
        // 2. 正常数据 + 状态改变：缓存（报警结束：IN_ALARM → INIT）
        // 3. 正常数据 + 状态未变化：不缓存（避免重复缓存相同的INIT状态）
        boolean shouldCache = false;
        if (analysisResult.isAnyAbnormal()) {
            // 异常数据：总是需要缓存（更新报警段统计信息）
            shouldCache = true;
        } else if (hasCachedState && !originalState.equals(newState.getCurrentState())) {
            // 正常数据但状态发生改变：缓存（如IN_ALARM → INIT）
            shouldCache = true;
        }
        // else: 正常数据且状态未变化（或无缓存）：不缓存

        if (shouldCache) {
            updateAlarmStateCacheWithDbSync(cacheKey, newState);
        }
    }

    /**
     * 状态机核心处理逻辑
     */
    private AlarmState processStateMachine(AlarmState state, SpcAnalysisResult result) {
        boolean isAbnormal = result.isAnyAbnormal();

        // 添加当前点到最近点列表
        addRecentPoint(state, result, isAbnormal);

        switch (state.getCurrentState()) {
            case "INIT":
                if (isAbnormal) {
                    String lockKey = result.getFacCode()+":"+result.getPoint();
                    String internedKey = lockKeyInterner.intern(lockKey);
                    synchronized (internedKey) {
                        return startNewAlarmSegment(state, result);
                    }
                }
                break;

            case "IN_ALARM":
                if (isAbnormal) {
                    return continueAlarmSegment(state, result);
                } else {
                    // 安全检查：对于明显的异常类型（如UWL_UPPER），即使isAnyAbnormal()返回false，
                    // 也不应该立即结束报警事件，而应该继续监控
                    String directionalType = result.getDirectionalAlarmType();
                    if (!"NORMAL".equals(directionalType)) {
                        // 对于这种不一致情况，暂时继续当前报警段，不立即结束
                        return continueAlarmSegment(state, result);
                    } else {
                        return endAlarmSegment(state, result);
                    }
                }
        }

        return state;
    }


    /**
     * 开始新的报警段
     */
    private AlarmState startNewAlarmSegment(AlarmState state, SpcAnalysisResult result) {
        // 在开始新报警事件前，检查并清理可能的孤儿报警事件
        try {
            boolean hasOrphanCleanup = checkAndCleanupOrphanAlarmEvents(result.getIndicatorId());
            if (hasOrphanCleanup) {
                log.info("在开始新报警段前完成孤儿事件清理: indicatorId={}, point={}",
                        result.getIndicatorId(), result.getPoint());
            }
        } catch (Exception e) {
            // 孤儿清理失败不应该阻止新报警事件的创建
            log.error("孤儿事件清理失败，继续创建新报警事件: indicatorId={}, point={}",
                    result.getIndicatorId(), result.getPoint(), e);
        }

        state.setCurrentState("IN_ALARM");

        // 获取报警类型、方向和严重程度
        String directionalAlarmType = result.getDirectionalAlarmType();
        String alarmDirection = result.getAlarmDirection();
        int severityLevel = getDirectionalSeverityLevel(directionalAlarmType);

        // 创建新的报警段
        AlarmSegment segment = AlarmSegment.builder()
                .segmentId(generateSegmentId())
                .startTime(result.getEventTime())
                .alarmType(extractBaseAlarmType(directionalAlarmType))
                .alarmDirection(extractDirection(directionalAlarmType))
                .severityLevel(severityLevel)
                .maxSeverityLevel(severityLevel)
                .pointCount(1)
                .maxValue(result.getPointValue())
                .maxValueTime(result.getEventTime())
                .minValue(result.getPointValue())
                .minValueTime(result.getEventTime())
                .currentValue(result.getPointValue())
                .lastUpdateTime(result.getEventTime())
                .indicatorId(result.getIndicatorId())
                .indicatorName(result.getIndicatorName())
                .point(result.getPoint())
                // 初始化峰值：根据报警方向设置初始峰值
                .peekValue(result.getPointValue())
                .peekTime(result.getEventTime())
                .peekDirectionType(directionalAlarmType)
                .build();

        // 设置报警类型标记
        setAlarmTypeFlags(segment, extractBaseAlarmType(directionalAlarmType));

        state.setAlarmSegment(segment);
        // 注意：不再手动设置时间，由updateAlarmStateCacheWithDbSync从数据库同步

        // 写入数据库 - 创建报警事件记录
        alarmEventService.createAlarmEvent(segment, result);

        // 触发报警通知
        alarmNotificationService.triggerAlarmNotification(segment, "ALARM_START");

        return state;
    }

    /**
     * 继续报警段
     */
    private AlarmState continueAlarmSegment(AlarmState state, SpcAnalysisResult result) {
        AlarmSegment segment = state.getAlarmSegment();
        String currentDirectionalType = result.getDirectionalAlarmType();
        String currentDirection = result.getAlarmDirection();

        // 检查是否需要切割事件（方向改变时）
        if (shouldCreateNewSegment(segment, currentDirection)) {
            // 方向改变：先结束当前报警事件
            segment.setEndTime(result.getEventTime());

            // 注意：方向改变时，当前值仍然是异常值，不是正常的结束值
            // 但为了完整性，仍然记录当前值作为这个方向报警段的结束值
            segment.setEndValue(result.getPointValue());
            segment.setEndValueTime(result.getEventTime());

            // 写入数据库 - 完成当前报警事件
            alarmEventService.completeAlarmEvent(segment, result);

            // 方向切换时清理历史孤儿事件（排除当前刚结束的段）
            // 注意：不能使用checkAndCleanupOrphanAlarmEvents，因为此时Redis状态为IN_ALARM会导致直接返回
            try {
                cleanupHistoricalOrphanEventsOnEnd(result.getIndicatorId(), segment.getSegmentId());
            } catch (Exception e) {
                // 清理失败不应该影响方向切换的正常流程
                log.error("方向切换时清理历史孤儿事件失败: indicatorId={}, currentSegmentId={}",
                        result.getIndicatorId(), segment.getSegmentId(), e);
            }

            // 创建新的报警段
            int currentSeverityLevel = getDirectionalSeverityLevel(currentDirectionalType);
            AlarmSegment newSegment = AlarmSegment.builder()
                    .segmentId(generateSegmentId())
                    .startTime(result.getEventTime())
                    .alarmType(extractBaseAlarmType(currentDirectionalType))
                    .alarmDirection(extractDirection(currentDirectionalType))
                    .severityLevel(currentSeverityLevel)
                    .maxSeverityLevel(currentSeverityLevel)
                    .pointCount(1)
                    .maxValue(result.getPointValue())
                    .maxValueTime(result.getEventTime())
                    .minValue(result.getPointValue())
                    .minValueTime(result.getEventTime())
                    .currentValue(result.getPointValue())
                    .lastUpdateTime(result.getEventTime())
                    .indicatorId(result.getIndicatorId())
                    .indicatorName(result.getIndicatorName())
                    .point(result.getPoint())
                    // 初始化峰值：方向改变时的新段峰值
                    .peekValue(result.getPointValue())
                    .peekTime(result.getEventTime())
                    .peekDirectionType(currentDirectionalType)
                    .build();

            // 设置报警类型标记
            setAlarmTypeFlags(newSegment, extractBaseAlarmType(currentDirectionalType));

            state.setAlarmSegment(newSegment);
            // 注意：不再手动设置时间，由updateAlarmStateCacheWithDbSync从数据库同步

            // 写入数据库 - 创建新的报警事件
            alarmEventService.createAlarmEvent(newSegment, result);

            // 同步当前值到数据库的最新值字段
            alarmEventService.updateLastValue(segment.getSegmentId(), segment.getCurrentValue(), result.getEventTime());
            // 触发方向切换通知
            alarmNotificationService.triggerAlarmNotification(newSegment, "ALARM_DIRECTION_CHANGE");

            return state;
        }

        // 同方向内，检查是否需要升级严重程度
        int currentSeverityLevel = getDirectionalSeverityLevel(currentDirectionalType);
        String currentBaseType = extractBaseAlarmType(currentDirectionalType);
//        boolean typeChanged = !currentBaseType.equals(segment.getAlarmType());
        boolean severityUpgraded = currentSeverityLevel > segment.getSeverityLevel();

        // 始终更新最高严重程度
        if (segment.getMaxSeverityLevel() == null || currentSeverityLevel > segment.getMaxSeverityLevel()) {
            segment.setMaxSeverityLevel(currentSeverityLevel);
        }

        // 严重等级升级后 才更新 AlarmType和SeverityLevel。不需要 typeChanged变量了
        if (severityUpgraded) {
            // 严重等级升级后，更新最新报警类型和当前严重程度
            String oldType = segment.getAlarmType();
            int oldSeverity = segment.getSeverityLevel();

            segment.setAlarmType(currentBaseType);
            segment.setSeverityLevel(currentSeverityLevel);

            // 更新最高严重程度
            if (segment.getMaxSeverityLevel() == null || currentSeverityLevel > segment.getMaxSeverityLevel()) {
                segment.setMaxSeverityLevel(currentSeverityLevel);
            }

            // 设置报警类型标记（只保留最高等级的标记）
            setAlarmTypeFlags(segment, currentBaseType);

            // 触发升级通知
            if (severityUpgraded) {
                alarmNotificationService.triggerAlarmNotification(segment, "ALARM_ESCALATE");
            }
        }

        // 继续当前报警段
        updateSegmentStatistics(segment, result);

        // 写入数据库 - 更新报警事件进度（包含同步currentValue到lastValue）
        alarmEventService.updateAlarmEventProgress(segment, result);

        // 注意：不再手动设置时间，由updateAlarmStateCacheWithDbSync从数据库同步

        return state;
    }

    /**
     * 结束报警段
     */
    private AlarmState endAlarmSegment(AlarmState state, SpcAnalysisResult result) {
        AlarmSegment segment = state.getAlarmSegment();
        segment.setEndTime(result.getEventTime());

        // 记录触发报警事件结束的正常值信息
        segment.setEndValue(result.getPointValue());
        segment.setEndValueTime(result.getEventTime());

        // 写入数据库 - 完成报警事件
        alarmEventService.completeAlarmEvent(segment, result);

        // 清理该指标的历史孤儿事件（排除当前刚结束的事件）
        try {
            cleanupHistoricalOrphanEventsOnEnd(result.getIndicatorId(), segment.getSegmentId());
        } catch (Exception e) {
            // 清理失败不应该影响当前报警的正常结束流程
            log.error("报警结束时清理历史孤儿事件失败: indicatorId={}, currentSegmentId={}",
                    result.getIndicatorId(), segment.getSegmentId(), e);
        }


        // 触发恢复通知
        alarmNotificationService.triggerAlarmNotification(segment, "ALARM_RECOVERY");

        // 重置状态
        state.setCurrentState("INIT");
        state.setAlarmSegment(null);
        // 注意：INIT状态时没有segmentId，updateAlarmStateCacheWithDbSync会使用当前时间

        return state;
    }

    /**
     * 更新段统计信息
     */
    private void updateSegmentStatistics(AlarmSegment segment, SpcAnalysisResult result) {
        BigDecimal currentValue = result.getPointValue();
        Date eventTime = result.getEventTime();

        // 更新点数
        segment.setPointCount(segment.getPointCount() + 1);
        segment.setLastUpdateTime(eventTime);
        segment.setCurrentValue(currentValue);

        // 更新最大值和最小值
        if (segment.getMaxValue() == null || currentValue.compareTo(segment.getMaxValue()) > 0) {
            segment.setMaxValue(currentValue);
            segment.setMaxValueTime(eventTime);
        }
        if (segment.getMinValue() == null || currentValue.compareTo(segment.getMinValue()) < 0) {
            segment.setMinValue(currentValue);
            segment.setMinValueTime(eventTime);
        }

        // 智能更新峰值：根据方向类型决定使用最大值还是最小值
        String directionalType = result.getDirectionalAlarmType();
        if (directionalType != null) {
            // 使用AlarmSegment的智能峰值更新方法
            segment.updatePeekValue(currentValue, eventTime, directionalType);

        } else {
            // 如果无法确定方向类型，使用传统方法
            if ("UPPER".equals(segment.getAlarmDirection())) {
                // 上限报警：使用最大值作为峰值
                segment.updateMaxValue(currentValue, eventTime);
            } else if ("LOWER".equals(segment.getAlarmDirection())) {
                // 下限报警：使用最小值作为峰值
                segment.updateMinValue(currentValue, eventTime);
            }
        }
    }

    /**
     * 添加最近的数据点
     */
    private void addRecentPoint(AlarmState state, SpcAnalysisResult result, boolean isAbnormal) {
        RecentPoint point = RecentPoint.builder()
                .value(result.getPointValue())
                .time(result.getEventTime())
                .status(result.getAlarmStatus())
                .isAbnormal(isAbnormal)
                .build();

        state.addRecentPoint(point);
    }

    /**
     * 设置报警类型标记字段（保留最高等级）
     */
    private void setAlarmTypeFlags(AlarmSegment segment, String baseAlarmType) {
        // 获取新报警类型的严重程度
        int newSeverity = getSeverityByAlarmType(baseAlarmType);

        // 如果新的严重程度更高，或者是第一次设置，则更新标记
        if (shouldUpdateAlarmTypeFlags(segment, newSeverity)) {
            // 重置所有标记
            segment.setOoc(0);
            segment.setOos(0);
            segment.setOow(0);
            segment.setOo3(0);

            // 根据基础类型设置对应标记（移除OO3支持）
            if ("OOS".equals(baseAlarmType)) {
                segment.setOos(1);  // Out of Specification (最高优先级)
            } else if ("OOC".equals(baseAlarmType)) {
                segment.setOoc(1);  // Out of Control
            } else if ("OOW".equals(baseAlarmType)) {
                segment.setOow(1);  // Out of Warning
            }
            // 不再支持OO3类型的报警
        }
        // 如果新的严重程度低于当前已有的最高等级，保持原有的标记不变
    }

    /**
     * 判断是否应该更新报警类型标记（只有更高等级才更新）
     */
    private boolean shouldUpdateAlarmTypeFlags(AlarmSegment segment, int newSeverity) {
        // 如果还没有设置过任何标记，则需要更新（不包含OO3）
        if ((segment.getOoc() == null || segment.getOoc() == 0) &&
                (segment.getOos() == null || segment.getOos() == 0) &&
                (segment.getOow() == null || segment.getOow() == 0)) {
            return true;
        }

        // 获取当前已设置的最高严重程度
        int currentMaxSeverity = getCurrentMaxSeverityFromFlags(segment);

        // 只有新的严重程度更高时才更新
        return newSeverity > currentMaxSeverity;
    }

    /**
     * 从当前标记中获取最高严重程度（不包含OO3）
     */
    private int getCurrentMaxSeverityFromFlags(AlarmSegment segment) {
        if (segment.getOos() != null && segment.getOos() == 1) return 4;  // OOS 最高
        if (segment.getOoc() != null && segment.getOoc() == 1) return 3;  // OOC
        if (segment.getOow() != null && segment.getOow() == 1) return 2;  // OOW
        // 注意：不再检查OO3标记
        return 0;
    }



    /**
     * 根据报警类型获取严重程度（不包含OO3）
     */
    private int getSeverityByAlarmType(String alarmType) {
        if ("OOS".equals(alarmType)) return 4;  // Out of Specification (最严重)
        if ("OOC".equals(alarmType)) return 3;  // Out of Control
        if ("OOW".equals(alarmType)) return 2;  // Out of Warning
        // 注意：不再支持OO3类型
        return 0;
    }

    /**
     * 从带方向的报警类型中提取基础类型（不包含OO3）
     */
    private String extractBaseAlarmType(String directionalType) {
        if (directionalType == null) return null;

        if (directionalType.contains("SL_")) return "OOS";  // USL/LSL → OOS (最严重)
        if (directionalType.contains("CL_")) return "OOC";  // UCL/LCL → OOC (较严重)
        if (directionalType.contains("WL_")) return "OOW";  // UWL/LWL → OOW (中等)
        // 注意：不再支持 U3L/L3L → OO3

        return "UNKNOWN";
    }

    /**
     * 从带方向的报警类型中提取方向
     */
    private String extractDirection(String directionalType) {
        if (directionalType == null) return "NORMAL";

        if (directionalType.endsWith("_UPPER")) return "UPPER";
        if (directionalType.endsWith("_LOWER")) return "LOWER";

        return "NORMAL";
    }

    /**
     * 获取带方向的严重程度级别（统一处理）
     */
    private int getDirectionalSeverityLevel(String directionalAlarmType) {
        if (directionalAlarmType == null) return 0;

        // 新的优先级：OOS > OOC > OOW (移除OO3)
        if (directionalAlarmType.contains("SL_")) return 4;  // USL/LSL → OOS 最严重
        if (directionalAlarmType.contains("CL_")) return 3;  // UCL/LCL → OOC 较严重
        if (directionalAlarmType.contains("WL_")) return 2;  // UWL/LWL → OOW 中等
        // 注意：不再支持 U3L/L3L → OO3 轻微

        return 0;
    }

    /**
     * 判断是否需要创建新的报警段（方向改变时）
     */
    private boolean shouldCreateNewSegment(AlarmSegment currentSegment, String newDirection) {
        String currentDirection = currentSegment.getAlarmDirection();

        // 如果当前段没有方向，不需要切割
        if (currentDirection == null) {
            return false;
        }

        // 如果新方向为正常，说明报警结束，不需要切割（由状态机处理）
        if ("NORMAL".equals(newDirection)) {
            return false;
        }

        // 从UPPER直接跳到LOWER（或相反），说明跨越了正常区间，需要切割
        if ("UPPER".equals(currentDirection) && "LOWER".equals(newDirection)) {
            return true;
        }
        if ("LOWER".equals(currentDirection) && "UPPER".equals(newDirection)) {
            return true;
        }

        // 同方向的报警升级不需要切割（如LWL_LOWER → LCL_LOWER → LSL_LOWER）
        // 只有真正的方向切换才需要切割
        return false;
    }

    /**
     * 生成事件段ID
     */
    private String generateSegmentId() {
        return "ALM_" + System.currentTimeMillis() + "_" +
                RandomStringUtils.randomNumeric(6);
    }



    /**
     * 从缓存获取报警状态
     */
    private AlarmState getAlarmStateFromCache(String cacheKey) {
        Object cached = null;
        try {
            cached = redisService.getCacheObject(cacheKey);
            if (cached != null) {

                if (cached instanceof AlarmState) {
                    return (AlarmState) cached;
                } else {
                    // 对于非AlarmState类型（如LinkedHashMap、String等），统一使用ObjectMapper转换
                    if (cached instanceof String) {
                        return objectMapper.readValue((String) cached, AlarmState.class);
                    } else {
                        // LinkedHashMap或其他Map类型，使用convertValue转换
                        return objectMapper.convertValue(cached, AlarmState.class);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从缓存获取报警状态失败: {}, 缓存对象类型: {}, 错误信息: {}",
                    cacheKey,
                    cached != null ? cached.getClass().getSimpleName() : "null",
                    e.getMessage(), e);
        }
        return null;
    }

    /**
     * 更新缓存中的报警状态（同步数据库时间）
     */
    private void updateAlarmStateCacheWithDbSync(String cacheKey, AlarmState state) {
        try {
            // 如果有报警段，从数据库同步最新的update_time
            if (state.getAlarmSegment() != null && state.getAlarmSegment().getSegmentId() != null) {
                Date latestDbTime = alarmEventService.getLatestUpdateTime(state.getAlarmSegment().getSegmentId());
                if (latestDbTime != null) {
                    // 使用数据库的实际更新时间
                    state.setLastUpdateTime(latestDbTime);
                } else {
                    state.setLastUpdateTime(new Date());
                }
            } else {
                // 没有segmentId的情况（INIT状态），使用当前时间
                state.setLastUpdateTime(new Date());
            }

            // 更新到Redis
            redisService.setCache(cacheKey, state, 24, TimeUnit.HOURS);

        } catch (Exception e) {
            log.error("更新报警状态缓存失败: key={}", cacheKey, e);
        }
    }

    /**
     * 更新缓存中的报警状态（保留原方法作为回退）
     */
    private void updateAlarmStateCache(String cacheKey, AlarmState state) {
        try {
            // 24小时 = 24 * 60 * 60 = 86400 秒
            redisService.setCache(cacheKey,state);
        } catch (Exception e) {
            log.error("更新报警状态缓存失败: {}", cacheKey, e);
        }
    }

    // ==================== 孤儿报警事件检查和清理逻辑 ====================

    /**
     * 在报警结束时清理历史孤儿事件
     *
     * 与checkAndCleanupOrphanAlarmEvents的区别：
     * 1. 此方法在报警结束时调用，Redis状态正确，只需清理数据库中的历史孤儿
     * 2. 需要排除当前刚结束的事件（通过segmentId过滤）
     * 3. 不进行Redis状态恢复，因为当前Redis状态是正确的
     *
     * @param indicatorId 指标ID
     * @param currentSegmentId 当前刚结束的事件段ID（需要排除）
     */
    private void cleanupHistoricalOrphanEventsOnEnd(Long indicatorId, String currentSegmentId) {
        try {
            // 查找该指标的所有活跃事件
            List<SpcAlarmEvent> activeEvents = findActiveAlarmEventsByIndicator(indicatorId);

            if (activeEvents.isEmpty()) {
                return;
            }

            // 过滤：排除当前刚结束的事件（理论上已经是RESOLVED状态，但双重保险）
            List<SpcAlarmEvent> historicalOrphans = activeEvents.stream()
                    .filter(event -> !event.getSegmentId().equals(currentSegmentId))
                    .collect(Collectors.toList());

            // 发现历史孤儿，进行强制结束
            int cleanedCount = 0;
            for (SpcAlarmEvent orphanEvent : historicalOrphans) {
                try {
                    forceEndOrphanAlarmEvent(orphanEvent,
                            "系统自动修复：报警结束时发现历史孤儿事件（当前事件: " + currentSegmentId + "）");
                    cleanedCount++;
                } catch (Exception e) {
                    log.error("清理历史孤儿事件失败: segmentId={}, indicatorId={}",
                            orphanEvent.getSegmentId(), indicatorId, e);
                }
            }

        } catch (Exception e) {
            log.error("报警结束时清理历史孤儿事件异常: indicatorId={}, currentSegmentId={}",
                    indicatorId, currentSegmentId, e);
            // 注意：这里重新抛出异常，让上层捕获但不影响主流程
            throw e;
        }
    }

    /**
     * 检查并清理指定指标的孤儿报警事件
     * 在开启新报警事件前调用，确保数据一致性
     *
     * @param indicatorId 指标ID
     * @return 是否发现并清理了孤儿记录
     */
    public boolean checkAndCleanupOrphanAlarmEvents(Long indicatorId) {
        try {
            String facCode = (String)ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);

            String cacheKey = facCode + ":spc:alarm_state:" + indicatorId;

            AlarmState redisState = getAlarmStateFromCache(cacheKey);

            // 无论Redis是否有状态，都检查数据库中的ACTIVE记录
            // 因为在多实例场景下，可能存在多条ACTIVE记录（历史孤儿）
            List<SpcAlarmEvent> activeEvents = findActiveAlarmEventsByIndicator(indicatorId);

            if (activeEvents.isEmpty()) {
                return false;
            }

            // 判断需要清理的孤儿事件
            final List<SpcAlarmEvent> orphanEvents;
            final String currentSegmentId;

            if (redisState != null && "IN_ALARM".equals(redisState.getCurrentState())
                    && redisState.getAlarmSegment() != null) {
                // Redis中有IN_ALARM状态，保留对应的segmentId，清理其他ACTIVE记录
                currentSegmentId = redisState.getAlarmSegment().getSegmentId();
                final String segmentIdForFilter = currentSegmentId; // Lambda中使用final变量
                orphanEvents = activeEvents.stream()
                        .filter(event -> !event.getSegmentId().equals(segmentIdForFilter))
                        .collect(Collectors.toList());
            } else {
                // Redis中没有状态或状态为INIT，清理所有ACTIVE记录
                currentSegmentId = null;
                orphanEvents = activeEvents;
            }

            if (orphanEvents.isEmpty()) {
                return false;
            }

            // 发现孤儿记录，进行强制结束
            int cleanedCount = 0;
            for (SpcAlarmEvent orphanEvent : orphanEvents) {
                try {
                    String reason = currentSegmentId != null
                        ? "系统自动修复：检测到多条ACTIVE记录（当前段: " + currentSegmentId + "）"
                        : "系统自动修复：Redis状态丢失或为INIT";
                    forceEndOrphanAlarmEvent(orphanEvent, reason);
                    cleanedCount++;
                } catch (Exception e) {
                    log.error("强制结束孤儿报警事件失败: segmentId={}, indicatorId={}",
                            orphanEvent.getSegmentId(), indicatorId, e);
                }
            }

            if (cleanedCount > 0) {
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("检查孤儿报警事件失败: indicatorId={}", indicatorId, e);
            return false;
        }
    }

    /**
     * 查找指定指标的活跃报警事件
     *
     * @param indicatorId 指标ID
     * @return 活跃的报警事件列表
     */
    private List<SpcAlarmEvent> findActiveAlarmEventsByIndicator(Long indicatorId) {
        QueryWrapper<SpcAlarmEvent> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("indicator_id", indicatorId)
                .eq("status", "ACTIVE")  // 只查找活跃状态的事件
                .isNull("end_time");     // 并且没有结束时间的事件

        return alarmEventMapper.selectList(queryWrapper);
    }

    /**
     * 强制结束孤儿报警事件
     *
     * @param orphanEvent 孤儿报警事件
     * @param reason 强制结束的原因
     */
    private void forceEndOrphanAlarmEvent(SpcAlarmEvent orphanEvent, String reason) {
        try {
            Date now = new Date();

            // 计算持续时间（秒）
            long durationMs = now.getTime() - orphanEvent.getStartTime().getTime();
            int durationSeconds = (int) (durationMs / 1000);

            // 更新报警事件为已结束状态
            SpcAlarmEvent updateEvent = new SpcAlarmEvent();
            updateEvent.setId(orphanEvent.getId());
            updateEvent.setEndTime(now);
            updateEvent.setDurationSeconds(durationSeconds);
            updateEvent.setStatus("RESOLVED");  // 设置为已解决状态
            updateEvent.setHandledBy("SYSTEM_AUTO_CLEANUP");  // 标记为系统自动清理
            updateEvent.setHandleTime(now);
            updateEvent.setHandleNote(reason);
            updateEvent.setUpdateTime(now);

            // 如果没有结束值，使用最后记录的值作为结束值
            if (orphanEvent.getEndValue() == null && orphanEvent.getLastValue() != null) {
                updateEvent.setEndValue(orphanEvent.getLastValue());
                updateEvent.setEndValueTime(orphanEvent.getLastValueTime());
            }

            // 执行更新
            int updateCount = alarmEventMapper.updateById(updateEvent);

        } catch (Exception e) {
            log.error("强制结束孤儿报警事件异常: segmentId={}, indicatorId={}",
                    orphanEvent.getSegmentId(), orphanEvent.getIndicatorId(), e);
            throw e;
        }
    }

    /**
     * 清理所有孤儿报警事件
     */
    public void scheduledCleanupOrphanAlarmEvents() {
        try {
            log.debug("开始定时清理孤儿报警事件任务");

            // 查找所有活跃的报警事件
            QueryWrapper<SpcAlarmEvent> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", "ACTIVE")
                    .isNull("end_time");

            List<SpcAlarmEvent> allActiveEvents = alarmEventMapper.selectList(queryWrapper);

            if (allActiveEvents.isEmpty()) {
                log.debug("未发现活跃的报警事件，无需清理");
                return;
            }

            log.debug("发现{}个活跃报警事件，开始检查孤儿记录", allActiveEvents.size());

            int orphanCount = 0;
            int cleanedCount = 0;
            int failedCount = 0;

            for (SpcAlarmEvent event : allActiveEvents) {
                try {
                    String facCode = event.getFacCode();
                    String cacheKey = facCode + ":spc:alarm_state:" + event.getIndicatorId();
                    AlarmState redisState = getAlarmStateFromCache(cacheKey);

                    // 如果Redis中没有对应的状态，这就是一个孤儿记录
                    if (redisState == null) {
                        orphanCount++;

                        // 添加额外检查：如果事件创建时间过于久远（超过24小时），直接清理
                        long eventAge = System.currentTimeMillis() - event.getCreateTime().getTime();
                        boolean isTooOld = eventAge > Duration.ofHours(24).toMillis();

                        String reason = isTooOld ?
                                "定时清理：事件过于久远且Redis状态丢失" :
                                "定时清理：Redis状态丢失";

                        forceEndOrphanAlarmEvent(event, reason);
                        cleanedCount++;

                        log.info("定时清理孤儿报警事件: segmentId={}, indicatorId={}, 事件年龄={}小时",
                                event.getSegmentId(), event.getIndicatorId(), eventAge / (1000 * 60 * 60));
                    }

                } catch (Exception e) {
                    failedCount++;
                    log.error("定时清理孤儿报警事件失败: segmentId={}, indicatorId={}",
                            event.getSegmentId(), event.getIndicatorId(), e);
                }
            }

            if (orphanCount > 0) {
                log.warn("定时清理孤儿报警事件完成: 总活跃事件={}, 发现孤儿={}, 清理成功={}, 清理失败={}",
                        allActiveEvents.size(), orphanCount, cleanedCount, failedCount);
            } else {
                log.debug("定时清理检查完成: 总活跃事件={}, 未发现孤儿记录", allActiveEvents.size());
            }

        } catch (Exception e) {
            log.error("定时清理孤儿报警事件任务失败", e);
        }
    }
}
