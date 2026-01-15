 package  com.px.ifp.spc.mq;

 import com.alibaba.fastjson.JSONObject;
 import com.fasterxml.jackson.annotation.JsonProperty;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import com.px.ifp.common.constant.CommonConstants;
 import com.px.ifp.common.utils.ThreadLocalUtils;
 import com.px.ifp.spc.bo.MonitorData;
 import com.px.ifp.spc.entity.SpcAnalysisResult;
 import com.px.ifp.spc.entity.SpcPointMetadataDO;
 import com.px.ifp.spc.mapper.SpcAnalysisResultMapper;
 import com.px.ifp.spc.properties.SpcKafkaProperties;
 import com.px.ifp.spc.service.analysis.SpcAnalysisResultService;
 import com.px.ifp.spc.service.indicator.SpcPointMetaDataService;
 import com.px.ifp.spc.service.cache.LocalSpcIndicatorCache;
 import com.px.ifp.spc.service.impl.AlarmEventProcessor;
 import com.px.ifp.spc.service.impl.AlarmStateConsistencyService;
 import com.px.ifp.spc.service.helper.IdempotencyService;
 import lombok.AllArgsConstructor;
 import lombok.Data;
 import lombok.NoArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
 import org.apache.kafka.clients.consumer.Consumer;
 import org.apache.kafka.clients.consumer.ConsumerRecord;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.data.redis.core.RedisTemplate;
 import org.springframework.kafka.annotation.KafkaListener;
 import org.springframework.kafka.support.KafkaHeaders;
 import org.springframework.messaging.handler.annotation.Header;

 import java.math.BigDecimal;
 import java.util.Date;
 import java.util.List;
 import java.util.Objects;
 import java.util.concurrent.TimeUnit;

/**
 * SPC数据流处理器 - 处理Kafka监控数据
 */
@Slf4j
//@Configuration
public class SpcStreamProcessor {
    
    @Autowired
    private SpcAnalysisResultMapper analysisResultMapper;

    @Autowired
    private SpcAnalysisResultService spcAnalysisResultService;
    
    @Autowired
    private SpcPointMetaDataService spcIndicatorService;
    
    @Autowired
    private AlarmEventProcessor alarmEventProcessor;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpcKafkaProperties kafkaProperties;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private AlarmStateConsistencyService consistencyService;

    @Autowired
    private LocalSpcIndicatorCache localIndicatorCache;

    private int consecutiveAbnormalThreshold = 3;

    private int consecutiveRecoveryThreshold = 3;

    private int counterTtlMinutes = 30;

    /**
     * 处理监控数据消息
     */
    @KafkaListener(
            topics = "#{@spcKafkaProperties.getMonitorDataTopics()}",
            groupId = "#{@spcKafkaProperties.consumer.groupId}",
            containerFactory = "spcKafkaListenerContainerFactory"
    )
    public void processMonitorData(ConsumerRecord<String, String> record,
                                   Consumer<String, String> consumer,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        long startTime = System.nanoTime();
        try {

            // 从 ConsumerRecord 获取消息信息
            String message = record.value();
            int partition = record.partition();
            long offset = record.offset();

            String facCode = fastExtractFacCode(message, topic);
            String point = fastExtractPoint(message, topic);
            if (point == null) {
                consumer.commitSync();
                return;
            }

            // 性能优化2：优先本地缓存快速过滤 - 检查点位是否配置了SPC指标
            // 使用本地 Caffeine 缓存
            SpcPointMetadataDO indicator = localIndicatorCache.get(facCode,point);
            if (indicator == null) {
                consumer.commitSync(); // 手动提交偏移量
                return;
            }

            MonitorData data = parseMonitorDataSmart(message, topic, partition, offset);
            if (data == null) {
                consumer.commitSync();// 手动提交偏移量
                return;
            }

            // 性能优化3：幂等性检查（布隆过滤器 + Redis）
            String uniqueKey = data.generateUniqueKey();
            if (!idempotencyService.checkAndMarkProcessed(uniqueKey, 5)) {
                // 手动提交偏移量（重复消息也需要确认消费）
                consumer.commitSync();
                return;
            }

            // 处理数据
            try {
                processMonitorDataInternal(data);
            } catch (Exception e) {
                // 处理失败时，移除幂等性标记，允许重试
                idempotencyService.removeProcessedMark(uniqueKey);
                throw e;
            }

            // 手动提交偏移量
            consumer.commitSync();

        } catch (Exception e) {
            log.error("处理监控数据失败: record={}", record, e);
            // 根据错误类型决定是否提交偏移量
            // 这里简单处理：提交偏移量以避免重复处理
            try {
                consumer.commitSync();
            } catch (Exception commitException) {
                log.error("提交偏移量失败", commitException);
            }
        }finally {
            ThreadLocalUtils.remove(CommonConstants.FACTORY_CODE);
        }
    }

