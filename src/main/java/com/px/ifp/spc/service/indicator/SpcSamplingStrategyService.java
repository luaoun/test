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

    /**
     * 批量保存或更新采样策略（通过jobId关联）
     * @param measureCode 指标编码
     * @param jobId 作业ID，关联spc_point_metadata表的job_id
     * @param samplingStrategies 采样策略列表
     */
    void batchSaveOrUpdateByJobId(String measureCode, String jobId, List<SamplingStrategyDTO> samplingStrategies);

    /**
     * 创建空策略（通过jobId关联）
     * @param measureCode 指标编码
     * @param jobId 作业ID
     */
    void createEmptyStrategyByJobId(String measureCode, String jobId);

    /**
     * 根据jobId查询采样策略列表
     * @param jobId 作业ID
     * @return 采样策略列表
     */
    List<SpcSamplingStrategy> selectByJobId(String jobId);

    /**
     * 根据jobId删除采样策略
     * @param jobId 作业ID
     */
    void deleteByJobId(String jobId);
}
