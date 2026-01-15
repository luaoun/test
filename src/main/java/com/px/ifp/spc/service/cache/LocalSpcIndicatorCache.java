package com.px.ifp.spc.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.px.ifp.spc.entity.SpcPointMetadataDO;
import com.px.ifp.spc.service.indicator.SpcPointMetaDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * SPC指标本地缓存管理器
 *
 * 使用 Caffeine 提供高性能的本地缓存，减少 Redis 查询压力
 * 配合 Redis Pub/Sub 实现多实例缓存一致性
 */
@Slf4j
@Component
public class LocalSpcIndicatorCache {

    /**
     * 本地缓存：Key=point, Value=SpcPointMetadataDO
     */
    private final Cache<String, SpcPointMetadataDO> pointCache;

    /**
     * 缓存版本号（用于检测配置更新）
     */
    private final AtomicLong cacheVersion = new AtomicLong(0);

    @Autowired
    private SpcPointMetaDataService indicatorService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public LocalSpcIndicatorCache() {
        this.pointCache = Caffeine.newBuilder()
                .maximumSize(100_000) // 最多缓存10万个点位
                .expireAfterWrite(24, TimeUnit.HOURS) // 24小时过期（兜底机制）
                .recordStats() // 启用统计功能，用于监控缓存命中率
                .build();
    }

    /**
     * 应用启动时加载全量配置
     */
    @PostConstruct
    public void init() {
        log.info("开始初始化SPC指标本地缓存...");
        refreshAll();
        log.info("SPC指标本地缓存初始化完成，缓存点位数: {}, 版本号: {}",
                pointCache.estimatedSize(), cacheVersion.get());
    }

    /**
     * 获取指标配置（快速查询，纳秒级）
     *
     * @param facCode 工厂代码
     * @param point 点位
     * @return 指标配置，如果不存在返回 null
     */
    public SpcPointMetadataDO get(String facCode, String point) {
        String cacheKey = buildCacheKey(facCode, point);
        return pointCache.getIfPresent(cacheKey);
    }

    /**
     * 构建缓存 Key：facCode:point
     *
     * @param facCode 工厂代码
     * @param point 点位
     * @return 缓存 key
     */
    private String buildCacheKey(String facCode, String point) {
        if (facCode == null || facCode.isEmpty()) {
            return point;
        }
        return facCode + ":" + point;
    }

    /**
     * 刷新全量配置（从 Redis 加载）
     * 由 Redis Pub/Sub 监听器调用
     */
    public void refreshAll() {
        try {
            long startTime = System.currentTimeMillis();

            // 从 Redis 加载全量配置（Redis已缓存数据库数据）
            List<SpcPointMetadataDO> allIndicators = loadAllIndicatorsFromRedis();

            // 清空旧缓存
            pointCache.invalidateAll();

            // 构建新缓存（facCode:point -> indicator 映射）
            Map<String, SpcPointMetadataDO> newCache = allIndicators.stream()
                    .filter(ind -> ind.getMeasureCode() != null) // 过滤无效数据
                    .collect(Collectors.toMap(
                            ind -> buildCacheKey(ind.getFacCode(), ind.getMeasureCode()),
                            ind -> ind,
                            (existing, replacement) -> replacement // 如果有重复，使用新的
                    ));

            // 批量写入缓存
            pointCache.putAll(newCache);

            // 更新版本号
            long newVersion = cacheVersion.incrementAndGet();

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("本地缓存刷新完成: 点位数={}, 版本号={}, 耗时={}ms",
                    newCache.size(), newVersion, elapsedTime);

        } catch (Exception e) {
            log.error("刷新本地缓存失败", e);
        }
    }