    /**
     * 内部处理监控数据逻辑
     */
    private void processMonitorDataInternal(MonitorData data) {
        try {

            // Step 1: 从Redis中获取点位对应的SPC指标配置
            SpcPointMetadataDO indicator = localIndicatorCache.get(data.getFacCode(), data.getPoint());

            if (indicator == null) {
                // 理论上不会到达这里，因为外层已经过滤了
                // 但保留此检查作为防御性编程，并尝试从数据库加载
                log.warn("本地缓存未命中（外层过滤失效），尝试从数据库加载: facCode={}, point={}",
                        data.getFacCode(), data.getPoint());

                List<SpcPointMetadataDO> indicators = spcIndicatorService.getIndicatorsByPoint(data.getFacCode(),data.getPoint());
                if (indicators == null || indicators.isEmpty()) {
                    log.warn("点位未配置SPC指标: point={}", data.getPoint());
                    return;
                }
                indicator = indicators.get(0);
            }

            //这个指标状态是禁用 ，则不计算报警规则
            if(!indicator.getEnableRealtimeAlarm()){
                return;
            }

            // Step 2: 状态一致性检查（在处理前检查并修复状态不一致）
//                try {
//                    consistencyService.validateAndRepairIfNeeded(indicator.getId());
//                } catch (Exception e) {
//                    log.warn("状态一致性检查失败: indicatorId={}, point={}", indicator.getId(), data.getPoint(), e);
//                    // 一致性检查失败不影响正常处理流程
//                }

            // Step 3: 执行SPC异常判断
            SpcAnalysisResult result = calculateSpcStatus(data, indicator);

            // Step 4: 立即保存异常数据到数据库（确保在状态管理前插入）
            if (result.isAnyAbnormal()) {
                try {
                    analysisResultMapper.insert(result);
                } catch (Exception e) {
                    log.error("保存异常数据失败: point={}, indicatorId={}", data.getPoint(), indicator.getId(), e);
                    // 插入失败，不进行后续状态管理，避免数据不一致
                    return;
                }
            }

            // Step 5: 连续异常检测和连续恢复检测
            boolean shouldProcessAlarm;

            if (result.isAnyAbnormal()){
                shouldProcessAlarm = checkAndUpdateConsecutiveAbnormalCount(
                        data.getFacCode(),data.getPoint(),true);
            }else {
                shouldProcessAlarm = checkAndUpdateConsecutiveNormalCount(
                        data.getFacCode(),data.getPoint());
            }

            // Step 6: 实时事件识别和状态管理
            try{
                if(shouldProcessAlarm){
                    alarmEventProcessor.processAlarmEvent(result);
                }else {
                    //isAnyAbnormal = true 跳过报警处理（异常数据未达到连续阈值）
                    //isAnyAbnormal = false 跳过报警处理（正常数据未达到连续阈值）
                }

            }catch (Exception e){
                log.error("处理报警事件失败：point{},indicatorId={}", data.getPoint(),indicator.getId(),e);
            }

        } catch (Exception e) {
            log.error("处理监控数据失败: point={}, value={}", data.getPoint(), data.getValue(), e);
            throw e;
        }
    }
    
