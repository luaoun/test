package com.px.ifp.spc.mapper;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.px.ifp.spc.dto.publish.request.QuerySpcAlarmRealTimeDTO;
import com.px.ifp.spc.dto.manager.request.QuerySpcIndicatorDTO;
import com.px.ifp.spc.dto.publish.request.QuerySpcIndicatorListReqDTO;
import com.px.ifp.spc.entity.SpcPointMetadataDO;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SPC点位元数据Mapper接口 (V2)
 * 对应表：spc_point_metadata
 *
 * 重要说明：
 * 1. 所有方法签名与 V1 SpcIndicatorMapper 保持一致
 * 2. 仅将参数/返回值类型从 SpcPointMetadataDO 改为 SpcPointMetadataDO
 * 3. 方法名和参数名保持不变，确保向后兼容
 */
@Mapper
public interface SpcPointMetadataMapper extends BaseMapper<SpcPointMetadataDO> {

    /**
     * 插入SPC点位元数据
     */
    int insert(SpcPointMetadataDO pointMetadata);

    /**
     * 根据ID更新SPC点位元数据
     */
    int updateById(SpcPointMetadataDO pointMetadata);

    /**
     * 根据ID删除（逻辑删除）
     */
    void deleteById(@Param("id") Long id);

    /**
     * 根据ID查询
     */
    SpcPointMetadataDO selectById(@Param("id") Long id);

    /**
     * 根据作业ID查询
     */
    List<SpcPointMetadataDO> selectByJobId(@Param("jobId") String jobId);

    /**
     * 根据点位ID查询
     * 注意：参数名仍为 measureCode，映射到数据库的 point_id 列
     */
    List<SpcPointMetadataDO> selectByPoint(@Param("facCode") String facCode,@Param("measureCode") String measureCode);



    /**
     * 根据指标名称查询
     */
    SpcPointMetadataDO selectByIndicatorName(@Param("indicatorName") String indicatorName);

    List<SpcPointMetadataDO> selectAllList(
            @Param("indicatorName") String indicatorName,
            @Param("classCode") String classCode,
            @Param("systemCode") List<String> systemCode,
            @Param("indicatorLevel") String indicatorLevel,
            @Param("measureCode") List<String> measureCode,
            @Param("jobId") List<String> jobId,
            @Param("configType") String configType
    );

    default Long selectCountByPoint(String measureCode) {
        return selectCount(new LambdaQueryWrapper<SpcPointMetadataDO>()
                .eq(SpcPointMetadataDO::getMeasureCode, measureCode)
        );
    }

