package com.px.ifp.spc.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;

/**
 * SPC Kafka配置属性
 * 注意：当前实现不支持配置热加载，需要重启应用才能生效
 */
@Data
@Component
@ConfigurationProperties(prefix = "spc.kafka")
public class SpcKafkaProperties {
    
    /**
     * 环境模式：prd / mock
     */
    private String mode = "prd";
    
    /**
     * Topic配置
     */
    private Topics topics = new Topics();

    /**
     * 消费者配置
     */
    private Consumer consumer = new Consumer();

    /**
     * 消息格式映射配置
     */
    private MessageFormat messageFormat = new MessageFormat();
    

    
    @Data
    public static class Topics {
        /**
         * 监控数据Topic
         */
        private TopicConfig monitorData = new TopicConfig();

        @Data
        public static class TopicConfig {
            /**
             * 正式环境Topic名称（支持逗号分隔的多个Topic）
             */
            private String prd;
            
            /**
             * Mock环境Topic名称（支持逗号分隔的多个Topic）
             */
            private String mock;
            
            /**
             * 获取正式环境Topic数组
             */
            public String[] getPrdTopics() {
                return parseTopics(prd);
            }
            
            /**
             * 获取Mock环境Topic数组
             */
            public String[] getMockTopics() {
                return parseTopics(mock);
            }
            
            /**
             * 解析逗号分隔的Topic字符串
             */
            private String[] parseTopics(String topicsStr) {
                if (topicsStr == null || topicsStr.trim().isEmpty()) {
                    return new String[0];
                }
                return Arrays.stream(topicsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
            }
        }
    }

    @Data
    public static class Consumer {
        /**
         * SPC报警系统专用消费组ID
         */
        private String groupId = "spc-alarm-processor-unified";

        /**
         * 并发消费者数量
         */
        private Integer concurrency;
    }

    @Data
    public static class MessageFormat {
        /**
         * 新格式Topic列表（逗号分隔）
         * 这些Topic中的消息使用BusinessMonitorData格式
         */
        private String businessFormatTopics = "";

        /**
         * 获取新格式Topic数组
         */
        public String[] getBusinessFormatTopicArray() {
            if (businessFormatTopics == null || businessFormatTopics.trim().isEmpty()) {
                return new String[0];
            }
            return Arrays.stream(businessFormatTopics.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        }

        /**
         * 检查指定Topic是否使用新格式
         */
        public boolean isBusinessFormat(String topicFromMessage) {
            if (topicFromMessage == null || topicFromMessage.trim().isEmpty()) {
                return false;
            }
            String[] businessTopics = getBusinessFormatTopicArray();
            return Arrays.asList(businessTopics).contains(topicFromMessage.trim());
        }
    }


    /**
     * 根据当前环境模式获取监控数据Topic数组
     */
    public String[] getMonitorDataTopics() {
        return "prd".equals(mode) ? topics.monitorData.getPrdTopics() : topics.monitorData.getMockTopics();
//        return new String[]{"spc-monitor-data","ods_ifp_business_monitor_rti"};
    }

    /**
     * 是否为Mock环境
     */
    public boolean isMockMode() {
        return "mock".equals(mode);
    }
    
    /**
     * 配置初始化后验证
     */
    @PostConstruct
    public void validateConfig() {
        if (topics.monitorData.prd == null && topics.monitorData.mock == null) {
            throw new IllegalArgumentException("至少需要配置一个监控数据Topic（prd或mock）");
        }

        if (mode == null || mode.trim().isEmpty()) {
            throw new IllegalArgumentException("环境模式不能为空");
        }

        if (!"prd".equals(mode) && !"mock".equals(mode)) {
            throw new IllegalArgumentException("环境模式只能是 prd 或 mock");
        }
    }
}
