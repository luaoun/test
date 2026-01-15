package com.px.ifp.spc.service.cache;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;


/**
 * SPC配置刷新监听器 - Redis Pub/Sub
 *
 * 监听 Redis 频道：spc:config:refresh
 * 当收到配置更新通知时，刷新本地缓存
 */
@Slf4j
@Component
public class SpcConfigRefreshListener implements MessageListener {

    /**
     * Redis Pub/Sub 频道名称
     */
    public static final String CHANNEL_NAME = "spc:config:refresh";

    @Autowired
    private LocalSpcIndicatorCache localCache;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            log.info("收到Redis配置刷新通知: channel={}, body={}", channel, body);

            // 校验频道名称
            if (!CHANNEL_NAME.equals(channel)) {
                log.warn("收到未知频道的消息: {}", channel);
                return;
            }

            // 解析消息体
            RefreshMessage refreshMessage = parseMessage(body);

            if (refreshMessage == null) {
                log.warn("无法解析刷新消息，执行全量刷新: body={}", body);
                localCache.refreshAll();
                return;
            }

            // 根据刷新类型执行不同操作
            switch (refreshMessage.getType()) {
                case "ALL":
                    // 全量刷新
                    log.info("执行全量缓存刷新");
                    localCache.refreshAll();
                    break;

                case "SINGLE":
                    // 单点刷新
                    String facCode = refreshMessage.getFacCode();
                    String point = refreshMessage.getPoint();
                    if (point != null) {
                        log.info("执行单点缓存刷新: point={}", point);
                        localCache.refresh(facCode,point);
                    } else {
                        log.warn("单点刷新消息缺少point字段: {}", body);
                    }
                    break;

                case "INVALIDATE":
                    // 清除缓存
                    String invalidateFacCode = refreshMessage.getFacCode();
                    String invalidatePoint = refreshMessage.getPoint();
                    if (invalidatePoint != null) {
                        log.info("执行单点缓存清除: point={}", invalidatePoint);
                        localCache.invalidate(invalidateFacCode,invalidatePoint);
                    } else {
                        log.info("执行全量缓存清除");
                        localCache.invalidateAll();
                    }
                    break;

                default:
                    log.warn("未知的刷新类型: {}, 执行全量刷新", refreshMessage.getType());
                    localCache.refreshAll();
            }

            log.info("配置刷新完成: type={}, point={}, 当前缓存大小={}",
                    refreshMessage.getType(), refreshMessage.getPoint(), localCache.size());

        } catch (Exception e) {
            log.error("处理配置刷新消息失败", e);
            // 异常时尝试全量刷新作为兜底
            try {
                localCache.refreshAll();
            } catch (Exception fallbackException) {
                log.error("兜底全量刷新也失败", fallbackException);
            }
        }
    }

    /**
     * 解析刷新消息
     */
    private RefreshMessage parseMessage(String body) {
        try {
            // 尝试解析为 JSON
            return objectMapper.readValue(body, RefreshMessage.class);
        } catch (Exception e) {
            // 如果不是 JSON，尝试解析为简单文本
            if ("refresh".equalsIgnoreCase(body) || "ALL".equalsIgnoreCase(body)) {
                return new RefreshMessage("ALL", null, null);
            }
            log.debug("无法解析消息体: {}", body, e);
            return null;
        }
    }

    /**
     * 刷新消息对象
     */
    public static class RefreshMessage {
        /**
         * 刷新类型：ALL-全量刷新, SINGLE-单点刷新, INVALIDATE-清除缓存
         */
        private String type;

        /**
         * 工厂代码（单点刷新时使用）
         */
        private String facCode;

        /**
         * 点位（单点刷新时使用）
         */
        private String point;

        /**
         * 版本号（可选，用于检测配置版本）
         */
        private Long version;

        public RefreshMessage() {
        }

        public RefreshMessage(String type, String facCode, String point) {
            this.type = type;
            this.facCode = facCode;
            this.point = point;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getFacCode() {
            return facCode;
        }

        public void setFacCode(String facCode) {
            this.facCode = facCode;
        }

        public String getPoint() {
            return point;
        }

        public void setPoint(String point) {
            this.point = point;
        }

        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }
    }
}