    default List<SpcPointMetadataDO> selectList(QuerySpcIndicatorListReqDTO reqDTO) {
        LambdaQueryWrapper<SpcPointMetadataDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SpcPointMetadataDO::getDeletedId,0);
        if (StringUtils.isNotBlank(reqDTO.getIndicatorName())){
            queryWrapper.like(SpcPointMetadataDO::getIndicatorName, reqDTO.getIndicatorName());
        }
        if (StringUtils.isNotBlank(reqDTO.getClassCode())){
            queryWrapper.eq(SpcPointMetadataDO::getClassCode, reqDTO.getClassCode());
        }
        if (reqDTO.getSystemCode() !=null && !reqDTO.getSystemCode().isEmpty()) {
            queryWrapper.in(SpcPointMetadataDO::getSystemCode, reqDTO.getSystemCode());
        }
        if (StringUtils.isNotBlank(reqDTO.getIndicatorLevel())){
            queryWrapper.eq(SpcPointMetadataDO::getIndicatorLevel, reqDTO.getIndicatorLevel());
        }
        if (StringUtils.isNotBlank(reqDTO.getPoint())){
            queryWrapper.like(SpcPointMetadataDO::getMeasureCode, reqDTO.getPoint());
        }
        if (!CollectionUtil.isEmpty(reqDTO.getJobId())){
            queryWrapper.in(SpcPointMetadataDO::getJobId,reqDTO.getJobId());
        }
        if (StringUtils.isNotBlank(reqDTO.getConfigType())){
            queryWrapper.eq(SpcPointMetadataDO::getConfigType, reqDTO.getConfigType());
        }
        queryWrapper.orderByDesc(SpcPointMetadataDO::getIndicatorLevel);
        return selectList(queryWrapper);
    }

    default List<SpcPointMetadataDO> selectList(QuerySpcAlarmRealTimeDTO reqDTO) {
        LambdaQueryWrapper<SpcPointMetadataDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SpcPointMetadataDO::getDeletedId,0);
        if (StringUtils.isNotBlank(reqDTO.getIndicatorName())){
            queryWrapper.like(SpcPointMetadataDO::getIndicatorName, reqDTO.getIndicatorName());
        }
        if (StringUtils.isNotBlank(reqDTO.getClassCode())){
            queryWrapper.eq(SpcPointMetadataDO::getClassCode, reqDTO.getClassCode());
        }
        if (reqDTO.getSystemCode() !=null && !reqDTO.getSystemCode().isEmpty()) {
            queryWrapper.in(SpcPointMetadataDO::getSystemCode, reqDTO.getSystemCode());
        }
        if (StringUtils.isNotBlank(reqDTO.getIndicatorLevel())){
            queryWrapper.eq(SpcPointMetadataDO::getIndicatorLevel, reqDTO.getIndicatorLevel());
        }
        if (reqDTO.getPoint() != null && !reqDTO.getPoint().isEmpty()){
            queryWrapper.in(SpcPointMetadataDO::getMeasureCode, reqDTO.getPoint());
        }
        if (!CollectionUtil.isEmpty(reqDTO.getJobId())){
            queryWrapper.in(SpcPointMetadataDO::getJobId,reqDTO.getJobId());
        }
        if (StringUtils.isNotBlank(reqDTO.getConfigType())){
            queryWrapper.eq(SpcPointMetadataDO::getConfigType, reqDTO.getConfigType());
        }
        queryWrapper.orderByDesc(SpcPointMetadataDO::getIndicatorLevel);
        return selectList(queryWrapper);
    }

    /**
     * 获取所有有效的指标ID
     * 注意：查询条件从 status=1 改为 enabled=1
     */
    List<Long> selectAllActiveIndicatorIds();

    /**
     * 获取所有活跃的SPC指标
     * 注意：查询条件从 status=1 改为 enabled=1
     */
    List<SpcPointMetadataDO> selectAllActiveIndicators();

    /**
     * 获取所有指标（包括非活跃的）
     */
    List<SpcPointMetadataDO> selectAllIndicators();

    /**
     * 根据指标ID列表获取指标信息
     */
    List<SpcPointMetadataDO> selectByIds(@Param("indicatorIds") List<Long> indicatorIds);

    List<QuerySpcIndicatorDTO> getJobSystemPointByConditions(@Param("conditions") List<QuerySpcIndicatorDTO> conditions, @Param("facCode") String facCode);


    default List<SpcPointMetadataDO> selectByJobIdList(List<String> jobIds) {
        return selectList(new LambdaQueryWrapper<SpcPointMetadataDO>()
                .in(SpcPointMetadataDO::getJobId, jobIds)
                .eq(SpcPointMetadataDO::getDeletedId, 0)
        );
    }

    default List<SpcPointMetadataDO> selectByPointList(List<String> measureCode) {
        return selectList(new LambdaQueryWrapper<SpcPointMetadataDO>()
                .in(SpcPointMetadataDO::getMeasureCode, measureCode)
                .eq(SpcPointMetadataDO::getDeletedId, 0)
        );
    }

    default List<SpcPointMetadataDO> selectList(List<String> jobIds, List<String> points) {
        LambdaQueryWrapper<SpcPointMetadataDO> queryWrapper = new LambdaQueryWrapper<>();
        if (CollectionUtil.isNotEmpty(jobIds)){
            queryWrapper.in(SpcPointMetadataDO::getJobId, jobIds);
        }
        if (CollectionUtil.isNotEmpty(points)){
            queryWrapper.in(SpcPointMetadataDO::getMeasureCode, points);
        }
        queryWrapper.orderByAsc(SpcPointMetadataDO::getId);
        return selectList(queryWrapper);
    }

    default List<SpcPointMetadataDO> selectByIndicatorName(List<String> indicatorNameList) {
        if (CollectionUtil.isEmpty(indicatorNameList)){
            return null;
        }
        return selectList(new LambdaQueryWrapper<SpcPointMetadataDO>()
                .in(SpcPointMetadataDO::getIndicatorName, indicatorNameList)
                .eq(SpcPointMetadataDO::getDeletedId, 0)
        );
    }
}
