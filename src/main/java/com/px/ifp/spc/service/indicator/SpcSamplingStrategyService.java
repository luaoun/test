package com.px.ifp.spc.service.indicator;

import com.baomidou.mybatisplus.extension.service.IService;
import com.px.ifp.spc.dto.manager.request.SamplingStrategyDTO;
import com.px.ifp.spc.dto.manager.request.SaveOrUpdateSamplingStrategyDTO;
import com.px.ifp.spc.entity.SpcSamplingStrategy;

import java.util.List;

/**
 * <p>
 * SPC 采样策略配置表 服务类
 * </p>
 *
 * @author liuxj
 * @since 2026-01-08
 */
public interface SpcSamplingStrategyService extends IService<SpcSamplingStrategy> {

    Boolean saveOrUpdate(SaveOrUpdateSamplingStrategyDTO reqDTO);

    void createEmptyStrategy(String measureCode);

    /**
     * 批量保存或更新采样策略
     * @param measureCode 指标编码
     * @param samplingStrategies 采样策略列表
     */
    void batchSaveOrUpdate(String measureCode, List<SamplingStrategyDTO> samplingStrategies);
}