    /**
     * 计算SPC状态
     */
    private SpcAnalysisResult calculateSpcStatus(MonitorData data, SpcPointMetadataDO indicator) {
        SpcAnalysisResult result = SpcAnalysisResult.builder()
            .indicatorId(indicator.getId())
            .jobId(indicator.getJobId())
            .indicatorName(indicator.getIndicatorName())
            .classCode(indicator.getClassCode())
            .indicatorLevel(indicator.getIndicatorLevel())
            .point(data.getPoint())
            .pointValue(data.getValue())
            .eventTime(data.getCtime())
            .segmentId(null) // 初始为空，后续在事件处理中设置
            // 复制各种控制线值
            .targetValue(indicator.getTargetValue())
            .uclValue(indicator.getUclValue())
            .lclValue(indicator.getLclValue())
            .uwlValue(indicator.getUwlValue())
            .lwlValue(indicator.getLwlValue())
            .uslValue(indicator.getUslValue())
            .lslValue(indicator.getLslValue())
//            .u3lValue(indicator.getU3lValue())
//            .l3lValue(indicator.getL3lValue())
            .createTime(new Date())
            .facCode(data.getFacCode())
            .build();
        
        BigDecimal pointValue = data.getValue();

        // 异常状态判断 - 只标记最严重的异常类型
        setMostSevereAlarmType(result, pointValue, indicator);
        
        return result;
    }

    /**
     * 设置最严重的报警类型（只标记一个）
     */
    private void setMostSevereAlarmType(SpcAnalysisResult result, BigDecimal pointValue, SpcPointMetadataDO indicator) {
        // 初始化所有标记为false
        result.setOoc(false);
        result.setOos(false);
        result.setOow(false);
        result.setOo3(false);

        // 调试：检查各个控制线值（不包含U3L/L3L，因为不再触发报警）
        log.debug("控制线检查: point={}, value={}, USL={}, LSL={}, UCL={}, LCL={}, UWL={}, LWL={}",
                result.getPoint(), pointValue,
                indicator.getUslValue(), indicator.getLslValue(),
                indicator.getUclValue(), indicator.getLclValue(),
                indicator.getUwlValue(), indicator.getLwlValue());

        // 按严重程度从高到低检查，只标记最严重的
        // 优先级：OOS > OOC > OOW (移除OO3)
        boolean oos = isOutOfSpecification(pointValue, indicator);
        boolean ooc = isOutOfControl(pointValue, indicator);
        boolean oow = isOutOfWarning(pointValue, indicator);
        // 注意：不再检查oo3 = isOutOf3Sigma(pointValue, indicator)

        log.debug("异常检查结果: point={}, value={}, oos={}, ooc={}, oow={}",
                result.getPoint(), pointValue, oos, ooc, oow);

        if (oos) {
            result.setOos(true);  // USL/LSL → OOS (最严重)
            log.debug("设置OOS标志: point={}, value={}", result.getPoint(), pointValue);
        } else if (ooc) {
            result.setOoc(true);  // UCL/LCL → OOC (较严重)
            log.debug("设置OOC标志: point={}, value={}", result.getPoint(), pointValue);
        } else if (oow) {
            result.setOow(true);  // UWL/LWL → OOW (中等)
            log.debug("设置OOW标志: point={}, value={}", result.getPoint(), pointValue);
        } else {
            log.debug("未设置任何异常标志: point={}, value={}", result.getPoint(), pointValue);
        }
        // 如果都不满足，所有标记保持false（正常状态）
        // U3L/L3L线不再触发报警，即使超出也不设置OO3标志
    }
    
    /**
     * 判断是否超出规格线
     */
    private boolean isOutOfSpecification(BigDecimal value, SpcPointMetadataDO indicator) {
        return (indicator.getUslValue() != null && value.compareTo(indicator.getUslValue()) > 0) ||
               (indicator.getLslValue() != null && value.compareTo(indicator.getLslValue()) < 0);
    }
    
    /**
     * 判断是否超出警告线
     */
    private boolean isOutOfWarning(BigDecimal value, SpcPointMetadataDO indicator) {
        return (indicator.getUwlValue() != null && value.compareTo(indicator.getUwlValue()) > 0) ||
               (indicator.getLwlValue() != null && value.compareTo(indicator.getLwlValue()) < 0);
    }
    
