package com.px.ifp.spc.service.filter;


import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 布隆过滤器幂等性服务
 */
@Slf4j
@Service
public class BloomFilterIdempotencyService {

    /**
     * 布隆过滤器预期插入数量（默认 100 万条消息）
     */
    @Value("${spc.idempotency.bloom-filter.expected-insertions:1000000}")
    private int expectedInsertions;

    /**
     * 布隆过滤器误判率（默认 1%）
     */
    @Value("${spc.idempotency.bloom-filter.false-positive-probability:0.01}")
    private double falsePositiveProbability;

    /**
     * 布隆过滤器重置阈值（默认达到预期容量的 90% 时重置）
     */
    @Value("${spc.idempotency.bloom-filter.reset-threshold:0.9}")
    private double resetThreshold;

    /**
     * 布隆过滤器实例（线程安全）
     */
    private volatile BloomFilter<CharSequence> bloomFilter;

    /**
     * 当前已插入元素计数器
     */
    private final AtomicLong insertCount = new AtomicLong(0);

    /**
     * 拦截统计：布隆过滤器命中次数
     */
    private final AtomicLong bloomHitCount = new AtomicLong(0);

    /**
     * 拦截统计：Redis 检查次数
     */
    private final AtomicLong redisCheckCount = new AtomicLong(0);

    @PostConstruct
    public void init() {
        bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                falsePositiveProbability
        );
        log.info("布隆过滤器初始化完成: expectedInsertions={}, falsePositiveProbability={}, resetThreshold={}",
                expectedInsertions, falsePositiveProbability, resetThreshold);
    }

    /**
     * 快速检查消息是否可能已处理（布隆过滤器预检查）
     *
     * @param uniqueKey 唯一标识
     * @return true=可能已处理（需Redis二次确认），false=肯定未处理
     */
    public boolean mightContain(String uniqueKey) {
        boolean result = bloomFilter.mightContain(uniqueKey);
        if (result) {
            bloomHitCount.incrementAndGet();
        }
        return result;
    }

    /**
     * 标记消息已处理（添加到布隆过滤器）
     *
     * @param uniqueKey 唯一标识
     */
    public void markProcessed(String uniqueKey) {
        bloomFilter.put(uniqueKey);
        long count = insertCount.incrementAndGet();

        // 检查是否需要重置布隆过滤器
        if (count >= expectedInsertions * resetThreshold) {
            resetBloomFilter();
        }
    }

    /**
     * 增加 Redis 检查计数（用于统计）
     */
    public void incrementRedisCheckCount() {
        redisCheckCount.incrementAndGet();
    }

    /**
     * 重置布隆过滤器（防止过载导致误判率上升）
     */
    private synchronized void resetBloomFilter() {
        long currentCount = insertCount.get();
        if (currentCount >= expectedInsertions * resetThreshold) {
            log.warn("布隆过滤器达到容量阈值，执行重置: insertCount={}, threshold={}",
                    currentCount, expectedInsertions * resetThreshold);

            // 创建新的布隆过滤器
            BloomFilter<CharSequence> newBloomFilter = BloomFilter.create(
                    Funnels.stringFunnel(StandardCharsets.UTF_8),
                    expectedInsertions,
                    falsePositiveProbability
            );

            // 原子替换
            bloomFilter = newBloomFilter;
            insertCount.set(0);

            log.info("布隆过滤器重置完成，统计信息 - 布隆命中: {}, Redis检查: {}",
                    bloomHitCount.get(), redisCheckCount.get());

            // 重置统计计数器
            bloomHitCount.set(0);
            redisCheckCount.set(0);
        }
    }

    /**
     * 获取布隆过滤器统计信息
     */
    public String getStatistics() {
        return String.format(
                "布隆过滤器统计 - 插入数: %d, 布隆命中: %d, Redis检查: %d, 拦截率: %.2f%%",
                insertCount.get(),
                bloomHitCount.get(),
                redisCheckCount.get(),
                bloomHitCount.get() > 0 ?
                        (bloomHitCount.get() * 100.0 / (bloomHitCount.get() + redisCheckCount.get())) : 0
        );
    }

    /**
     * 手动重置（用于测试或维护）
     */
    public void manualReset() {
        log.info("执行手动重置布隆过滤器");
        insertCount.set(expectedInsertions); // 触发重置
        resetBloomFilter();
    }
}
