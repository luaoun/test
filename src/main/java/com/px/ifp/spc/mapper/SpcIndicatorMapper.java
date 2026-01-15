package com.px.ifp.spc.mapper;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.px.ifp.spc.dto.publish.request.QuerySpcAlarmRealTimeDTO;
import com.px.ifp.spc.dto.manager.request.QuerySpcIndicatorDTO;
import com.px.ifp.spc.dto.publish.request.QuerySpcIndicatorListReqDTO;
import com.px.ifp.spc.entity.SpcIndicatorDO;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-08
 */
public interface SpcIndicatorMapper extends BaseMapper<SpcIndicatorDO> {
    default List<SpcIndicatorDO> selectByIndicatorName(List<String> indicatorNameList) {
        if (CollectionUtil.isEmpty(indicatorNameList)){
            return null;
        }
        return selectList(new LambdaQueryWrapper<SpcIndicatorDO>()
                .in(SpcIndicatorDO::getIndicatorName, indicatorNameList)
                .eq(SpcIndicatorDO::getDeleted, false)
        );
    }

    default List<SpcIndicatorDO> selectByPoint(String facCode,String point) {
        LambdaQueryWrapper<SpcIndicatorDO> queryWrapper = new LambdaQueryWrapper<>();
        if(StringUtils.isNotEmpty(facCode))
            queryWrapper.eq(SpcIndicatorDO::getFacCode,facCode);
        queryWrapper.eq(SpcIndicatorDO::getPoint, point);
        queryWrapper.eq(SpcIndicatorDO::getDeleted, false);
        return selectList(queryWrapper);
    }

    default List<SpcIndicatorDO> selectByPointList(List<String> point) {
        return selectList(new LambdaQueryWrapper<SpcIndicatorDO>()
                .in(SpcIndicatorDO::getPoint, point)
                .eq(SpcIndicatorDO::getDeleted, false)
        );
    }

    default List<SpcIndicatorDO> selectByJobIdList(List<String> jobIds) {
        return selectList(new LambdaQueryWrapper<SpcIndicatorDO>()
                .in(SpcIndicatorDO::getJobId, jobIds)
                .eq(SpcIndicatorDO::getDeleted, false)
        );
    }

    default List<SpcIndicatorDO> selectList(List<String> jobIds, List<String> points) {
        LambdaQueryWrapper<SpcIndicatorDO> queryWrapper = new LambdaQueryWrapper<>();
        if (CollectionUtil.isNotEmpty(jobIds)){
            queryWrapper.in(SpcIndicatorDO::getJobId, jobIds);
        }
        if (CollectionUtil.isNotEmpty(points)){
            queryWrapper.in(SpcIndicatorDO::getPoint, points);
        }
        queryWrapper.orderByAsc(SpcIndicatorDO::getId);
        return selectList(queryWrapper);
    }

    default Long selectCountByPoint(String point) {
        return selectCount(new LambdaQueryWrapper<SpcIndicatorDO>()
                .eq(SpcIndicatorDO::getPoint, point)
        );
    }

    default List<SpcIndicatorDO> selectList(QuerySpcIndicatorListReqDTO reqDTO) {
        LambdaQueryWrapper<SpcIndicatorDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SpcIndicatorDO::getDeleted, false);
        if (StringUtils.isNotBlank(reqDTO.getIndicatorName())){
            queryWrapper.like(SpcIndicatorDO::getIndicatorName, reqDTO.getIndicatorName());
        }
        if (StringUtils.isNotBlank(reqDTO.getClassCode())){
            queryWrapper.eq(SpcIndicatorDO::getClassCode, reqDTO.getClassCode());
        }
        if (reqDTO.getSystemCode() !=null && !reqDTO.getSystemCode().isEmpty()) {
            queryWrapper.in(SpcIndicatorDO::getSystemCode, reqDTO.getSystemCode());
        }
        if (StringUtils.isNotBlank(reqDTO.getIndicatorLevel())){
            queryWrapper.eq(SpcIndicatorDO::getIndicatorLevel, reqDTO.getIndicatorLevel());
        }
        if (StringUtils.isNotBlank(reqDTO.getPoint())){
            queryWrapper.like(SpcIndicatorDO::getPoint, reqDTO.getPoint());
        }
        if (!CollectionUtil.isEmpty(reqDTO.getJobId())){
            queryWrapper.in(SpcIndicatorDO::getJobId,reqDTO.getJobId());
        }
        if (StringUtils.isNotBlank(reqDTO.getConfigType())){
            queryWrapper.eq(SpcIndicatorDO::getConfigType, reqDTO.getConfigType());
        }
        queryWrapper.orderByDesc(SpcIndicatorDO::getIndicatorLevel);
        return selectList(queryWrapper);
    }

    default List<SpcIndicatorDO> selectList(QuerySpcAlarmRealTimeDTO reqDTO) {
        LambdaQueryWrapper<SpcIndicatorDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SpcIndicatorDO::getDeleted, false);
        if (StringUtils.isNotBlank(reqDTO.getIndicatorName())){
            queryWrapper.like(SpcIndicatorDO::getIndicatorName, reqDTO.getIndicatorName());
        }
        if (StringUtils.isNotBlank(reqDTO.getClassCode())){
            queryWrapper.eq(SpcIndicatorDO::getClassCode, reqDTO.getClassCode());
        }
        if (reqDTO.getSystemCode() !=null && !reqDTO.getSystemCode().isEmpty()) {
            queryWrapper.in(SpcIndicatorDO::getSystemCode, reqDTO.getSystemCode());
        }
        if (StringUtils.isNotBlank(reqDTO.getIndicatorLevel())){
            queryWrapper.eq(SpcIndicatorDO::getIndicatorLevel, reqDTO.getIndicatorLevel());
        }
        if (reqDTO.getPoint() != null && !reqDTO.getPoint().isEmpty()){
            queryWrapper.in(SpcIndicatorDO::getPoint, reqDTO.getPoint());
        }
        if (!CollectionUtil.isEmpty(reqDTO.getJobId())){
            queryWrapper.in(SpcIndicatorDO::getJobId,reqDTO.getJobId());
        }
        if (StringUtils.isNotBlank(reqDTO.getConfigType())){
            queryWrapper.eq(SpcIndicatorDO::getConfigType, reqDTO.getConfigType());
        }
        queryWrapper.orderByDesc(SpcIndicatorDO::getIndicatorLevel);
        return selectList(queryWrapper);
    }


    List<QuerySpcIndicatorDTO> getJobSystemPointByConditions(@Param("conditions") List<QuerySpcIndicatorDTO> conditions, @Param("facCode") String facCode);

    List<SpcIndicatorDO> selectAllList(
            @Param("indicatorName") String indicatorName,
            @Param("classCode") String classCode,
            @Param("systemCode") List<String> systemCode,
            @Param("indicatorLevel") String indicatorLevel,
            @Param("point") List<String> point,
            @Param("jobId") List<String> jobId,
            @Param("configType") String configType
    );

    /**
     * 获取所有有效的指标ID
     */
    List<Long> selectAllActiveIndicatorIds();

    /**
     * 获取所有活跃的SPC指标
     */
    List<SpcIndicatorDO> selectAllActiveIndicators();

    /**
     * 获取所有指标（包括非活跃的）
     */
    List<SpcIndicatorDO> selectAllIndicators();

    /**
     * 根据指标ID列表获取指标信息
     */
    List<SpcIndicatorDO> selectByIds(@Param("indicatorIds") List<Long> indicatorIds);
}