    /**
     * 判断是否超出控制线
     */
    private boolean isOutOfControl(BigDecimal value, SpcPointMetadataDO indicator) {
        return (indicator.getUclValue() != null && value.compareTo(indicator.getUclValue()) > 0) ||
               (indicator.getLclValue() != null && value.compareTo(indicator.getLclValue()) < 0);
    }

    /**
     * 判断是否超出3σ线（保留方法但不再用于报警判断）
     * U3L/L3L线不再触发报警，此方法仅作为预留
     */
//    private boolean isOutOf3Sigma(BigDecimal value, SpcPointMetadataDO indicator) {
//        return (indicator.getU3lValue() != null && value.compareTo(indicator.getU3lValue()) > 0) ||
//               (indicator.getL3lValue() != null && value.compareTo(indicator.getU3lValue()) < 0);
//    }

    /**
     * 解析监控数据（基于Topic字段识别格式）
     */
    private MonitorData parseMonitorDataSmart(String message, String topic, int partition, long offset) {
        try {
            // 基于Topic字段判断消息格式
            // 生成消息ID（基于topic+partition+offset）
            String messageId = String.format("%s_%d_%d", topic, partition, offset);
            if (isBusinessMonitorFormatByTopic(message, topic)) {
                BusinessMonitorData.MessageData businessData = objectMapper.readValue(message, BusinessMonitorData.MessageData.class);
                return convertBusinessMonitorData(businessData, messageId);
            } else {
                // 原格式：直接解析为MonitorData
                return parseMonitorData(message,messageId);
            }
        } catch (Exception e) {
            log.error("智能解析监控数据失败: kafkaTopic={}, message={}", topic, message, e);
            return null;
        }
    }

    /**
     * 基于Topic字段检测是否为业务监控数据格式（新格式）
     */
    private boolean isBusinessMonitorFormatByTopic(String message, String kafkaTopic) {
        try {
            // 首先检查消息是否包含Topic字段（新格式特征）
//            if (!message.contains("\"Topic\":")) {
//                return false;
//            }

            // 尝试提取消息中的Topic字段值
//            String topicInMessage = extractTopicFromMessage(message);
            if (message != null) {
                // 使用配置的映射关系判断
                boolean isBusinessFormat = kafkaProperties.getMessageFormat().isBusinessFormat(kafkaTopic);
                return isBusinessFormat;
            }

            // 如果无法提取Topic字段值，说明消息格式有问题，默认使用原格式
            return false;
        } catch (Exception e) {
            log.debug("Topic格式检测失败，默认使用原格式: kafkaTopic={}, error={}", kafkaTopic, e.getMessage());
            return false;
        }
    }

