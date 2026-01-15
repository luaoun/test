package com.px.ifp.spc.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.px.ifp.common.constant.CommonConstants;
import com.px.ifp.common.utils.ThreadLocalUtils;
import com.px.ifp.spc.bo.AlarmSegment;
import com.px.ifp.spc.bo.AlarmState;
import com.px.ifp.spc.entity.SpcAlarmEvent;
import com.px.ifp.spc.service.alarm.SpcAlarmEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 报警状态一致性检查服务
 */
@Slf4j
@Service
public class AlarmStateConsistencyService {

    @Autowired
    private SpcAlarmEventService alarmEventService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AlarmStateRecoveryService recoveryService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 状态不一致类型枚举
     */
    public enum InconsistencyType {
        REDIS_IN_ALARM_DB_NO_ACTIVE,     // Redis显示报警，但数据库无活跃事件
        REDIS_NORMAL_DB_HAS_ACTIVE,      // Redis显示正常，但数据库有活跃事件
        REDIS_SEGMENT_MISMATCH,          // Redis的段ID与数据库不匹配
        REDIS_MISSING,                   // Redis状态缺失，但数据库有活跃事件
        REDIS_TIME_INCONSISTENT,         // Redis的时间与数据库不一致
        REDIS_STATUS_INCONSISTENT,       // Redis的状态值与数据库不一致
        REDIS_SEGMENT_DATA_INCONSISTENT, // Redis段数据与数据库不一致
        CONSISTENT                       // 状态一致
    }

    /**
     * 状态一致性检查结果
     */
    public static class ConsistencyCheckResult {
        private InconsistencyType type;
        private String description;
        private Long indicatorId;
        private String redisState;
        private String redisSegmentId;
        private String dbSegmentId;
        private boolean repaired;

        // getters and setters
        public InconsistencyType getType() { return type; }
        public void setType(InconsistencyType type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getIndicatorId() { return indicatorId; }
        public void setIndicatorId(Long indicatorId) { this.indicatorId = indicatorId; }
        public String getRedisState() { return redisState; }
        public void setRedisState(String redisState) { this.redisState = redisState; }
        public String getRedisSegmentId() { return redisSegmentId; }
        public void setRedisSegmentId(String redisSegmentId) { this.redisSegmentId = redisSegmentId; }
        public String getDbSegmentId() { return dbSegmentId; }
        public void setDbSegmentId(String dbSegmentId) { this.dbSegmentId = dbSegmentId; }
        public boolean isRepaired() { return repaired; }
        public void setRepaired(boolean repaired) { this.repaired = repaired; }
    }

    /**
     * 检查特定指标的状态一致性
     */
    public ConsistencyCheckResult validateStateConsistency(Long indicatorId) {
        ConsistencyCheckResult result = new ConsistencyCheckResult();
        result.setIndicatorId(indicatorId);

        try {
            // 1. 获取Redis状态
            String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);
            String cacheKey = facCode + ":spc:alarm_state:" + indicatorId;
            AlarmState redisState = getAlarmStateFromCache(cacheKey);

            // 2. 获取数据库活跃事件
            List<SpcAlarmEvent> activeEvents = alarmEventService.getActiveAlarmEvents(Collections.singletonList(indicatorId));

            // 3. 执行一致性检查
            result = performConsistencyCheck(redisState, activeEvents, indicatorId);

            // 4. 记录检查结果
            if (result.getType() != InconsistencyType.CONSISTENT) {
                log.warn("状态不一致检测: indicatorId={}, type={}, description={}",
                        indicatorId, result.getType(), result.getDescription());
            } else {
                log.debug("状态一致: indicatorId={}", indicatorId);
            }

            return result;

        } catch (Exception e) {
            log.error("状态一致性检查失败: indicatorId={}", indicatorId, e);
            result.setType(InconsistencyType.REDIS_MISSING);
            result.setDescription("检查过程中发生异常: " + e.getMessage());
            return result;
        }
    }

