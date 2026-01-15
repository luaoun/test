package com.px.ifp.spc.mapper;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.px.ifp.spc.bo.SpcAnalysisResultBO;
import com.px.ifp.spc.bo.SpcAnalysisResultNoteBO;
import com.px.ifp.spc.dto.manager.request.QuerySpcAnalysisResultListReqDTO;
import com.px.ifp.spc.entity.SpcAlarmEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * SPC报警事件Mapper接口
 */
@Mapper
public interface SpcAlarmEventMapper extends BaseMapper<SpcAlarmEvent> {
    
    
    /**
     * 更新报警事件结束信息
     */
    int updateAlarmEventOnEnd(@Param("segmentId") String segmentId,
                              @Param("endTime") Date endTime,
                              @Param("durationSeconds") Integer durationSeconds,
                              @Param("pointCount") Integer pointCount,
                              @Param("maxValue") BigDecimal maxValue,
                              @Param("maxValueTime") Date maxValueTime,
                              @Param("minValue") BigDecimal minValue,
                              @Param("minValueTime") Date minValueTime,
                              @Param("avgValue") BigDecimal avgValue,
                              @Param("firstValue") BigDecimal firstValue,
                              @Param("firstValueTime") Date firstValueTime,
                              @Param("lastValue") BigDecimal lastValue,
                              @Param("lastValueTime") Date lastValueTime,
                              @Param("endValue") BigDecimal endValue,
                              @Param("endValueTime") Date endValueTime,
                              @Param("peekValue") BigDecimal peekValue,
                              @Param("peekTime") Date peekTime);
    
    /**
     * 查询活跃的报警事件
     */
    List<SpcAlarmEvent> selectActiveAlarmEvents(@Param("indicatorIds") List<Long> indicatorIds,
                                                @Param("facCode") String facCode);
    
    /**
     * 查询指定时间范围内的报警事件
     */
    List<SpcAlarmEvent> selectAlarmEventsByTimeRange(@Param("startTime") Date startTime,
                                                     @Param("endTime") Date endTime,
                                                     @Param("systemCode") String systemCode,
                                                     @Param("classCode") String classCode,
                                                     @Param("facCode") String facCode);
    
    /**
     * 根据指标ID查询最近的报警事件
     */
    List<SpcAlarmEvent> selectRecentAlarmEventsByIndicatorId(@Param("indicatorId") Long indicatorId,
                                                             @Param("limit") Integer limit,
                                                             @Param("facCode") String facCode);
    
    /**
     * 统计指定时间范围内的报警事件数量
     */
    Long countAlarmEventsByTimeRange(@Param("startTime") Date startTime,
                                     @Param("endTime") Date endTime,
                                     @Param("alarmType") String alarmType,
                                     @Param("facCode") String facCode);
    
    
    /**
     * 更新报警事件统计信息
     */
    int updateEventStatistics(@Param("segmentId") String segmentId,
                              @Param("pointCount") Integer pointCount,
                              @Param("maxValue") BigDecimal maxValue,
                              @Param("maxValueTime") Date maxValueTime,
                              @Param("minValue") BigDecimal minValue,
                              @Param("minValueTime") Date minValueTime,
                              @Param("avgValue") BigDecimal avgValue,
                              @Param("firstValue") BigDecimal firstValue,
                              @Param("firstValueTime") Date firstValueTime,
                              @Param("lastValue") BigDecimal lastValue,
                              @Param("lastValueTime") Date lastValueTime,
                              @Param("peekValue") BigDecimal peekValue,
                              @Param("peekTime") Date peekTime,
                              @Param("alarmType") String alarmType,
                              @Param("severityLevel") String severityLevel);

    /**
     * 更新首值信息
     */
    int updateFirstValue(@Param("segmentId") String segmentId,
                         @Param("firstValue") BigDecimal firstValue,
                         @Param("firstValueTime") Date firstValueTime);

    /**
     * 更新最新值信息
     */
    int updateLastValue(@Param("segmentId") String segmentId,
                        @Param("lastValue") BigDecimal lastValue,
                        @Param("lastValueTime") Date lastValueTime);

    /**
     * 查询指定时间范围内的首值和最新值统计
     */
    Map<String, Object> selectValueStatisticsByTimeRange(@Param("jobIdList") List<String> jobIdList,
                                                         @Param("startTime") Date startTime,
                                                         @Param("endTime") Date endTime,
                                                         @Param("facCode") String facCodE);

    /**
     * 查询报警事件数量
     */
    List<SpcAnalysisResultBO> selectResultCount(@Param("jobIdList") List<String> jobIdList,
                                                @Param("pointList") List<String> pointList,
                                                @Param("classCode") String classCode,
                                                @Param("indicatorLevel") String indicatorLevel,
                                                @Param("startTime") Date startTime,
                                                @Param("endTime") Date endTime,
                                                @Param("facCode") String facCode);


