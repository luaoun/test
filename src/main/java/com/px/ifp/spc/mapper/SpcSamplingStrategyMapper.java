package com.px.ifp.spc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.px.ifp.spc.dto.manager.request.QuerySpcIndicatorDTO;
import com.px.ifp.spc.entity.SpcPointMetadataDO;
import com.px.ifp.spc.entity.SpcSamplingStrategy;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * SPC 采样策略配置表 Mapper 接口
 * </p>
 *
 * @author liuxj
 * @since 2026-01-09
 */
public interface SpcSamplingStrategyMapper extends BaseMapper<SpcSamplingStrategy> {

    int insert(SpcSamplingStrategy samplingStrategy);

    int updateById(SpcSamplingStrategy samplingStrategy);

    SpcSamplingStrategy selectByMeasureCodeAndPeriodS(@Param("measureCode") String measureCode, @Param("periodS") Integer periodS);
}
