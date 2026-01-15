package com.px.ifp.spc.configure;


import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SpcKafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spc.kafka.consumer.group-id}")
    private String groupId;

    // 可通过配置文件调整，提供默认值
    @Value("${spc.kafka.consumer.concurrency:3}")
    private int concurrency;

    @Value("${spc.kafka.consumer.max-poll-records:100}")
    private int maxPollRecords;

    @Value("${spc.kafka.consumer.fetch-max-wait-ms:200}")
    private int fetchMaxWaitMs;

    @Value("${spc.kafka.consumer.auto-offset-reset:latest}")
    private String autoOffsetReset;

    /**
     * SPC专属ConsumerFactory
     * 独立的消费者工厂，配置仅对SPC消费者生效
     */
    @Bean
    public ConsumerFactory<String, String> spcConsumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // 基础配置
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // SPC优化配置
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, fetchMaxWaitMs);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);

        // 增加超时时间，避免频繁rebalance
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);  // 30秒
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);  // 10秒

        // 设置最大拉取大小（可选）
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1048576);  // 1MB

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * SPC专属KafkaListenerContainerFactory
     *
     * 在@KafkaListener中通过 containerFactory = "spcKafkaListenerContainerFactory"
     * 来指定使用此工厂
     */
    @Bean("spcKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String>
    spcKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(spcConsumerFactory());

        // 并发数 = 消费线程数，理想值等于Kafka分区数
        factory.setConcurrency(concurrency);

        // 手动提交模式（与现有代码保持一致）
        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // 设置批量监听器（可选，如果需要批量处理）
        // factory.setBatchListener(true);

        return factory;
    }
}
