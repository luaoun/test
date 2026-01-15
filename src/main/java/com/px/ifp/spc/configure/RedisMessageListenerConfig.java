package com.px.ifp.spc.configure;

import com.px.ifp.spc.service.cache.SpcConfigRefreshListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Redis 消息监听器配置
 *
 * 配置 Redis Pub/Sub 监听器，用于实现多实例配置缓存一致性
 */
@Slf4j
@Configuration
public class RedisMessageListenerConfig {

    @Autowired
    private SpcConfigRefreshListener spcConfigRefreshListener;

    /**
     * Redis 消息监听容器
     *
     * 负责管理 Redis Pub/Sub 的监听器生命周期
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 注册 SPC 配置刷新监听器
        container.addMessageListener(
                new MessageListenerAdapter(spcConfigRefreshListener),
                new ChannelTopic(SpcConfigRefreshListener.CHANNEL_NAME)
        );

        log.info("Redis Pub/Sub 监听器已注册: channel={}", SpcConfigRefreshListener.CHANNEL_NAME);

        return container;
    }
}
