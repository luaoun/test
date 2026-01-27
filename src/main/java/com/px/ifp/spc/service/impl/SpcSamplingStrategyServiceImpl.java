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
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    @Transactional
    public void batchSaveOrUpdateByJobId(String measureCode, String jobId, List<SamplingStrategyDTO> samplingStrategies) {
        if (CollectionUtil.isEmpty(samplingStrategies)) {
            // 如果没有传入采样策略，创建一个默认的空策略
            createEmptyStrategyByJobId(measureCode, jobId);
            return;
        }

        // 检查传入的采样策略列表中是否有重复的 periodS（唯一约束检查）
        checkDuplicatePeriodS(samplingStrategies, jobId);

        for (SamplingStrategyDTO strategyDTO : samplingStrategies) {
            // 转换为实体对象
            SpcSamplingStrategy entity = ObjectConvertUtil.convert(strategyDTO, SpcSamplingStrategy.class);
            // 设置指标编码和作业ID
            entity.setMeasureCode(measureCode);
            entity.setJobId(jobId);

            // 将 features List<String> 转换为逗号分隔的字符串
            if (strategyDTO.getFeatures() != null && !strategyDTO.getFeatures().isEmpty()) {
                entity.setFeatures(String.join(",", strategyDTO.getFeatures()));
            } else {
                entity.setFeatures(null);
            }

            if (strategyDTO.getId() != null) {
                // 如果有ID，执行更新
                spcSamplingStrategyMapper.updateById(entity);
                log.info("更新采样策略成功: jobId={}, measureCode={}, periodS={}, id={}",
                        jobId, measureCode, entity.getPeriodS(), entity.getId());
            } else {
                // 如果没有ID，检查是否已存在相同jobId和周期的策略
                if (strategyDTO.getPeriodS() != null) {
                    SpcSamplingStrategy existing = spcSamplingStrategyMapper.selectByJobIdAndPeriodS(
                            jobId, strategyDTO.getPeriodS());

                    if (existing != null) {
                        // 已存在，执行更新
                        entity.setId(existing.getId());
                        spcSamplingStrategyMapper.updateById(entity);
                        log.info("更新已存在的采样策略: jobId={}, measureCode={}, periodS={}, id={}",
                                jobId, measureCode, entity.getPeriodS(), existing.getId());
                    } else {
                        // 不存在，执行插入
                        spcSamplingStrategyMapper.insert(entity);
                        log.info("新增采样策略成功: jobId={}, measureCode={}, periodS={}",
                                jobId, measureCode, entity.getPeriodS());
                    }
                } else {
                    // 没有指定周期，直接插入
                    spcSamplingStrategyMapper.insert(entity);
                    log.info("新增采样策略成功: jobId={}, measureCode={}", jobId, measureCode);
                }
            }
        }
    }

    @Override
    public void createEmptyStrategyByJobId(String measureCode, String jobId) {
        SpcSamplingStrategy entity = new SpcSamplingStrategy();
        entity.setMeasureCode(measureCode);
        entity.setJobId(jobId);
        entity.setPeriodS(60);
        entity.setEnabled(false);
        entity.setPeriodLabel("1m");
        entity.setStrategyType("periodic");
        entity.setWindowType("tumble");
        entity.setWindowSizeS(60);
        spcSamplingStrategyMapper.insert(entity);
    }

    @Override
    public List<SpcSamplingStrategy> selectByJobId(String jobId) {
        return spcSamplingStrategyMapper.selectByJobId(jobId);
    }

    @Override
    public void deleteByJobId(String jobId) {
        spcSamplingStrategyMapper.deleteByJobId(jobId);
    }

    /**
     * 检查采样策略列表中是否有重复的 periodS
     * 同一个 jobId 下不能有两个相同 periodS 的策略（唯一约束: job_id + period_s）
     *
     * @param samplingStrategies 采样策略列表
     * @param jobId 作业ID
     */
    private void checkDuplicatePeriodS(List<SamplingStrategyDTO> samplingStrategies, String jobId) {
        if (CollectionUtil.isEmpty(samplingStrategies)) {
            return;
        }

        // 检查传入列表中是否有重复的 periodS
        Map<Integer, Long> periodSCountMap = samplingStrategies.stream()
                .filter(s -> s.getPeriodS() != null)
                .collect(Collectors.groupingBy(SamplingStrategyDTO::getPeriodS, Collectors.counting()));

        List<Integer> duplicatePeriods = periodSCountMap.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (CollectionUtil.isNotEmpty(duplicatePeriods)) {
            throw new com.px.ifp.common.exception.BusinessException(
                    "采样策略配置中存在重复的采样周期(period_s): " + duplicatePeriods +
                    "，同一个指标配置下不能有相同采样周期的策略");
        }
    }
}
