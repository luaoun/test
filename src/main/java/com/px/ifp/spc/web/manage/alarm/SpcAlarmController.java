package com.px.ifp.spc.web.manage.alarm;

import com.px.ifp.spc.entity.SpcAlarmEvent;
import com.px.ifp.spc.entity.SpcAnalysisResult;
import com.px.ifp.spc.service.alarm.SpcAlarmEventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * SPC报警控制器
 */
@Slf4j
@Tag(name = "SPC告警相关接口(V2)")
@RestController
@RequestMapping("/api/v2/alarm")
public class SpcAlarmController {
    
    @Autowired
    private SpcAlarmEventService alarmEventService;
    
    /**
     * 查询活跃的报警事件
     */
    @GetMapping("/active")
    public ResponseEntity<List<SpcAlarmEvent>> getActiveAlarmEvents(
            @RequestParam(required = false) List<Long> indicatorIds) {
        List<SpcAlarmEvent> activeEvents = alarmEventService.getActiveAlarmEvents(indicatorIds);
        return ResponseEntity.ok(activeEvents);
    }
    
    /**
     * 查询报警事件明细
     */
    @GetMapping("/event/{segmentId}/details")
    public ResponseEntity<List<SpcAnalysisResult>> getAlarmEventDetails(
            @PathVariable String segmentId) {
        List<SpcAnalysisResult> details = alarmEventService.getAlarmEventDetails(segmentId);
        return ResponseEntity.ok(details);
    }
    
    /**
     * 查询报警事件的异常点
     */
    @GetMapping("/event/{segmentId}/abnormal-points")
    public ResponseEntity<List<SpcAnalysisResult>> getAlarmEventAbnormalPoints(
            @PathVariable String segmentId) {
        List<SpcAnalysisResult> abnormalPoints = alarmEventService.getAlarmEventAbnormalPoints(segmentId);
        return ResponseEntity.ok(abnormalPoints);
    }
    
    /**
     * 查询指定时间范围内的报警事件
     */
    @GetMapping("/events")
    public ResponseEntity<List<SpcAlarmEvent>> getAlarmEventsByTimeRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String classCode,
            @RequestParam(required = false) String facCode) {
        
        List<SpcAlarmEvent> events = alarmEventService.getAlarmEventsByTimeRange(
            startTime, endTime, systemCode, classCode,facCode);
        return ResponseEntity.ok(events);
    }
    
    /**
     * 根据指标ID查询最近的报警事件
     */
    @GetMapping("/indicator/{indicatorId}/recent")
    public ResponseEntity<List<SpcAlarmEvent>> getRecentAlarmEventsByIndicatorId(
            @PathVariable Long indicatorId,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String facCode
        ) {
        
        List<SpcAlarmEvent> events = alarmEventService.getRecentAlarmEventsByIndicatorId(indicatorId, limit,facCode);
        return ResponseEntity.ok(events);
    }
    
    /**
     * 统计指定时间范围内的报警事件数量
     */
    @GetMapping("/statistics/count")
    public ResponseEntity<AlarmStatisticsVO> getAlarmStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime,
            @RequestParam(required = false) String alarmType,
            @RequestParam(required = false) String facCode) {
        
        Long totalCount = alarmEventService.countAlarmEventsByTimeRange(startTime, endTime, null,facCode);
        Long specificCount = alarmType != null ? 
            alarmEventService.countAlarmEventsByTimeRange(startTime, endTime, alarmType,facCode) : totalCount;
        
        AlarmStatisticsVO statistics = AlarmStatisticsVO.builder()
            .totalCount(totalCount)
            .specificTypeCount(specificCount)
            .alarmType(alarmType)
            .startTime(startTime)
            .endTime(endTime)
            .build();
            
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 手动处理报警事件
     */
    @PostMapping("/event/{segmentId}/handle")
    public ResponseEntity<String> handleAlarmEvent(
            @PathVariable String segmentId,
            @RequestBody HandleAlarmRequest request) {
        
        // TODO: 实现报警事件处理逻辑
        log.info("手动处理报警事件: segmentId={}, handledBy={}, note={}", 
            segmentId, request.getHandledBy(), request.getHandleNote());
            
        return ResponseEntity.ok("报警事件处理成功");
    }
    
    /**
     * 忽略报警事件
     */
    @PostMapping("/event/{segmentId}/ignore")
    public ResponseEntity<String> ignoreAlarmEvent(
            @PathVariable String segmentId,
            @RequestBody IgnoreAlarmRequest request) {
        
        // TODO: 实现报警事件忽略逻辑
        log.info("忽略报警事件: segmentId={}, reason={}", segmentId, request.getReason());
        
        return ResponseEntity.ok("报警事件已忽略");
    }
    
    /**
     * 报警统计VO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AlarmStatisticsVO {
        private Long totalCount;
        private Long specificTypeCount;
        private String alarmType;
        private Date startTime;
        private Date endTime;
    }
    
    /**
     * 处理报警请求
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HandleAlarmRequest {
        private String handledBy;
        private String handleNote;
    }
    
    /**
     * 忽略报警请求
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class IgnoreAlarmRequest {
        private String reason;
        private String operator;
    }
}