    default List<SpcAlarmEvent> selectList(QuerySpcAnalysisResultListReqDTO reqDTO) {
        LambdaQueryWrapper<SpcAlarmEvent> queryWrapper = new LambdaQueryWrapper<>();
        if (CollectionUtil.isNotEmpty(reqDTO.getJobIdList())){
            queryWrapper.in(SpcAlarmEvent::getJobId, reqDTO.getJobIdList());
        }

        if (CollectionUtil.isNotEmpty(reqDTO.getPointList())){
            queryWrapper.in(SpcAlarmEvent::getPoint,reqDTO.getPointList());
        }

        //开始时间和结束时间的查询字段对应的是 event_time而不是create_time，用指标数据实际时间来匹配
        if (reqDTO.getStartTime() != null){
            queryWrapper.ge(SpcAlarmEvent::getPeekValueTime, reqDTO.getStartTime());
        }
        if (reqDTO.getEndTime() != null){
            queryWrapper.le(SpcAlarmEvent::getPeekValueTime, reqDTO.getEndTime());
        }
        if (StrUtil.isNotBlank(reqDTO.getAlarmType())){
            if (reqDTO.getAlarmType().equalsIgnoreCase("oos")){
                queryWrapper.eq(SpcAlarmEvent::getOos, true);
            }else if (reqDTO.getAlarmType().equalsIgnoreCase("oow")){
                queryWrapper.eq(SpcAlarmEvent::getOow, true);
            }else if (reqDTO.getAlarmType().equalsIgnoreCase("ooc")){
                queryWrapper.eq(SpcAlarmEvent::getOoc, true);
            }else if (reqDTO.getAlarmType().equalsIgnoreCase("oo3")){
                queryWrapper.eq(SpcAlarmEvent::getOo3, true);
            }else if(reqDTO.getAlarmType().equalsIgnoreCase("normal")){
                queryWrapper.eq(SpcAlarmEvent::getNormal,true);
            }
        }else {
            //默认只查询oos,ooc,oow,normal
            queryWrapper.and(
                    wrapper->wrapper.eq(SpcAlarmEvent::getOos,true)
                    .or().eq(SpcAlarmEvent::getOoc,true)
                    .or().eq(SpcAlarmEvent::getOow,true)
                    .or().eq(SpcAlarmEvent::getNormal,true)
            );
        }
        queryWrapper.orderByDesc(SpcAlarmEvent::getStartTime);
        return selectList(queryWrapper);
    }

    List<SpcAlarmEvent> selectAllList(
            @Param("jobIdList") List<String> jobIdList,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime,
            @Param("alarmType") String alarmType
    );

    List<SpcAnalysisResultNoteBO> selectAnalysisResultNotes(@Param("jobIdList") List<String> jobIdList, @Param("pointList") List<String> pointList, @Param("classCode") String classCode, @Param("systemCodeList") List<String> systemCodeList, @Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("facCode") String facCode, @Param("configType") String configType);


    /**
     * 查询指定时间范围内的报警事件明细（带完整统计信息）
     */
    List<Map<String, Object>> selectAlarmEventDetails(@Param("startTime") Date startTime,
                                                      @Param("endTime") Date endTime,
                                                      @Param("severityLevel") String severityLevel,
                                                      @Param("status") String status,
                                                      @Param("indicatorName") String indicatorName,
                                                      @Param("point") String point,
                                                      @Param("alarmType") String alarmType,
                                                      @Param("limit") Integer limit,
                                                      @Param("offset") Integer offset);

    /**
     * 统计指定时间范围内的报警事件汇总信息
     */
    Map<String, Object> selectAlarmEventSummary(@Param("startTime") Date startTime,
                                                @Param("endTime") Date endTime);

    /**
     * 按指标分组统计报警事件
     */
    List<Map<String, Object>> selectAlarmEventsByIndicator(@Param("startTime") Date startTime,
                                                           @Param("endTime") Date endTime);

    /**
     * 按日期分组统计报警事件趋势
     */
    List<Map<String, Object>> selectAlarmEventsTrend(@Param("startTime") Date startTime,
                                                     @Param("endTime") Date endTime);

    /**
     * 更新严重程度和对应的报警类型标记
     */
    int updateSeverityLevelAndMarkers(@Param("segmentId") String segmentId,
                                      @Param("severityLevel") String severityLevel,
                                      @Param("ooc") Boolean ooc,
                                      @Param("oos") Boolean oos,
                                      @Param("oow") Boolean oow,
                                      @Param("oo3") Boolean oo3);

    /**
     * 查询报警事件的最新更新时间（用于同步Redis缓存）
     */
    Date selectLatestUpdateTime(@Param("segmentId") String segmentId);

}
