package com.px.ifp.spc.service.indicator;

import com.baomidou.mybatisplus.extension.service.IService;
import com.px.ifp.spc.dto.manager.request.SaveOrUpdateSamplingStrategyDTO;
import com.px.ifp.spc.entity.SpcSamplingStrategy;

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
}
