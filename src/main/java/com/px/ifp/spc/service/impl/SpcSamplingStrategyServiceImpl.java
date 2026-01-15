package com.px.ifp.spc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.px.ifp.common.utils.ObjectConvertUtil;
import com.px.ifp.spc.dto.manager.request.SaveOrUpdateSamplingStrategyDTO;
import com.px.ifp.spc.entity.SpcSamplingStrategy;
import com.px.ifp.spc.mapper.SpcSamplingStrategyMapper;
import com.px.ifp.spc.service.indicator.SpcSamplingStrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * SPC 采样策略配置表 服务实现类
 * </p>
 *
 * @author liuxj
 * @since 2026-01-08
 */
@Service
public class SpcSamplingStrategyServiceImpl extends ServiceImpl<SpcSamplingStrategyMapper, SpcSamplingStrategy> implements SpcSamplingStrategyService {

    @Autowired
    private SpcSamplingStrategyMapper spcSamplingStrategyMapper;

    @Override
    public Boolean saveOrUpdate(SaveOrUpdateSamplingStrategyDTO reqDTO){
        String measureCode = reqDTO.getMeasureCode();
        Integer periodS = reqDTO.getPeriodS();
        SpcSamplingStrategy samplingStrategy = spcSamplingStrategyMapper.selectByMeasureCodeAndPeriodS(measureCode, periodS);
        if(samplingStrategy != null && samplingStrategy.getId() == reqDTO.getId()) {
            SpcSamplingStrategy entity = ObjectConvertUtil.convert(reqDTO,SpcSamplingStrategy.class);
            spcSamplingStrategyMapper.updateById(entity);
        }
        return true;
    }

    @Override
    public void createEmptyStrategy(String measureCode) {
        SpcSamplingStrategy entity = new SpcSamplingStrategy();
        entity.setMeasureCode(measureCode);
        entity.setPeriodS(60);
        entity.setEnabled(false);
        entity.setPeriodLabel("1m");
        entity.setStrategyType("periodic");
        entity.setWindowType("tumble");
        entity.setWindowSizeS(60);
    }
}
