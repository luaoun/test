package com.px.ifp.spc.service.impl;

import com.px.ifp.spc.bo.AlarmSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报警通知服务
 */
@Slf4j
@Service
public class AlarmNotificationService {
    
    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;
    
    // 通知渠道枚举
    public enum NotificationChannel {
        WEB_SOCKET,    // WebSocket推送
        EMAIL,         // 邮件通知  
        SMS,           // 短信通知
        DING_TALK,     // 钉钉通知
        WECHAT_WORK    // 企微通知
    }
    
    /**
     * 触发报警通知
     */
    public void triggerAlarmNotification(AlarmSegment segment, String eventType) {
        /**
        try {
            // 构建通知消息
            AlarmNotification notification = buildNotification(segment, eventType);

            // 根据严重程度选择通知渠道
            List<NotificationChannel> channels = determineChannels(segment.getAlarmType());

            // 异步发送通知
            for (NotificationChannel channel : channels) {
                CompletableFuture.runAsync(() -> {
                    sendNotification(notification, channel);
                });
            }

            // 记录通知历史
            saveNotificationHistory(notification);

            log.info("触发报警通知: 事件类型={}, 报警段={}, 通知渠道={}",
                eventType, segment.getSegmentId(), channels);

        } catch (Exception e) {
            log.error("触发报警通知失败: 事件类型={}, 报警段={}",
                eventType, segment.getSegmentId(), e);
        }
         **/
    }
    
    /**
     * 构建通知消息
     */
    private AlarmNotification buildNotification(AlarmSegment segment, String eventType) {
        return AlarmNotification.builder()
            .segmentId(segment.getSegmentId())
            .title(buildNotificationTitle(segment, eventType))
            .content(buildNotificationContent(segment, eventType))
            .urgency(determineUrgency(segment.getAlarmType()))
            .eventType(eventType)
            .indicatorName(segment.getIndicatorName())
            .point(segment.getPoint())
            .alarmType(segment.getAlarmType())
            .currentValue(segment.getCurrentValue())
            .startTime(segment.getStartTime())
            .pointCount(segment.getPointCount())
            .timestamp(java.time.Instant.now())
            .build();
    }
    
    /**
     * 构建通知标题
     */
    private String buildNotificationTitle(AlarmSegment segment, String eventType) {
        switch (eventType) {
            case "ALARM_START":
                return String.format("🚨 SPC报警 - %s [%s]", 
                    segment.getIndicatorName(), segment.getAlarmType());
            case "ALARM_ESCALATE":
                return String.format("⚠️ SPC报警升级 - %s [%s]", 
                    segment.getIndicatorName(), segment.getAlarmType());
            case "ALARM_RECOVERY":
                return String.format("✅ SPC报警恢复 - %s", segment.getIndicatorName());
            default:
                return "SPC系统通知";
        }
    }
    
    /**
     * 构建通知内容
     */
    private String buildNotificationContent(AlarmSegment segment, String eventType) {
        StringBuilder content = new StringBuilder();
        
        content.append("指标名称: ").append(segment.getIndicatorName()).append("\n");
        content.append("点位: ").append(segment.getPoint()).append("\n");
        content.append("报警类型: ").append(getAlarmTypeDescription(segment.getAlarmType())).append("\n");
        
        if (segment.getCurrentValue() != null) {
            content.append("当前值: ").append(segment.getCurrentValue()).append("\n");
        }
        
        if ("ALARM_RECOVERY".equals(eventType)) {
            content.append("持续时长: ").append(segment.getDurationSeconds()).append("秒\n");
            content.append("异常点数: ").append(segment.getPointCount()).append("个\n");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            content.append("开始时间: ").append(sdf.format(segment.getStartTime())).append("\n");
        }
        
        content.append("事件ID: ").append(segment.getSegmentId());
        
        return content.toString();
    }
    
    /**
     * 获取报警类型描述
     */
    private String getAlarmTypeDescription(String alarmType) {
        switch (alarmType) {
            case "OOC": return "超出控制线";
            case "OOS": return "超出规格线";
            case "OOW": return "超出警告线";
            case "OO3": return "超出3σ线";
            default: return alarmType;
        }
    }
    
    /**
     * 确定紧急程度
     */
    private String determineUrgency(String alarmType) {
        switch (alarmType) {
            case "OOC":
            case "OOS":
                return "HIGH";
            case "OOW":
                return "MEDIUM";
            case "OO3":
                return "LOW";
            default:
                return "LOW";
        }
    }
    
