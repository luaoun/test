package com.px.ifp.spc.service.helper;


import com.px.ifp.common.constant.CommonConstants;
import com.px.ifp.common.utils.ThreadLocalUtils;
import com.px.ifp.spc.service.filter.BloomFilterIdempotencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.stream.Collectors;

/**
 * 幂等性检查服务 - 防止重复处理消息
 */
@Slf4j
@Service
public class IdempotencyService {

    private static final String IDEMPOTENCY_KEY_PREFIX = ":spc:idempotency:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10); // 默认10分钟过期

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private BloomFilterIdempotencyService bloomFilterService;

    /**
     * 检查并标记消息是否已处理（原子操作）
     * @param uniqueKey 唯一标识
     * @param ttl 过期时间
     * @return true=首次处理，false=重复消息
     */
    public boolean checkAndMarkProcessed(String uniqueKey, Duration ttl) {

        // 性能优化1：布隆过滤器快速预检查（纳秒级）
        // 如果布隆过滤器判定消息可能已处理，则执行 Redis 二次确认
        // 如果布隆过滤器判定消息肯定未处理，则直接跳过 Redis 检查（减少 Redis 调用）
        if (bloomFilterService.mightContain(uniqueKey)) {
            // 可能已处理，需要 Redis 权威检查
            bloomFilterService.incrementRedisCheckCount();
        } else {
            // 肯定未处理，直接标记为新消息
            bloomFilterService.markProcessed(uniqueKey);

            // 仍需在 Redis 中标记（用于分布式一致性）
            String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);

            String redisKey = facCode+IDEMPOTENCY_KEY_PREFIX + uniqueKey;
            try {
                redisTemplate.opsForValue().set(
                        redisKey,
                        System.currentTimeMillis(),
                        ttl != null ? ttl : DEFAULT_TTL
                );
                return true;
            } catch (Exception e) {
                log.error("Redis标记失败，但允许处理（布隆过滤器已确认为新消息）: {}", uniqueKey, e);
                return true;
            }
        }

        // ⚡ 性能优化2：Redis 权威检查（仅对布隆过滤器命中的消息执行）
        String redisKey = IDEMPOTENCY_KEY_PREFIX + uniqueKey;

        try {
            // 使用 setIfAbsent 实现原子性的检查和设置
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                    redisKey,
                    System.currentTimeMillis(),
                    ttl != null ? ttl : DEFAULT_TTL
            );

            boolean isFirstTime = Boolean.TRUE.equals(success);

            if (isFirstTime) {
                // Redis 确认为首次处理，添加到布隆过滤器
                bloomFilterService.markProcessed(uniqueKey);
            }

            return isFirstTime;

        } catch (Exception e) {
            log.error("幂等性检查失败，允许处理以避免丢失数据: {}", uniqueKey, e);
            // 出现异常时，为了避免丢失数据，允许处理
            return true;
        }
    }

    /**
     * 检查并标记消息是否已处理（使用默认TTL）
     */
    public boolean checkAndMarkProcessed(String uniqueKey) {
        return checkAndMarkProcessed(uniqueKey, DEFAULT_TTL);
    }

    /**
     * 检查并标记消息是否已处理（使用分钟作为单位）
     * @param uniqueKey 唯一标识
     * @param ttlMinutes 过期时间（分钟）
     * @return true=首次处理，false=重复消息
     */
    public boolean checkAndMarkProcessed(String uniqueKey, int ttlMinutes) {
        return checkAndMarkProcessed(uniqueKey, Duration.ofMinutes(ttlMinutes));
    }

    /**
     * 检查消息是否已处理（不标记）
     */
    public boolean isProcessed(String uniqueKey) {
        String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);

        String redisKey = facCode + IDEMPOTENCY_KEY_PREFIX + uniqueKey;
        try {
            return redisTemplate.hasKey(redisKey);
        } catch (Exception e) {
            log.error("检查幂等性失败: {}", uniqueKey, e);
            return false;
        }
    }

    /**
     * 手动标记消息已处理
     */
    public void markProcessed(String uniqueKey, Duration ttl) {

        String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);

        String redisKey = facCode + IDEMPOTENCY_KEY_PREFIX + uniqueKey;
        try {
            redisTemplate.opsForValue().set(
                    redisKey,
                    System.currentTimeMillis(),
                    ttl != null ? ttl : DEFAULT_TTL
            );
        } catch (Exception e) {
            log.error("标记消息已处理失败: {}", uniqueKey, e);
        }
    }

    /**
     * 删除幂等性标记（用于异常回滚）
     */
    public void removeProcessedMark(String uniqueKey) {

        String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);

        String redisKey = facCode + IDEMPOTENCY_KEY_PREFIX + uniqueKey;

        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("删除幂等性标记失败: {}", uniqueKey, e);
        }
    }

    /**
     * 批量清理过期的幂等性标记
     */
    public void cleanupExpiredKeys(int batchSize) {
        try {
            // Redis会自动清理过期键，这里主要用于统计和监控
            String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);

            String pattern = facCode + IDEMPOTENCY_KEY_PREFIX + "*";
            Long count = redisTemplate.countExistingKeys(
                    redisTemplate.keys(pattern).stream().limit(batchSize).collect(Collectors.toList())
            );
        } catch (Exception e) {
            log.warn("清理幂等性缓存统计失败", e);
        }
    }
}
