package com.px.ifp.spc.web.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.px.ifp.common.constant.CommonConstants;
import com.px.ifp.common.utils.ThreadLocalUtils;
import com.px.ifp.spc.properties.SpcKafkaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * SPC实时数据测试控制器 - 用于模拟数据发送和测试
 */
@Slf4j
@RestController
@RequestMapping("/api/spc/realtime")
public class SpcRealtimeController {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private SpcKafkaProperties kafkaProperties;
    
    /**
     * 获取当前应该使用的监控数据Topic
     */
    private String getCurrentMonitorDataTopic() {
        String[] topics = kafkaProperties.getMonitorDataTopics();
        String selectedTopic = topics.length > 0 ? topics[0] : "spc-monitor-data";
        log.debug("当前选择的监控数据Topic: {}, 模式: {}", selectedTopic, kafkaProperties.getMode());
        return selectedTopic;
    }
    

    
    /**
     * 发送模拟监控数据到Kafka
     */
    @PostMapping("/send-mock-data")
    public ResponseEntity<String> sendMockData(@RequestBody MockDataRequest request) {
        try {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < request.getCount(); i++) {
                // 使用正确的MonitorData格式，确保消费者能够正确解析
                //时间随机
                long collectTimestamp = System.currentTimeMillis();
                if(request.getStartTime() != null && request.getEndTime() != null){
                    Random random = new Random();
                    long startMillis = request.getStartTime().getTime();
                    long endMillis = request.getEndTime().getTime();
                    collectTimestamp = startMillis + (long)(random.nextDouble() * (endMillis - startMillis));
                }

                ScadaMockMonitorData.MessageData data = ScadaMockMonitorData.MessageData.builder()
                        .facCode(request.getFacCode())
                        .measureCode(request.getMeasureCode())
                        .value(BigDecimal.valueOf(generateRandomValue(request.getMinValue(), request.getMaxValue())))
                        .collectTimestamp(collectTimestamp)
                        .build();

                String jsonData = objectMapper.writeValueAsString(data);
                sb.append(jsonData);
                // 发送到Kafka - 使用动态Topic
                String topic = getCurrentMonitorDataTopic();
                kafkaTemplate.send(topic, data.getMeasureCode(), jsonData);

                log.debug("发送模拟数据: topic={}, point={}, value={}, mode={}",
                        topic, data.getMeasureCode(), data.getValue(), kafkaProperties.getMode());

                // 如果需要间隔发送，添加延迟
                if (request.getIntervalMs() > 0 && i < request.getCount() - 1) {
                    Thread.sleep(request.getIntervalMs());
                }
            }

            return ResponseEntity.ok("成功发送 " + sb + " 条模拟数据");

        } catch (Exception e) {
            log.error("发送模拟数据失败", e);
            return ResponseEntity.internalServerError().body("发送失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送模拟报警数据（故意产生异常值）
     */
    @PostMapping("/send-alarm-data")
    public ResponseEntity<String> sendAlarmData(@RequestBody AlarmDataRequest request) {
        try {
            // 发送一系列异常值来触发报警
            for (int i = 0; i < request.getAlarmPointCount(); i++) {
                double alarmValue = generateAlarmValue(request, i);
                
                // 使用新的JSON格式
                RawMonitorData data = RawMonitorData.builder()
                    .measure_code(request.getPoint())
                    .value(String.valueOf(alarmValue))
                    .collect_timestamp(System.currentTimeMillis())
                    .fac_code(request.getFacCode() != null ? request.getFacCode() : "FAC_83abf9")
                    .build();
                
                String jsonData = objectMapper.writeValueAsString(data);
                String topic = getCurrentMonitorDataTopic();
                kafkaTemplate.send(topic, data.getMeasure_code(), jsonData);
                
                log.info("发送报警测试数据: topic={}, measure_code={}, value={}, 异常类型={}, mode={}", 
                    topic, data.getMeasure_code(), data.getValue(), request.getAlarmType(), kafkaProperties.getMode());
                
                if (request.getIntervalMs() > 0 && i < request.getAlarmPointCount() - 1) {
                    Thread.sleep(request.getIntervalMs());
                }
            }
            
            // 发送恢复数据
            if (request.isSendRecovery()) {
                Thread.sleep(1000); // 短暂延迟
                double recoveryValue = request.getNormalValue() + ThreadLocalRandom.current().nextGaussian() * 2.0;
                
                RawMonitorData recoveryData = RawMonitorData.builder()
                    .measure_code(request.getPoint())
                    .value(String.valueOf(recoveryValue))
                    .collect_timestamp(System.currentTimeMillis())
                    .fac_code(request.getFacCode() != null ? request.getFacCode() : "FAC_83abf9")
                    .build();
                
                String jsonData = objectMapper.writeValueAsString(recoveryData);
                String topic = getCurrentMonitorDataTopic();
                kafkaTemplate.send(topic, recoveryData.getMeasure_code(), jsonData);
                
                log.info("发送恢复数据: topic={}, measure_code={}, value={}, mode={}", 
                    topic, recoveryData.getMeasure_code(), recoveryData.getValue(), kafkaProperties.getMode());
            }
            
            return ResponseEntity.ok("成功发送报警测试数据");
            
        } catch (Exception e) {
            log.error("发送报警测试数据失败", e);
            return ResponseEntity.internalServerError().body("发送失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量发送模拟数据
     */
    @PostMapping("/send-batch-data")
    public ResponseEntity<String> sendBatchData(@RequestBody BatchDataRequest request) {
        try {
            List<RawMonitorData> dataList = new ArrayList<>();
            
            // 使用新的JSON格式逐个发送每个点位的数据
            for (String point : request.getPoints()) {
                RawMonitorData data = RawMonitorData.builder()
                    .measure_code(point)
                    .value(String.valueOf(generateRandomValue(request.getMinValue(), request.getMaxValue())))
                    .collect_timestamp(System.currentTimeMillis())
                    .fac_code(request.getFacCode() != null ? request.getFacCode() : "FAC_83abf9")
                    .build();
                
                dataList.add(data);
                
                // 发送到Kafka - 使用动态Topic
                String jsonData = objectMapper.writeValueAsString(data);
                String topic = getCurrentMonitorDataTopic();
                kafkaTemplate.send(topic, data.getMeasure_code(), jsonData);
                
                log.debug("批量发送模拟数据: topic={}, measure_code={}, value={}, mode={}", 
                    topic, data.getMeasure_code(), data.getValue(), kafkaProperties.getMode());
            }
            
            log.info("批量发送模拟数据完成: 共发送 {} 个点位", dataList.size());
            
            return ResponseEntity.ok("成功发送批量数据，包含 " + dataList.size() + " 个点位");
            
        } catch (Exception e) {
            log.error("发送批量数据失败", e);
            return ResponseEntity.internalServerError().body("发送失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送原始格式监控数据到Kafka (measure_code格式)
     */
    @PostMapping("/send-raw-monitor-data")
    public ResponseEntity<String> sendRawMonitorData(@RequestBody RawMonitorDataRequest request) {
        try {
            for (int i = 0; i < request.getCount(); i++) {
                RawMonitorData data = generateRawMonitorData(request);
                String jsonData = objectMapper.writeValueAsString(data);
                
                // 发送到Kafka - 使用measure_code作为key，使用动态Topic
                String topic = getCurrentMonitorDataTopic();
                kafkaTemplate.send(topic, data.getMeasure_code(), jsonData);
                
                log.debug("发送原始格式监控数据: topic={}, measure_code={}, value={}, mode={}", 
                    topic, data.getMeasure_code(), data.getValue(), kafkaProperties.getMode());
                
                // 如果需要间隔发送，添加延迟
                if (request.getIntervalMs() > 0 && i < request.getCount() - 1) {
                    Thread.sleep(request.getIntervalMs());
                }
            }
            
            return ResponseEntity.ok("成功发送 " + request.getCount() + " 条原始格式监控数据");
            
        } catch (Exception e) {
            log.error("发送原始格式监控数据失败", e);
            return ResponseEntity.internalServerError().body("发送失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量发送原始格式监控数据
     */
    @PostMapping("/send-batch-raw-data")
    public ResponseEntity<String> sendBatchRawData(@RequestBody BatchRawDataRequest request) {
        try {
            List<RawMonitorData> dataList = new ArrayList<>();
            
            for (String measureCode : request.getMeasureCodes()) {
                RawMonitorData data = RawMonitorData.builder()
                    .measure_code(measureCode)
                    .value(String.valueOf(generateRandomValue(request.getMinValue(), request.getMaxValue())))
                    .collect_timestamp(System.currentTimeMillis())
                    .fac_code(request.getFacCode())
                    .build();
                
                dataList.add(data);
                
                // 发送到Kafka - 使用动态Topic
                String jsonData = objectMapper.writeValueAsString(data);
                String topic = getCurrentMonitorDataTopic();
                kafkaTemplate.send(topic, data.getMeasure_code(), jsonData);
                
                log.debug("批量发送原始监控数据: topic={}, measure_code={}, value={}, mode={}", 
                    topic, data.getMeasure_code(), data.getValue(), kafkaProperties.getMode());
            }
            
            return ResponseEntity.ok("成功批量发送 " + dataList.size() + " 条原始格式监控数据");
            
        } catch (Exception e) {
            log.error("批量发送原始格式监控数据失败", e);
            return ResponseEntity.internalServerError().body("发送失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送指定JSON格式的数据到Kafka
     */
    @PostMapping("/send-custom-json")
    public ResponseEntity<String> sendCustomJson(@RequestBody Map<String, Object> jsonData) {
        try {
            String jsonString = objectMapper.writeValueAsString(jsonData);
            
            // 尝试从JSON中提取key，优先使用measure_code，其次使用point
            String key = null;
            if (jsonData.containsKey("measure_code")) {
                key = jsonData.get("measure_code").toString();
            } else if (jsonData.containsKey("point")) {
                key = jsonData.get("point").toString();
            } else {
                key = "default_key";
            }
            
            String topic = getCurrentMonitorDataTopic();
            kafkaTemplate.send(topic, key, jsonString);
            
            log.info("发送自定义JSON数据: topic={}, key={}, data={}, mode={}", 
                topic, key, jsonString, kafkaProperties.getMode());
            
            return ResponseEntity.ok("成功发送自定义JSON数据");
            
        } catch (Exception e) {
            log.error("发送自定义JSON数据失败", e);
            return ResponseEntity.internalServerError().body("发送失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前的Kafka配置信息
     */
    @GetMapping("/kafka-config")
    public ResponseEntity<Map<String, Object>> getKafkaConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("mode", kafkaProperties.getMode());
        config.put("isMockMode", kafkaProperties.isMockMode());
        config.put("currentMonitorDataTopic", getCurrentMonitorDataTopic());
        config.put("allMonitorDataTopics", kafkaProperties.getMonitorDataTopics());

        // 显示完整的topic配置
        Map<String, Object> topicConfig = new HashMap<>();

        Map<String,String> monitorData = new HashMap<String,String>(){
            {
                put("prod",kafkaProperties.getTopics().getMonitorData().getPrd());
                put("mock",kafkaProperties.getTopics().getMonitorData().getMock());
            }
        };
        topicConfig.put("monitorData", monitorData);
        config.put("topicConfig", topicConfig);
        
        return ResponseEntity.ok(config);
    }
    
    /**
     * 清除Redis中所有SPC相关数据
     */
    @DeleteMapping("/clear-redis-data")
    public ResponseEntity<String> clearSpcRedisData() {
        try {
            // 查找所有报警状态相关的键
            Set<String> alarmStateKeys = redisTemplate.keys("FAC_83abf9:spc:alarm_state:*");
            
            // 查找所有可能的SPC相关键（如果有其他前缀，可以在这里添加）
            Set<String> spcKeys = new HashSet<>();
            if (alarmStateKeys != null) {
                spcKeys.addAll(alarmStateKeys);
            }

            alarmStateKeys = redisTemplate.keys("FAC_93abf6:spc:alarm_state:*");
            if (alarmStateKeys != null) {
                spcKeys.addAll(alarmStateKeys);
            }

            if (spcKeys.isEmpty()) {
                log.info("Redis中没有找到SPC相关数据");
                return ResponseEntity.ok("Redis中没有SPC相关数据需要清除");
            }
            
            // 批量删除键
            Long deletedCount = redisTemplate.delete(spcKeys);
            
            log.info("成功清除Redis中的SPC数据，删除键数量: {}", deletedCount);
            log.debug("删除的键: {}", spcKeys);
            
            return ResponseEntity.ok(String.format("成功清除Redis中的SPC数据，删除了 %d 个键", deletedCount));
            
        } catch (Exception e) {
            log.error("清除Redis中SPC数据失败", e);
            return ResponseEntity.internalServerError().body("清除失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取Redis中SPC数据统计信息
     */
    @GetMapping("/redis-data-stats")
    public ResponseEntity<Map<String, Object>> getSpcRedisDataStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);
            // 统计报警状态键
            Set<String> alarmStateKeys = redisTemplate.keys(facCode+":spc:alarm_state:*");
            stats.put("alarmStateCount", alarmStateKeys != null ? alarmStateKeys.size() : 0);
            
            // 总计
            int totalCount = (alarmStateKeys != null ? alarmStateKeys.size() : 0);
            stats.put("totalCount", totalCount);
            
            // 示例键（最多显示10个）
            Set<String> allKeys = new HashSet<>();
            if (alarmStateKeys != null) {
                allKeys.addAll(alarmStateKeys.stream().limit(5).collect(java.util.stream.Collectors.toSet()));
            }

            stats.put("sampleKeys", allKeys);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("获取Redis SPC Alarm 数据统计失败", e);

            Map<String,Object> immutableMap = Collections.unmodifiableMap(new HashMap<String,Object>(){
                {
                    put("error",e.getMessage());
                }
            });

            return ResponseEntity.internalServerError().body(immutableMap);
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ScadaMockMonitorData{
        @com.fasterxml.jackson.annotation.JsonProperty("Message")
        private MessageData message;
        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class MessageData {
            @com.fasterxml.jackson.annotation.JsonProperty("fac_code")
            private String facCode;

            @com.fasterxml.jackson.annotation.JsonProperty("measure_code")
            private String measureCode;

            @com.fasterxml.jackson.annotation.JsonProperty("value")
            private BigDecimal value;

            @com.fasterxml.jackson.annotation.JsonProperty("collect_timestamp")
            private Long collectTimestamp;
        }
    }

    // 业务监控数据类（新格式）- 用于构建发送消息，保留Topic字段
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BusinessMockMonitorData {
        @com.fasterxml.jackson.annotation.JsonProperty("Topic")
        private String topic;

        @com.fasterxml.jackson.annotation.JsonProperty("Message")
        private MessageData message;

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class MessageData {
            @com.fasterxml.jackson.annotation.JsonProperty("fac_code")
            private String facCode;

            @com.fasterxml.jackson.annotation.JsonProperty("tag")
            private String tag;

            @com.fasterxml.jackson.annotation.JsonProperty("value")
            private Double value;

            @com.fasterxml.jackson.annotation.JsonProperty("ts")
            private Long ts;
        }
    }

    // 新格式消息请求类
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BusinessMonitorDataRequest {
        private String tag;                           // 点位标签
        private String facCode = "FAC_83abf9";       // 工厂代码
        private String topic = "ode_ifp_business_momitor_rt1";  // Topic名称
        private int count = 1;                       // 发送条数
        private long intervalMs = 0;                 // 发送间隔
        private double minValue = 0.0;              // 最小值
        private double maxValue = 100.0;            // 最大值
    }

    /**
     * 发送新格式业务监控数据到Kafka
     */
    @PostMapping("/send-business-monitor-data")
    public ResponseEntity<String> sendBusinessMonitorData(@RequestBody BusinessMonitorDataRequest request) {
        try {
            for (int i = 0; i < request.getCount(); i++) {
                // 构建新格式业务监控数据
                BusinessMockMonitorData.MessageData messageData = BusinessMockMonitorData.MessageData.builder()
                        .facCode(request.getFacCode())
                        .tag(request.getTag())
                        .value(generateRandomValue(request.getMinValue(), request.getMaxValue()))
                        .ts(System.currentTimeMillis())
                        .build();

                BusinessMockMonitorData businessData = BusinessMockMonitorData.builder()
                        .topic(request.getTopic())
                        .message(messageData)
                        .build();

                String jsonData = objectMapper.writeValueAsString(businessData);

                // 发送到Kafka - 使用动态Topic
//                String topic = getCurrentMonitorDataTopic();
                String topic = request.getTopic();
                kafkaTemplate.send(topic, request.getTag(), jsonData);

                log.debug("发送新格式业务监控数据: topic={}, tag={}, value={}, mode={}",
                        topic, request.getTag(), messageData.getValue(), kafkaProperties.getMode());

                // 如果需要间隔发送，添加延迟
                if (request.getIntervalMs() > 0 && i < request.getCount() - 1) {
                    Thread.sleep(request.getIntervalMs());
                }
            }

            return ResponseEntity.ok("成功发送 " + request.getCount() + " 条新格式业务监控数据");

        } catch (Exception e) {
            log.error("发送新格式业务监控数据失败", e);
            return ResponseEntity.internalServerError().body("发送失败: " + e.getMessage());
        }
    }
    
    /**
     * 清除指定指标的Redis数据
     */
    @DeleteMapping("/clear-redis-data/{indicatorId}")
    public ResponseEntity<String> clearIndicatorRedisData(@PathVariable String indicatorId) {
        try {
            String facCode = (String)ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);

            String cacheKey = facCode+":spc:alarm_state:" + indicatorId;
            Boolean deleted = redisTemplate.delete(cacheKey);
            
            if (Boolean.TRUE.equals(deleted)) {
                log.info("成功删除指标 {} 的Redis缓存数据", indicatorId);
                return ResponseEntity.ok("成功删除指标 " + indicatorId + " 的缓存数据");
            } else {
                log.info("指标 {} 的Redis缓存数据不存在", indicatorId);
                return ResponseEntity.ok("指标 " + indicatorId + " 的缓存数据不存在");
            }
            
        } catch (Exception e) {
            log.error("删除指标 {} 的Redis缓存数据失败", indicatorId, e);
            return ResponseEntity.internalServerError().body("删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成报警值
     */
    private double generateAlarmValue(AlarmDataRequest request, int index) {
        double value;
        
        // 根据报警类型生成不同的异常值
        switch (request.getAlarmType()) {
            case "HIGH":
                // 生成超高值
                value = request.getNormalValue() + request.getAlarmOffset() * (1 + index * 0.1);
                break;
            case "LOW":
                // 生成超低值
                value = request.getNormalValue() - request.getAlarmOffset() * (1 + index * 0.1);
                break;
            default:
                // 随机生成高低异常值
                value = ThreadLocalRandom.current().nextBoolean() ? 
                    request.getNormalValue() + request.getAlarmOffset() * (1 + index * 0.1) :
                    request.getNormalValue() - request.getAlarmOffset() * (1 + index * 0.1);
        }
        
        return value;
    }
    
    /**
     * 生成原始格式监控数据
     */
    private RawMonitorData generateRawMonitorData(RawMonitorDataRequest request) {
        return RawMonitorData.builder()
            .measure_code(request.getMeasureCode())
            .value(String.valueOf(generateRandomValue(request.getMinValue(), request.getMaxValue())))
            .collect_timestamp(System.currentTimeMillis())
            .fac_code(request.getFacCode())
            .build();
    }

    /**
     * 生成随机值
     */
    private double generateRandomValue(double min, double max) {
        return min + ThreadLocalRandom.current().nextDouble() * (max - min);
    }
    
    // 请求对象
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MockDataRequest {
        private String measureCode;
        private String facCode = "FAC_83abf9";  // 工厂代码
        private int count = 1;
        private long intervalMs = 0;
        private double minValue = 0.0;
        private double maxValue = 100.0;
        private Date startTime;
        private Date endTime;
    }
    
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AlarmDataRequest {
        private String point;
        private String facCode = "FAC_83abf9";  // 工厂代码
        private int alarmPointCount = 3;
        private long intervalMs = 1000;
        private String alarmType = "HIGH"; // HIGH, LOW, RANDOM
        private double normalValue = 50.0;
        private double alarmOffset = 30.0;
        private boolean sendRecovery = true;
        // 保留旧字段以保持兼容性
        private String deviceId = "TEST_DEVICE";
        private String systemCode = "SYS001";
        private String classCode = "DEPT_TEST";
    }
    
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchDataRequest {
        private List<String> points;
        private String facCode = "FAC_83abf9";  // 工厂代码
        private double minValue = 0.0;
        private double maxValue = 100.0;
        // 保留旧字段以保持兼容性
        private String deviceId = "TEST_DEVICE";
        private String systemCode = "SYS001";
        private String classCode = "DEPT_TEST";
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TestPoint {
        private String point;
        private String name;
        private String unit;
        private double minValue;
        private double maxValue;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TestMeasureCode {
        private String measureCode;  // measure_code
        private String name;         // 描述名称
        private String unit;         // 单位
        private double minValue;     // 最小值
        private double maxValue;     // 最大值
        private String facCode;      // 工厂代码
    }
    
    // 批量监控数据类
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchMonitorData {
        private String batchId;
        private String deviceId;
        private String systemCode;
        private String classCode;
        private Date ctime;
        private List<BatchPointData> points;
    }
    
    // 批量点位数据类
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchPointData {
        private String point;
        private Double value;
        private String quality;
    }
    
    // 原始监控数据格式 (对应图片中的JSON格式)
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RawMonitorData {
        private String measure_code;  // 测量点位编码
        private String value;         // 测量值（字符串格式）
        private Long collect_timestamp; // 采集时间戳
        private String fac_code;      // 工厂代码
    }
    
    // 原始监控数据请求
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RawMonitorDataRequest {
        private String measureCode;  // measure_code
        private String facCode = "FAC_83abf9";  // 默认工厂代码
        private int count = 1;
        private long intervalMs = 0;
        private double minValue = 0.0;
        private double maxValue = 100.0;
    }
    
    // 批量原始监控数据请求
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchRawDataRequest {
        private List<String> measureCodes;  // measure_code列表
        private String facCode = "FAC_83abf9";  // 工厂代码
        private double minValue = 0.0;
        private double maxValue = 100.0;
    }
}