    /**
     * 根据严重程度确定通知渠道
     */
    private List<NotificationChannel> determineChannels(String alarmType) {
        List<NotificationChannel> channels = new ArrayList<>();
        
        // 所有报警都推送到WebSocket
        channels.add(NotificationChannel.WEB_SOCKET);
        
        // 根据严重程度添加其他渠道
        switch (alarmType) {
            case "OOC":
            case "OOS":
                // 高严重性：邮件 + 短信 + 即时通讯
                channels.add(NotificationChannel.EMAIL);
                channels.add(NotificationChannel.SMS);
                channels.add(NotificationChannel.DING_TALK);
                break;
            case "OOW":
                // 中等严重性：邮件 + 即时通讯
                channels.add(NotificationChannel.EMAIL);
                channels.add(NotificationChannel.DING_TALK);
                break;
            case "OO3":
                // 低严重性：只发邮件
                channels.add(NotificationChannel.EMAIL);
                break;
        }
        
        return channels;
    }
    
    /**
     * 发送通知
     */
    private void sendNotification(AlarmNotification notification, NotificationChannel channel) {
        try {
            switch (channel) {
                case WEB_SOCKET:
                    sendWebSocketNotification(notification);
                    break;
                case EMAIL:
                    sendEmailNotification(notification);
                    break;
                case SMS:
                    sendSmsNotification(notification);
                    break;
                case DING_TALK:
                    sendDingTalkNotification(notification);
                    break;
                case WECHAT_WORK:
                    sendWeChatWorkNotification(notification);
                    break;
            }
            
            log.debug("发送通知成功: 渠道={}, 事件={}", channel, notification.getSegmentId());
            
        } catch (Exception e) {
            log.error("发送通知失败: 渠道={}, 事件={}", channel, notification.getSegmentId(), e);
        }
    }
    
    /**
     * 发送WebSocket通知
     */
    private void sendWebSocketNotification(AlarmNotification notification) {
        if (messagingTemplate == null) {
            log.warn("WebSocket消息模板未配置，跳过WebSocket通知");
            return;
        }
        
        try {
            // 构建WebSocket消息
            Map<String, Object> message = new HashMap<>();
            message.put("type", "spc_alarm");
            message.put("eventType", notification.getEventType());
            message.put("segmentId", notification.getSegmentId());
            message.put("title", notification.getTitle());
            message.put("content", notification.getContent());
            message.put("urgency", notification.getUrgency());
            message.put("indicatorName", notification.getIndicatorName());
            message.put("point", notification.getPoint());
            message.put("alarmType", notification.getAlarmType());
            message.put("currentValue", notification.getCurrentValue());
            message.put("timestamp", notification.getTimestamp());
            
            // 发送到全局报警主题
            messagingTemplate.convertAndSend("/topic/spc/alarms", message);
            
            // 发送到指定指标主题（如果前端订阅了特定指标）
            if (notification.getPoint() != null) {
                messagingTemplate.convertAndSend(
                    "/topic/spc/alarms/" + notification.getPoint(), message);
            }
            
        } catch (Exception e) {
            log.error("发送WebSocket通知失败", e);
        }
    }
    
    /**
     * 发送邮件通知（占位符实现）
     */
    private void sendEmailNotification(AlarmNotification notification) {
        // TODO: 实现邮件通知逻辑
        log.info("发送邮件通知: {}", notification.getTitle());
    }
    
    /**
     * 发送短信通知（占位符实现）
     */
    private void sendSmsNotification(AlarmNotification notification) {
        // TODO: 实现短信通知逻辑
        log.info("发送短信通知: {}", notification.getTitle());
    }
    
    /**
     * 发送钉钉通知（占位符实现）
     */
    private void sendDingTalkNotification(AlarmNotification notification) {
        // TODO: 实现钉钉通知逻辑
        log.info("发送钉钉通知: {}", notification.getTitle());
    }
    
    /**
     * 发送企业微信通知（占位符实现）
     */
    private void sendWeChatWorkNotification(AlarmNotification notification) {
        // TODO: 实现企业微信通知逻辑
        log.info("发送企业微信通知: {}", notification.getTitle());
    }
    
    /**
     * 保存通知历史（占位符实现）
     */
    private void saveNotificationHistory(AlarmNotification notification) {
        // TODO: 实现通知历史保存逻辑
        log.debug("保存通知历史: {}", notification.getSegmentId());
    }
    
    /**
     * 通知消息内部类
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AlarmNotification {
        private String segmentId;
        private String title;
        private String content;
        private String urgency;
        private String eventType;
        private String indicatorName;
        private String point;
        private String alarmType;
        private java.math.BigDecimal currentValue;
        private java.util.Date startTime;
        private Integer pointCount;
        private java.time.Instant timestamp;
    }
}
