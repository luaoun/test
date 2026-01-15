package com.px.ifp.spc.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 监控数据模型 - 用于接收Kafka消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorData {
    
    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 点位编码
     */
    private String point;
    
    /**
     * 点位值
     */
    private BigDecimal value;
    
    /**
     * 数据时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date ctime;
    
    /**
     * 数据质量
     */
    private String quality;
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * 系统代码
     */
    private String systemCode;
    
    /**
     * 科室代码
     */
    private String classCode;
    
    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 厂区
     */
    private String facCode;
    
    /**
     * 判断数据质量是否良好
     */
    public boolean isGoodQuality() {
        return "GOOD".equals(quality);
    }

    /**
     * 生成唯一标识用于幂等性检查
     * 基于: messageId + point + value + ctime(精确到秒)
     */
    public String generateUniqueKey() {
        StringBuilder sb = new StringBuilder();

        // 使用messageId（如果存在）
        if (messageId != null && !messageId.isEmpty()) {
            sb.append(messageId);
        } else {
            // 如果没有messageId，使用point+value组合
            sb.append("AUTO_");
            if (point != null) sb.append(point);
            sb.append("_");
            if (value != null) sb.append(value);
        }

        // 添加时间戳（精确到秒，避免毫秒级重复）
        if (ctime != null) {
            sb.append("_").append(ctime.getTime() / 1000);
        }

        return sb.toString();
    }

    /**
     * 生成处理会话唯一标识（用于整个处理流程的幂等性）
     * 基于: point + ctime(精确到毫秒) + value
     */
    public String generateProcessingSessionKey() {
        StringBuilder sb = new StringBuilder("session_");

        if (point != null) {
            sb.append(point).append("_");
        }

        if (ctime != null) {
            sb.append(ctime.getTime()).append("_");
        }

        if (value != null) {
            sb.append(value);
        }

        return sb.toString();
    }
}
