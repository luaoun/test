package com.px.ifp.spc.mapper;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.px.ifp.spc.bo.SpcAnalysisResultBO;
import com.px.ifp.spc.bo.SpcAnalysisResultNoteBO;
import com.px.ifp.spc.bo.SpcAnalysisSimpleResultBO;
import com.px.ifp.spc.dto.manager.request.QuerySpcAnalysisResultListReqDTO;
import com.px.ifp.spc.entity.SpcAnalysisResult;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-10
 */
public interface SpcAnalysisResultMapper extends BaseMapper<SpcAnalysisResult> {
    default List<SpcAnalysisResult> selectList(QuerySpcAnalysisResultListReqDTO reqDTO) {
        LambdaQueryWrapper<SpcAnalysisResult> queryWrapper = new LambdaQueryWrapper<>();
        if (CollectionUtil.isNotEmpty(reqDTO.getJobIdList())){
            queryWrapper.in(SpcAnalysisResult::getJobId, reqDTO.getJobIdList());
        }

        if (CollectionUtil.isNotEmpty(reqDTO.getPointList())){
            queryWrapper.in(SpcAnalysisResult::getPoint,reqDTO.getPointList());
        }

        //开始时间和结束时间的查询字段对应的是 event_time而不是create_time，用指标数据实际时间来匹配
        if (reqDTO.getStartTime() != null){
            queryWrapper.ge(SpcAnalysisResult::getEventTime, reqDTO.getStartTime());
        }
        if (reqDTO.getEndTime() != null){
            queryWrapper.le(SpcAnalysisResult::getEventTime, reqDTO.getEndTime());
        }
        if (StrUtil.isNotBlank(reqDTO.getAlarmType())){
            if (reqDTO.getAlarmType().equalsIgnoreCase("oos")){
                queryWrapper.eq(SpcAnalysisResult::getOos, true);
            }else if (reqDTO.getAlarmType().equalsIgnoreCase("oow")){
                queryWrapper.eq(SpcAnalysisResult::getOow, true);
            }else if (reqDTO.getAlarmType().equalsIgnoreCase("ooc")){
                queryWrapper.eq(SpcAnalysisResult::getOoc, true);
            }else if (reqDTO.getAlarmType().equalsIgnoreCase("oo3")){
                queryWrapper.eq(SpcAnalysisResult::getOo3, true);
            }else if(reqDTO.getAlarmType().equalsIgnoreCase("normal")){
                queryWrapper.eq(SpcAnalysisResult::getNormal,true);
            }
        }
        queryWrapper.orderByDesc(SpcAnalysisResult::getEventTime);
        return selectList(queryWrapper);
    }

    List<SpcAnalysisResult> selectAllList(
            @Param("jobIdList") List<String> jobIdList,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime,
            @Param("alarmType") String alarmType
    );

    List<SpcAnalysisResultBO> selectResultCount(@Param("jobIdList") List<String> jobIdList, @Param("pointList") List<String> pointList, @Param("classCode") String classCode, @Param("indicatorLevel") String indicatorLevel, @Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("facCode") String facCode);

    List<SpcAnalysisResultNoteBO> selectAnalysisResultNotes(@Param("jobIdList") List<String> jobIdList, @Param("pointList") List<String> pointList, @Param("classCode") String classCode, @Param("systemCodeList") List<String> systemCodeList, @Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("facCode") String facCode, @Param("configType") String configType);

    List<SpcAnalysisSimpleResultBO> queryAnalysisResultsBySystemAndPoints(@Param("systemCode") String systemCode, @Param("points") List<String> points, @Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("facCode") String facCode);

    int updateSegmentId(@Param("id") Long id, @Param("segmentId") String segmentId);

    List<SpcAnalysisResult> selectBySegmentId(@Param("segmentId") String segmentId);

    List<SpcAnalysisResult> selectAbnormalPointsBySegmentId(@Param("segmentId") String segmentId);


}