    /**
     * 从消息中提取Topic字段值
     */
    private String extractTopicFromMessage(String message) {
        try {
            // 简单的JSON字段提取（避免完整解析提高性能）
            String pattern = "\"Topic\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(message);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            log.debug("提取Topic字段失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 快速提取工厂代码字段（避免完整 JSON 反序列化）
     *
     * @param message JSON 消息字符串
     * @param topic Kafka topic 名称（用于判断格式）
     * @return facCode 字段值，提取失败返回 null
     */
    private String fastExtractFacCode(String message, String topic) {
        try {
            // 判断消息格式
            boolean isBusinessFormat = kafkaProperties.getMessageFormat().isBusinessFormat(topic);

            if (isBusinessFormat) {
                // 业务格式：提取 "fac_code" 字段
                return fastExtractJsonField(message, "fac_code");
            } else {
                // 标准格式：提取 "fac_code" 字段
                return fastExtractJsonField(message, "fac_code");
            }

        } catch (Exception e) {
            log.debug("快速提取facCode字段失败: topic={}, error={}", topic, e.getMessage());
            return null;
        }
    }

    private String fastExtractPoint(String message, String topic) {
        try {
            // 判断消息格式
            boolean isBusinessFormat = kafkaProperties.getMessageFormat().isBusinessFormat(topic);

            if (isBusinessFormat) {
                // 业务格式：提取 "tag" 字段
                return fastExtractJsonField(message, "tag");
            } else {
                // 标准格式：提取 "measure_code" 字段
                return fastExtractJsonField(message, "measure_code");
            }

        } catch (Exception e) {
            log.debug("快速提取point字段失败: topic={}, error={}", topic, e.getMessage());
            return null;
        }
    }

    private String fastExtractJsonField(String json, String fieldName) {
        try {
            // 查找字段名称的位置：\"fieldName\":
            String searchPattern = "\"" + fieldName + "\"";
            int fieldIndex = json.indexOf(searchPattern);

            if (fieldIndex == -1) {
                return null;
            }

            // 查找冒号位置
            int colonIndex = json.indexOf(':', fieldIndex);
            if (colonIndex == -1) {
                return null;
            }

            // 跳过冒号和空白字符，查找值的起始引号
            int valueStartIndex = -1;
            for (int i = colonIndex + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '"') {
                    valueStartIndex = i + 1; // 跳过起始引号
                    break;
                } else if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                    // 如果遇到非空白字符且不是引号，说明值不是字符串类型
                    return null;
                }
            }

            if (valueStartIndex == -1) {
                return null;
            }

            // 查找值的结束引号（需要处理转义字符）
            int valueEndIndex = -1;
            for (int i = valueStartIndex; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\') {
                    // 跳过转义字符（双反斜杠、引号、斜杠等 JSON 转义）
                    i++; // 跳过下一个字符
                    continue;
                } else if (c == '"') {
                    valueEndIndex = i;
                    break;
                }
            }

            if (valueEndIndex == -1) {
                return null;
            }

            // 提取原始 JSON 字符串值
            String rawValue = json.substring(valueStartIndex, valueEndIndex);

            // 处理 JSON 转义字符
            return unescapeJson(rawValue);

        } catch (Exception e) {
            log.debug("提取JSON字段失败: fieldName={}, error={}", fieldName, e.getMessage());
            return null;
        }
    }

    private String unescapeJson(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return jsonString;
        }

        // 如果不包含反斜杠，直接返回（性能优化）
        if (jsonString.indexOf('\\') == -1) {
            return jsonString;
        }

        StringBuilder result = new StringBuilder(jsonString.length());
        int length = jsonString.length();

        for (int i = 0; i < length; i++) {
            char c = jsonString.charAt(i);

            if (c == '\\' && i + 1 < length) {
                char next = jsonString.charAt(i + 1);

                switch (next) {
                    case '\\':  // \\ -> \
                        result.append('\\');
                        i++;
                        break;
                    case '"':   // \" -> "
                        result.append('"');
                        i++;
                        break;
                    case '/':   // \/ -> /
                        result.append('/');
                        i++;
                        break;
                    case 'b':   // \b -> backspace
                        result.append('\b');
                        i++;
                        break;
                    case 'f':   // \f -> form feed
                        result.append('\f');
                        i++;
                        break;
                    case 'n':   // \n -> newline
                        result.append('\n');
                        i++;
                        break;
                    case 'r':   // \r -> carriage return
                        result.append('\r');
                        i++;
                        break;
                    case 't':   // \t -> tab
                        result.append('\t');
                        i++;
                        break;
                    case 'u':   // Unicode 转义字符（如 \u0041 代表字符 A）
                        // 简化处理：如果遇到 Unicode 转义，暂不处理
                        // 这里为了性能考虑，保持原样
                        result.append(c);
                        break;
                    default:
                        // 非标准转义，保持原样
                        result.append(c);
                        break;
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
    
    /**
     * 解析监控数据
     */
    private MonitorData parseMonitorData(String message,String messageId) {
        try {
            ScadaMonitorData.MessageData scadaMonitorData = JSONObject.parseObject(message, ScadaMonitorData.MessageData.class);
            MonitorData monitorData = new MonitorData();

            if (Objects.nonNull(scadaMonitorData)) {
                monitorData.setMessageId(messageId);
                monitorData.setCtime(new Date(scadaMonitorData.getCollectTimestamp()));
                monitorData.setPoint(scadaMonitorData.getMeasureCode());
                monitorData.setValue(scadaMonitorData.getValue());
                monitorData.setFacCode(scadaMonitorData.getFacCode());
            }

            return monitorData;
        } catch (Exception e) {
            log.error("解析SPC监控数据失败: {}", message, e);
            return null;
        }
    }

    /**
     * 转换业务监控数据为标准监控数据格式
     */
    private MonitorData convertBusinessMonitorData(BusinessMonitorData.MessageData msgData,String messageId) {
        try {
            if (msgData == null) {
                return null;
            }

            // 校验必要字段
            if (msgData.getTag() == null || msgData.getValue() == null || msgData.getTs() == null) {
                return null;
            }

            // 转换时间戳（毫秒 -> Date）
            Date ctime = new Date(msgData.getTs());

            return MonitorData.builder()
                    .messageId(messageId)
                    .facCode(msgData.getFacCode())
                    .point(msgData.getTag())
                    .value(msgData.getValue())
                    .ctime(ctime)
                    .quality("GOOD")  // 新格式默认质量为GOOD
                    .facCode(msgData.getFacCode())  // 工厂代码映射为classCode
                    .classCode(null)
                    .systemCode(null)  // 新格式暂无systemCode
                    .deviceId(null)    // 新格式暂无deviceId
                    .build();

        } catch (Exception e) {
            log.error("转换业务监控数据失败: {}", msgData, e);
            return null;
        }
    }

    private boolean checkAndUpdateConsecutiveAbnormalCount(String facCode,String point,boolean isAbnormal){
        try{
            String redisKey = String.format("%s:spc:consecutive:alarm:%s",facCode,point);

            // 当前数据异常： 递增计数器
            Long currentCount = redisTemplate.opsForValue().increment(redisKey);

            // 设置过期时间（仅在首次创建时设置）
            if(currentCount != null && currentCount == 1){
                redisTemplate.expire(redisKey,counterTtlMinutes, TimeUnit.MINUTES);
            }

            // 检查是否达到阈值
            if(currentCount != null && currentCount >= consecutiveAbnormalThreshold) {
                // 连续异常次数达到阈值，触发报警
                // 达到阈值后重置计数器，避免重复触发
                redisTemplate.delete(redisKey);
                return true;
            }
            // 连续异常次数未达到阈值，暂不触发报警
            return false;
        }catch (Exception e){
            log.error("连续异常计数检查失败：facCode ={},point={}，isAbnormal={}",facCode,point,isAbnormal,e);
            // 出现异常时，采用降级策略：允许触发报警（避免漏报）
            return true;
        }
    }

    private boolean checkAndUpdateConsecutiveNormalCount(String facCode,String point){
        try{
            String recoverKey = String.format("%s:spc:consecutive:recovery:%s",facCode,point);
            String alarmKey = String.format("%s:spc:consecutive:alarm:%s",facCode,point);

            // 当前数据正常；递增恢复计数器
            Long currentCount = redisTemplate.opsForValue().increment(recoverKey);

            // 设置过期时间（仅在首次创建时设置）
            if (currentCount !=null && currentCount == 1){
                redisTemplate.expire(recoverKey,counterTtlMinutes,TimeUnit.MINUTES);
            }

            // 检查是否达到阈值
            if (currentCount != null && currentCount >= consecutiveRecoveryThreshold){
                // 连续正常次数达到阈值，结束报警
                // 达到阈值后清理所有计数器
                redisTemplate.delete(recoverKey);
                redisTemplate.delete(alarmKey);
                return true;
            }
            // 连续正常次数未达到阈值，暂不结束报警
            return false;
        }catch(Exception e){
            log.error("连续正常值计数检查失败: facCode={}, point={}",facCode,point,e);
            return true;
        }
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScadaMonitorData{
        @JsonProperty("Message")
        private BusinessMonitorData.MessageData message;
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MessageData {
            @JsonProperty("fac_code")
            private String facCode;

            @JsonProperty("measure_code")
            private String measureCode;

            @JsonProperty("value")
            private BigDecimal value;

            @JsonProperty("collect_timestamp")
            private Long collectTimestamp;
        }
    }



    /**
     * 业务监控数据内部类（新格式）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessMonitorData {
        @JsonProperty("Message")
        private MessageData message;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MessageData {
            @JsonProperty("fac_code")
            private String facCode;

            @JsonProperty("tag")
            private String tag;

            @JsonProperty("value")
            private BigDecimal value;

            @JsonProperty("ts")
            private Long ts;
        }
    }
}