    /**
     * 执行一致性检查逻辑
     */
    private ConsistencyCheckResult performConsistencyCheck(AlarmState redisState,
                                                           List<SpcAlarmEvent> activeEvents,
                                                           Long indicatorId) {
        ConsistencyCheckResult result = new ConsistencyCheckResult();
        result.setIndicatorId(indicatorId);

        boolean hasRedisState = (redisState != null);
        boolean hasActiveEvent = !activeEvents.isEmpty();

        if (hasRedisState) {
            result.setRedisState(redisState.getCurrentState());
            if (redisState.getAlarmSegment() != null) {
                result.setRedisSegmentId(redisState.getAlarmSegment().getSegmentId());
            }
        }

        if (hasActiveEvent) {
            result.setDbSegmentId(activeEvents.get(0).getSegmentId());
        }

        // 场景1: Redis无状态，数据库有活跃事件
        if (!hasRedisState && hasActiveEvent) {
            result.setType(InconsistencyType.REDIS_MISSING);
            result.setDescription("Redis状态缺失，但数据库存在活跃报警事件");
            return result;
        }

        // 场景2: Redis有状态，数据库无活跃事件
        if (hasRedisState && !hasActiveEvent) {
            if ("IN_ALARM".equals(redisState.getCurrentState())) {
                result.setType(InconsistencyType.REDIS_IN_ALARM_DB_NO_ACTIVE);
                result.setDescription("Redis显示报警状态，但数据库无活跃事件");
            } else {
                result.setType(InconsistencyType.CONSISTENT);
                result.setDescription("状态一致：都为正常状态");
            }
            return result;
        }

        // 场景3: Redis正常状态，数据库有活跃事件
        if (hasRedisState && hasActiveEvent && !"IN_ALARM".equals(redisState.getCurrentState())) {
            result.setType(InconsistencyType.REDIS_NORMAL_DB_HAS_ACTIVE);
            result.setDescription("Redis显示正常状态，但数据库存在活跃报警事件");
            return result;
        }

        // 场景4: 都有状态，进行详细的一致性检查
        if (hasRedisState && hasActiveEvent) {
            SpcAlarmEvent dbEvent = activeEvents.get(0);
            String expectedRedisState = translateDbStatusToStateValue(dbEvent.getStatus());

            // 4.1 检查状态值一致性
            if (!expectedRedisState.equals(redisState.getCurrentState())) {
                result.setType(InconsistencyType.REDIS_STATUS_INCONSISTENT);
                result.setDescription(String.format("Redis状态值不一致: Redis=%s, 数据库转换后=%s",
                        redisState.getCurrentState(), expectedRedisState));
                return result;
            }

            // 4.2 检查时间一致性
            if (dbEvent.getUpdateTime() != null && redisState.getLastUpdateTime() != null) {
                if (!dbEvent.getUpdateTime().equals(redisState.getLastUpdateTime())) {
                    result.setType(InconsistencyType.REDIS_TIME_INCONSISTENT);
                    result.setDescription(String.format("时间不一致: Redis=%s, 数据库=%s",
                            redisState.getLastUpdateTime(), dbEvent.getUpdateTime()));
                    return result;
                }
            }

            // 4.3 如果是报警状态，检查段ID和段详细信息
            if ("IN_ALARM".equals(redisState.getCurrentState())) {
                String redisSegmentId = result.getRedisSegmentId();
                String dbSegmentId = result.getDbSegmentId();

                if (redisSegmentId == null || !redisSegmentId.equals(dbSegmentId)) {
                    result.setType(InconsistencyType.REDIS_SEGMENT_MISMATCH);
                    result.setDescription("Redis和数据库的报警段ID不匹配");
                    return result;
                }

                // 4.4 检查段详细信息一致性
                if (redisState.getAlarmSegment() != null) {
                    ConsistencyCheckResult segmentCheckResult = checkSegmentDataConsistency(
                            redisState.getAlarmSegment(), dbEvent);
                    if (segmentCheckResult.getType() != InconsistencyType.CONSISTENT) {
                        return segmentCheckResult;
                    }
                }
            }

            result.setType(InconsistencyType.CONSISTENT);
            result.setDescription("状态一致：所有关键字段都匹配");
            return result;
        }

        // 场景5: 都无状态
        if (!hasRedisState && !hasActiveEvent) {
            result.setType(InconsistencyType.CONSISTENT);
            result.setDescription("状态一致：都为正常状态");
            return result;
        }

        // 默认情况
        result.setType(InconsistencyType.CONSISTENT);
        result.setDescription("状态检查完成");
        return result;
    }

