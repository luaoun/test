package com.px.ifp.spc.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.px.ifp.common.utils.ObjectConvertUtil;
import com.px.ifp.spc.dto.manager.request.SamplingStrategyDTO;
import com.px.ifp.spc.dto.manager.request.SaveOrUpdateSamplingStrategyDTO;
import com.px.ifp.spc.entity.SpcSamplingStrategy;
import com.px.ifp.spc.mapper.SpcSamplingStrategyMapper;
import com.px.ifp.spc.service.indicator.SpcSamplingStrategyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * SPC 采样策略配置表 服务实现类
 * </p>
 *
 * @author liuxj
 * @since 2026-01-08
 */
@Slf4j
@Service
public class SpcSamplingStrategyServiceImpl extends ServiceImpl<SpcSamplingStrategyMapper, SpcSamplingStrategy> implements SpcSamplingStrategyService {

    @Autowired
    private SpcSamplingStrategyMapper spcSamplingStrategyMapper;

    @Override
    public Boolean saveOrUpdate(SaveOrUpdateSamplingStrategyDTO reqDTO){
        String measureCode = reqDTO.getMeasureCode();
        Integer periodS = reqDTO.getPeriodS();
        SpcSamplingStrategy samplingStrategy = spcSamplingStrategyMapper.selectByMeasureCodeAndPeriodS(measureCode, periodS);

        SpcSamplingStrategy entity = ObjectConvertUtil.convert(reqDTO, SpcSamplingStrategy.class);

        // 将 features List<String> 转换为逗号分隔的字符串
        if (reqDTO.getFeatures() != null && !reqDTO.getFeatures().isEmpty()) {
            entity.setFeatures(String.join(",", reqDTO.getFeatures()));
        } else {
            entity.setFeatures(null);
        }

        if(samplingStrategy != null) {
            // 更新：如果 reqDTO 有 ID，则使用 reqDTO 的 ID；否则使用查询到的 ID
            if(reqDTO.getId() == null) {
                entity.setId(samplingStrategy.getId());
            }
            spcSamplingStrategyMapper.updateById(entity);
        } else {
            // 新增
            spcSamplingStrategyMapper.insert(entity);
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
        spcSamplingStrategyMapper.insert(entity);
    }

    @Override
    @Transactional
    public void batchSaveOrUpdate(String measureCode, List<SamplingStrategyDTO> samplingStrategies) {
        if (CollectionUtil.isEmpty(samplingStrategies)) {
            // 如果没有传入采样策略，创建一个默认的空策略
            createEmptyStrategy(measureCode);
            return;
        }

        for (SamplingStrategyDTO strategyDTO : samplingStrategies) {
            // 转换为实体对象
            SpcSamplingStrategy entity = ObjectConvertUtil.convert(strategyDTO, SpcSamplingStrategy.class);
            // 设置指标编码
            entity.setMeasureCode(measureCode);

            // 将 features List<String> 转换为逗号分隔的字符串
            if (strategyDTO.getFeatures() != null && !strategyDTO.getFeatures().isEmpty()) {
                entity.setFeatures(String.join(",", strategyDTO.getFeatures()));
            } else {
                entity.setFeatures(null);
            }

            if (strategyDTO.getId() != null) {
                // 如果有ID，执行更新
                spcSamplingStrategyMapper.updateById(entity);
                log.info("更新采样策略成功: measureCode={}, periodS={}, id={}",
                        measureCode, entity.getPeriodS(), entity.getId());
            } else {
                // 如果没有ID，检查是否已存在相同周期的策略
                if (strategyDTO.getPeriodS() != null) {
                    SpcSamplingStrategy existing = spcSamplingStrategyMapper.selectByMeasureCodeAndPeriodS(
                            measureCode, strategyDTO.getPeriodS());

                    if (existing != null) {
                        // 已存在，执行更新
                        entity.setId(existing.getId());
                        spcSamplingStrategyMapper.updateById(entity);
                        log.info("更新已存在的采样策略: measureCode={}, periodS={}, id={}",
                                measureCode, entity.getPeriodS(), existing.getId());
                    } else {
                        // 不存在，执行插入
                        spcSamplingStrategyMapper.insert(entity);
                        log.info("新增采样策略成功: measureCode={}, periodS={}",
                                measureCode, entity.getPeriodS());
                    }
                } else {
                    // 没有指定周期，直接插入
                    spcSamplingStrategyMapper.insert(entity);
                    log.info("新增采样策略成功: measureCode={}", measureCode);
                }
            }
        }
    }
}