    /**
     * 从 Redis 加载全量指标配置
     */
    private List<SpcPointMetadataDO> loadAllIndicatorsFromRedis() {
        try {
            // 1. 获取所有 SPC 指标配置的 Redis key
            Set<String> keys_fac_83abf9 = redisTemplate.keys("FAC_83abf9:spc:indicator:*");
            Set<String> keys_fac_93abf6 = redisTemplate.keys("FAC_93abf6:spc:indicator:*");
            List<String> keys = new ArrayList<>();
            keys.addAll(keys_fac_83abf9);
            keys.addAll(keys_fac_93abf6);

            if (keys == null || keys.isEmpty()) {
                log.warn("Redis 中没有 SPC 指标配置缓存，降级到数据库查询");
                return loadAllIndicatorsFromDatabase();
            }

            log.debug("从 Redis 加载 {} 个FAC_83abf9点位的配置", keys_fac_83abf9.size());


            // 2. 批量读取所有 key 的值
            List<SpcPointMetadataDO> allIndicators = new ArrayList<>();

            for (String key : keys) {
                try {
                    Object cached = redisTemplate.opsForValue().get(key);

                    if (cached == null) {
                        continue;
                    }

                    // Redis 中存储的是 List<SpcPointMetadataDO>（一个点位可能有多个指标）
                    if (cached instanceof List) {
                        List<SpcPointMetadataDO> indicators = (List<SpcPointMetadataDO>) cached;
                        allIndicators.addAll(indicators);
                    } else if (cached instanceof SpcPointMetadataDO) {
                        // 兼容单个对象的情况
                        allIndicators.add((SpcPointMetadataDO) cached);
                    }

                } catch (Exception e) {
                    log.warn("读取 Redis key 失败: {}", key, e);
                    // 单个 key 失败不影响其他 key 的读取
                }
            }

            if (allIndicators.isEmpty()) {
                log.warn("从 Redis 读取到的配置为空，降级到数据库查询");
                return loadAllIndicatorsFromDatabase();
            }

            log.info("从 Redis 成功加载 {} 条 SPC 指标配置（覆盖 {} 个点位）",
                    allIndicators.size(), keys.size());

            return allIndicators;

        } catch (Exception e) {
            log.error("从 Redis 加载配置失败，降级到数据库查询", e);
            return loadAllIndicatorsFromDatabase();
        }
    }

    /**
     * 从数据库加载全量指标配置（降级方案）
     *
     * 当 Redis 不可用或为空时使用
     */
    private List<SpcPointMetadataDO> loadAllIndicatorsFromDatabase() {
        try {
            log.info("从数据库加载全量 SPC 指标配置");
            List<SpcPointMetadataDO> indicators = indicatorService.getAllActiveIndicators();

            if (indicators == null || indicators.isEmpty()) {
                log.warn("数据库中没有有效的 SPC 指标配置");
                return Collections.emptyList();
            }

            log.info("从数据库成功加载 {} 条 SPC 指标配置", indicators.size());
            return indicators;

        } catch (Exception e) {
            log.error("从数据库加载配置失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 刷新单个点位配置
     *
     * @param facCode 工厂代码
     * @param point 点位
     */
    public void refresh(String facCode, String point) {
        try {
            String cacheKey = buildCacheKey(facCode, point);
            List<SpcPointMetadataDO> indicators = indicatorService.getIndicatorsByPoint(facCode,point);

            if (indicators == null || indicators.isEmpty()) {
                // 点位未配置，移除缓存
                pointCache.invalidate(cacheKey);
                log.debug("移除本地缓存: facCode={}, point={}", facCode, point);
            } else {
                // 更新缓存（一个 facCode:point 只有一个指标配置）
                SpcPointMetadataDO indicator = indicators.stream()
                        .filter(ind -> facCode == null || facCode.equals(ind.getFacCode()))
                        .findFirst()
                        .orElse(indicators.get(0));

                pointCache.put(cacheKey, indicator);
                log.debug("更新本地缓存: facCode={}, point={}, indicatorId={}",
                        facCode, point, indicator.getId());
            }

        } catch (Exception e) {
            log.error("刷新单点配置失败: facCode={}, point={}", facCode, point, e);
        }
    }

    /**
     * 移除指定点位的缓存
     *
     * @param facCode 工厂代码
     * @param point 点位
     */
    public void invalidate(String facCode, String point) {
        String cacheKey = buildCacheKey(facCode, point);
        pointCache.invalidate(cacheKey);
        log.debug("清除本地缓存: facCode={}, point={}", facCode, point);
    }

    /**
     * 清空所有缓存
     */
    public void invalidateAll() {
        pointCache.invalidateAll();
        log.info("清空所有本地缓存");
    }

    /**
     * 获取当前缓存版本号
     */
    public long getCacheVersion() {
        return cacheVersion.get();
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        CacheStats stats = pointCache.stats();
        return String.format(
                "缓存统计: 大小=%d, 命中率=%.2f%%, 命中次数=%d, 未命中次数=%d, 加载成功=%d, 加载失败=%d",
                pointCache.estimatedSize(),
                stats.hitRate() * 100,
                stats.hitCount(),
                stats.missCount(),
                stats.loadSuccessCount(),
                stats.loadFailureCount()
        );
    }

    /**
     * 获取缓存大小
     */
    public long size() {
        return pointCache.estimatedSize();
    }

    /**
     * 判断点位是否已配置SPC指标
     *
     * @param facCode 工厂代码
     * @param point 点位
     * @return true=已配置, false=未配置
     */
    public boolean isPointConfigured(String facCode, String point) {
        return get(facCode, point) != null;
    }
}