    /**
     * 修复状态不一致问题
     */
    public boolean repairStateInconsistency(ConsistencyCheckResult checkResult) {
        if (checkResult.getType() == InconsistencyType.CONSISTENT) {
            return true; // 无需修复
        }

        try {
            Long indicatorId = checkResult.getIndicatorId();
            String facCode = (String)ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);

            String cacheKey = facCode + ":spc:alarm_state:" + indicatorId;

            switch (checkResult.getType()) {
                case REDIS_MISSING:
                case REDIS_NORMAL_DB_HAS_ACTIVE:
                case REDIS_SEGMENT_MISMATCH:
                case REDIS_TIME_INCONSISTENT:
                case REDIS_STATUS_INCONSISTENT:
                case REDIS_SEGMENT_DATA_INCONSISTENT:
                    // 从数据库恢复正确状态
                    recoveryService.recoverAlarmStateForIndicator(indicatorId);
                    checkResult.setRepaired(true);
                    log.info("状态修复成功: indicatorId={}, type={}", indicatorId, checkResult.getType());
                    return true;

                case REDIS_IN_ALARM_DB_NO_ACTIVE:
                    // 清除错误的Redis状态
                    redisTemplate.delete(cacheKey);
                    checkResult.setRepaired(true);
                    log.info("清除错误状态: indicatorId={}", indicatorId);
                    return true;

                default:
                    log.warn("未知的不一致类型，无法修复: indicatorId={}, type={}",
                            indicatorId, checkResult.getType());
                    return false;
            }

        } catch (Exception e) {
            log.error("修复状态不一致失败: indicatorId={}, type={}",
                    checkResult.getIndicatorId(), checkResult.getType(), e);
            return false;
        }
    }

    /**
     * 检查并修复状态不一致（组合操作）
     */
    public ConsistencyCheckResult validateAndRepairIfNeeded(Long indicatorId) {
        ConsistencyCheckResult result = validateStateConsistency(indicatorId);

        if (result.getType() != InconsistencyType.CONSISTENT) {
            boolean repaired = repairStateInconsistency(result);
            result.setRepaired(repaired);

            if (repaired) {
                log.info("自动修复状态不一致: indicatorId={}, type={}", indicatorId, result.getType());
            } else {
                log.error("自动修复失败: indicatorId={}, type={}", indicatorId, result.getType());
            }
        }

        return result;
    }

    /**
     * 检查报警段详细信息的一致性
     */
    private ConsistencyCheckResult checkSegmentDataConsistency(AlarmSegment redisSegment,
                                                               SpcAlarmEvent dbEvent) {
        ConsistencyCheckResult result = new ConsistencyCheckResult();
        result.setIndicatorId(dbEvent.getIndicatorId());

        try {
            // 检查关键字段的一致性
            boolean startTimeMatch = checkFieldConsistency("startTime",
                    redisSegment.getStartTime(), dbEvent.getStartTime());
            boolean endTimeMatch = checkFieldConsistency("endTime",
                    redisSegment.getEndTime(), dbEvent.getEndTime());
            boolean alarmTypeMatch = checkFieldConsistency("alarmType",
                    redisSegment.getAlarmType(), dbEvent.getAlarmType());
            boolean pointCountMatch = checkFieldConsistency("pointCount",
                    redisSegment.getPointCount(), dbEvent.getPointCount());
            boolean maxValueMatch = checkFieldConsistency("maxValue",
                    redisSegment.getMaxValue(), dbEvent.getMaxValue());
            boolean minValueMatch = checkFieldConsistency("minValue",
                    redisSegment.getMinValue(), dbEvent.getMinValue());

            // 汇总检查结果
            if (!startTimeMatch || !endTimeMatch || !alarmTypeMatch ||
                    !pointCountMatch || !maxValueMatch || !minValueMatch) {
                result.setType(InconsistencyType.REDIS_SEGMENT_DATA_INCONSISTENT);
                result.setDescription(String.format("段数据不一致: startTime=%b, endTime=%b, alarmType=%b, pointCount=%b, maxValue=%b, minValue=%b",
                        startTimeMatch, endTimeMatch, alarmTypeMatch, pointCountMatch, maxValueMatch, minValueMatch));
                return result;
            }

            result.setType(InconsistencyType.CONSISTENT);
            result.setDescription("段数据一致");
            return result;

        } catch (Exception e) {
            log.warn("检查段数据一致性时出错: indicatorId={}, segmentId={}",
                    dbEvent.getIndicatorId(), dbEvent.getSegmentId(), e);
            result.setType(InconsistencyType.REDIS_SEGMENT_DATA_INCONSISTENT);
            result.setDescription("段数据检查异常: " + e.getMessage());
            return result;
        }
    }

    /**
     * 检查两个字段值是否一致（处理null值）
     */
    private boolean checkFieldConsistency(String fieldName, Object redisValue, Object dbValue) {
        try {
            if (redisValue == null && dbValue == null) {
                return true;
            }
            if (redisValue == null || dbValue == null) {
                log.debug("字段{}一个为null: redis={}, db={}", fieldName, redisValue, dbValue);
                return false;
            }

            boolean isEqual = redisValue.equals(dbValue);
            if (!isEqual) {
                log.debug("字段{}不一致: redis={}, db={}", fieldName, redisValue, dbValue);
            }
            return isEqual;
        } catch (Exception e) {
            log.warn("检查字段{}一致性时出错: redis={}, db={}", fieldName, redisValue, dbValue, e);
            return false;
        }
    }

    /**
     * 将数据库status值转换为标准的AlarmState状态值
     * 与AlarmStateRecoveryService保持一致的转换逻辑
     */
    private String translateDbStatusToStateValue(String dbStatus) {
        if (dbStatus == null) {
            return "IN_ALARM";  // 默认为报警状态（活跃事件）
        }

        // 标准化状态值转换
        switch (dbStatus.toUpperCase()) {
            case "ACTIVE":
            case "TIMEOUT":
                return "IN_ALARM";
            case "INIT":
            case "NORMAL":
            case "RECOVERED":
            case "IGNORED":
                return "INIT";
            default:
                // 对于活跃的报警事件，默认应该是IN_ALARM状态
                log.debug("未识别的数据库状态值: {}, 默认设置为IN_ALARM", dbStatus);
                return "IN_ALARM";
        }
    }

    /**
     * 从缓存获取报警状态（复用AlarmEventProcessor的逻辑）
     */
    private AlarmState getAlarmStateFromCache(String cacheKey) {
        Object cached = null;
        try {
            cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                if (cached instanceof AlarmState) {
                    return (AlarmState) cached;
                } else {
                    if (cached instanceof String) {
                        return objectMapper.readValue((String) cached, AlarmState.class);
                    } else {
                        return objectMapper.convertValue(cached, AlarmState.class);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从缓存获取报警状态失败: {}, 缓存对象类型: {}",
                    cacheKey,
                    cached != null ? cached.getClass().getSimpleName() : "null", e);
        }
        return null;
    }
}