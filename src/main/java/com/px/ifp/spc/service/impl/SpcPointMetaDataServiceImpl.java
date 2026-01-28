package com.px.ifp.spc.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.constant.CommonConstants;
import com.px.ifp.common.dto.account.request.CurrentAccountDTO;
import com.px.ifp.common.dto.account.response.DictItemDTO;
import com.px.ifp.common.dto.account.response.MeaDTO;
import com.px.ifp.common.dto.data.DataDTO;
import com.px.ifp.common.dto.data.DataEventDTO;
import com.px.ifp.common.dto.dataquery.request.ScadaIndicatorDTO;
import com.px.ifp.common.dto.dataquery.response.DataResultDTO;
import com.px.ifp.common.dto.digitaltwins.request.QuerySystemCategoryByCodesDTO;
import com.px.ifp.common.dto.digitaltwins.response.SystemCategoryDTO;
import com.px.ifp.common.enums.DataQueryContentType;
import com.px.ifp.common.exception.BusinessException;
import com.px.ifp.common.service.dict.DictService;
import com.px.ifp.common.service.kafka.KafkaService;
import com.px.ifp.common.service.measure.MeaService;
import com.px.ifp.common.service.redis.RedisService;
import com.px.ifp.common.utils.*;
import com.px.ifp.spc.bo.*;
import com.px.ifp.spc.constant.DictConstant;
import com.px.ifp.spc.constant.RedisKeyConstants;
import com.px.ifp.spc.dto.IdReqDTO;
import com.px.ifp.spc.dto.manager.request.*;
import com.px.ifp.spc.dto.manager.response.QuerySpcIndicatorDetailRespDTO;
import com.px.ifp.spc.dto.manager.response.SpcAnalysisResultCountBO;
import com.px.ifp.spc.dto.manager.response.SpcAnalysisResultDTO;
import com.px.ifp.spc.dto.manager.response.SpcNoteDTO;
import com.px.ifp.spc.dto.publish.request.*;
import com.px.ifp.spc.dto.publish.response.*;
import com.px.ifp.spc.entity.SpcAlarmEvent;
import com.px.ifp.spc.entity.SpcAnalysisResult;
import com.px.ifp.spc.entity.SpcNoteDO;
import com.px.ifp.spc.entity.SpcPointMetadataDO;
import com.px.ifp.spc.enums.PointMetaDataTypeEnum;
import com.px.ifp.spc.error.OperationError;
import com.px.ifp.spc.mapper.SpcAlarmEventMapper;
import com.px.ifp.spc.mapper.SpcAnalysisResultMapper;
import com.px.ifp.spc.mapper.SpcNoteMapper;
import com.px.ifp.spc.mapper.SpcPointMetadataMapper;
import com.px.ifp.spc.remote.BaseFeign;
import com.px.ifp.spc.remote.StoreFeign;
import com.px.ifp.spc.service.cache.SpcConfigRefreshListener;
import com.px.ifp.spc.service.common.CommonService;
import com.px.ifp.spc.service.helper.SpcServiceHelper;
import com.px.ifp.spc.service.helper.SpcTimeAlignmentStrategy;
import com.px.ifp.spc.service.indicator.SpcPointMetaDataService;
import com.px.ifp.spc.service.indicator.SpcSamplingStrategyService;
import com.px.ifp.spc.util.DateExtUtils;
import com.px.ifp.spc.util.NumberUtil;
import com.px.ifp.spc.util.SpcUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-08
 */
@Service
@Slf4j
public class SpcPointMetaDataServiceImpl extends ServiceImpl<SpcPointMetadataMapper, SpcPointMetadataDO> implements SpcPointMetaDataService {
    private static int CURRENT_HOUR = 0;
    @Autowired
    private RedisService redisService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private SpcAlarmEventMapper alarmEventMapper;
    @Autowired
    private CommonService commonService;
    @Autowired
    private SpcPointMetadataMapper spcIndicatorMapper;
    @Autowired
    private SpcAnalysisResultMapper spcAnalysisResultMapper;
    @Autowired
    private SpcNoteMapper spcNoteMapper;
    @Autowired
    private DictService dictService;

    @Autowired
    private BaseFeign digitaltwinsFeign;

    @Autowired
    private StoreFeign dataFeign;

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private MeaService meaService;

    @Autowired
    private SpcServiceHelper spcServiceHelper;

    @Autowired
    private SpcAlarmEventMapper spcAlarmEventMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpcSamplingStrategyService spcSamplingStrategyService;

    @Override
    @Transactional
    public Boolean add(AddSpcPointMetaReqDTO reqDTO) {
        String measureCode = reqDTO.getPoint();
        if(StringUtils.isEmpty(measureCode)) measureCode = reqDTO.getMeasureCode();
        List<MeaDTO> meaDTO = meaService.getListByMeasureCodes(Arrays.asList(measureCode));
        if(CollectionUtil.isEmpty(meaDTO))
            throw new BusinessException(OperationError.PARAM_ERROR.getErrorCode(),"指标编码不存在");

        Optional<MeaDTO> optional = meaDTO.stream().findFirst();
        if(!optional.isPresent())
            throw new BusinessException(OperationError.PARAM_ERROR.getErrorCode(),"指标编码不存在");

        String unit = optional.get().getMeasureUnit();
        SpcPointMetadataDO spcIndicatorDO = ObjectConvertUtil.convert(reqDTO, SpcPointMetadataDO.class);
        spcIndicatorDO.setPointUnit(unit);
        spcIndicatorDO.setMeasureCode(optional.get().getMeasureCode());
        spcIndicatorDO.setMeasureName(optional.get().getMeasureName());
        spcIndicatorDO.setDataType(PointMetaDataTypeEnum.ANALOG.name());
        spcIndicatorDO.setPrecisionDecimal(reqDTO.getPrecisionDecimal());
        spcIndicatorDO.setRangeMin(reqDTO.getRangeMin());
        spcIndicatorDO.setRangeMax(reqDTO.getRangeMax());
        spcIndicatorDO.setYAxisMin(reqDTO.getYAxisMin());
        spcIndicatorDO.setYAxisMax(reqDTO.getYAxisMax());
        spcIndicatorDO.setYAxisStep(reqDTO.getYAxisStep());
        Boolean enabledRealtimeAlaram = reqDTO.getEnableRealtimeAlarm();
        if(reqDTO.getStatus() != null) enabledRealtimeAlaram = reqDTO.getStatus();
        spcIndicatorDO.setEnableRealtimeAlarm(enabledRealtimeAlaram);
        spcIndicatorDO.setEnableOfflineChart(reqDTO.getEnableOfflineChart());
        spcIndicatorDO.setEquipmentId(reqDTO.getEquipmentId());
        spcIndicatorDO.setEquipmentName(reqDTO.getEquipmentName());
        String ocapTemplateId = reqDTO.getOcapTemplateId();
        if(StringUtils.isNotEmpty(reqDTO.getOcap())) ocapTemplateId = reqDTO.getOcap();
        spcIndicatorDO.setOcapTemplateId(ocapTemplateId);
        spcIndicatorDO.setEnabledSpcControlled(reqDTO.getEnabledSpcControlled());
        // 将标签列表转换为逗号分隔的字符串
        if (reqDTO.getTags() != null && !reqDTO.getTags().isEmpty()) {
            spcIndicatorDO.setTags(String.join(",", reqDTO.getTags()));
        } else {
            spcIndicatorDO.setTags(null);
        }
        spcIndicatorDO.setAttributes(reqDTO.getAttributes());
        CurrentAccountDTO currentAccount = RequestHeaderUtil.getCurrentAccount();
        String createBy = StringUtils.isEmpty(currentAccount.getAccountId()) ? "system" : currentAccount.getAccountId();
        spcIndicatorDO.setCreatedBy(createBy);
        spcIndicatorDO.setUpdatedBy(createBy);

        long pointCount = spcIndicatorMapper.selectCountByPoint(reqDTO.getPoint()) + 1;
        //电科指标编码有 \ 要替换成 _ ，否则很多地方查询会报错
        String jobId = spcIndicatorDO.getMeasureCode().replace("\\","_");
        spcIndicatorDO.setJobId(jobId + "_" + pointCount);
        try {
            spcIndicatorMapper.insert(spcIndicatorDO);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("指标名称已存在");
        }

        // 处理采样策略：根据 periodLabel 自动填充 periodS 和 windowSizeS
        processSamplingStrategiesPeriodLabel(reqDTO.getSamplingStrategies());

        // 批量保存或更新采样策略（通过jobId关联，支持多个相同measureCode的点位配置）
        spcSamplingStrategyService.batchSaveOrUpdateByJobId(spcIndicatorDO.getMeasureCode(), spcIndicatorDO.getJobId(), reqDTO.getSamplingStrategies());
        //更新缓存
        List<SpcPointMetadataDO> spcIndicatorDOList = spcIndicatorMapper.selectByPoint(reqDTO.getFacCode(),spcIndicatorDO.getMeasureCode());
        String facCode = commonService.getThreadLocalFacCode();
        redisService.setCache(facCode+":"+ RedisKeyConstants.SPC_INDICATOR + spcIndicatorDO.getMeasureCode(), JSON.toJSONString(spcIndicatorDOList));
        // 发送全量刷新通知（因为新增的点位需要被所有实例识别）
        publishRefreshNotification("ALL", null,null);
        return true;
    }

    @Override
    @Transactional
    public Boolean update(UpdateSpcPointMetaReqDTO reqDTO) {
        String measureCode = reqDTO.getPoint();
        if(StringUtils.isEmpty(measureCode)) measureCode = reqDTO.getMeasureCode();
        List<MeaDTO> meaDTO = meaService.getListByMeasureCodes(Arrays.asList(measureCode));
        if(CollectionUtil.isEmpty(meaDTO) || Objects.isNull(meaDTO.get(0)))
            throw new BusinessException(OperationError.PARAM_ERROR.getErrorCode(),"指标编码不存在");

        //1. 按ID查询数据库
        SpcPointMetadataDO oldIndicator = spcIndicatorMapper.selectById(reqDTO.getId());


        //2. 更新最新值到数据库
        String unit = meaDTO.get(0).getMeasureUnit();
        SpcPointMetadataDO spcIndicatorDO = ObjectConvertUtil.convert(reqDTO, SpcPointMetadataDO.class);
        spcIndicatorDO.setPointUnit(unit);
        spcIndicatorDO.setMeasureCode(meaDTO.get(0).getMeasureCode());
        spcIndicatorDO.setMeasureName(meaDTO.get(0).getMeasureName());
        spcIndicatorDO.setDataType(PointMetaDataTypeEnum.ANALOG.name());
        spcIndicatorDO.setPrecisionDecimal(reqDTO.getPrecisionDecimal());
        spcIndicatorDO.setRangeMin(reqDTO.getRangeMin());
        spcIndicatorDO.setRangeMax(reqDTO.getRangeMax());
        if(reqDTO.getStartValue() != null) {
            spcIndicatorDO.setYAxisMin(reqDTO.getStartValue());
        }else {
            spcIndicatorDO.setYAxisMin(reqDTO.getYAxisMin());
        }
        if(reqDTO.getEndValue() != null) {
            spcIndicatorDO.setYAxisMax(reqDTO.getEndValue());
        }else {
            spcIndicatorDO.setYAxisMax(reqDTO.getYAxisMax());
        }
        if(reqDTO.getStep() != null) {
            spcIndicatorDO.setYAxisStep(reqDTO.getStep());
        }else {
            spcIndicatorDO.setYAxisStep(reqDTO.getYAxisStep());
        }
        Boolean enabledRealtimeAlaram = reqDTO.getEnableRealtimeAlarm();
        if(reqDTO.getStatus() != null) enabledRealtimeAlaram = reqDTO.getStatus();
        spcIndicatorDO.setEnableRealtimeAlarm(enabledRealtimeAlaram);
        spcIndicatorDO.setEnableOfflineChart(reqDTO.getEnableOfflineChart());
        spcIndicatorDO.setEquipmentId(reqDTO.getEquipmentId());
        spcIndicatorDO.setEquipmentName(reqDTO.getEquipmentName());
        String ocapTemplateId = reqDTO.getOcapTemplateId();
        if(StringUtils.isNotEmpty(reqDTO.getOcap())) ocapTemplateId = reqDTO.getOcap();
        spcIndicatorDO.setOcapTemplateId(ocapTemplateId);
        spcIndicatorDO.setEnabledSpcControlled(reqDTO.getEnabledSpcControlled());
        // 将标签列表转换为逗号分隔的字符串
        if (reqDTO.getTags() != null && !reqDTO.getTags().isEmpty()) {
            spcIndicatorDO.setTags(String.join(",", reqDTO.getTags()));
        } else {
            spcIndicatorDO.setTags(null);
        }
        spcIndicatorDO.setAttributes(reqDTO.getAttributes());
        CurrentAccountDTO currentAccount = RequestHeaderUtil.getCurrentAccount();
        String updateBy = StringUtils.isEmpty(currentAccount.getAccountId()) ? "system" : currentAccount.getAccountId();
        spcIndicatorDO.setUpdatedBy(updateBy);
        int updateFlag = 0;

        try {

            updateFlag = spcIndicatorMapper.updateById(spcIndicatorDO);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("指标名称已存在");
        }
        //没有更新任何数据 则返回
        if(updateFlag <=0)
            return true;

        //3. 更新spc指标定义缓存，同时 指标JobId和Point不能修改，只能删除在添加，因为缓存ID是用了 JobId+Point组合的key
        SpcPointMetadataDO spcIndicator = spcIndicatorMapper.selectById(reqDTO.getId());

        // 处理采样策略：根据 periodLabel 自动填充 periodS 和 windowSizeS
        processSamplingStrategiesPeriodLabel(reqDTO.getSamplingStrategies());

        // 批量保存或更新采样策略（通过jobId关联，支持多个相同measureCode的点位配置）
        if (reqDTO.getSamplingStrategies() != null) {
            spcSamplingStrategyService.batchSaveOrUpdateByJobId(spcIndicator.getMeasureCode(), spcIndicator.getJobId(), reqDTO.getSamplingStrategies());
        }
        //判断空指针，正常情况不会查不到 spcIndicator
        if(Objects.isNull(spcIndicator))
            return true;

        List<SpcPointMetadataDO> spcIndicatorDOList = spcIndicatorMapper.selectByPoint(spcIndicator.getFacCode(),spcIndicator.getMeasureCode());
        String facCode = commonService.getThreadLocalFacCode();

        //判断空集合，正常情况不会查不到 spcIndicatorDOList
        if(CollectionUtil.isNotEmpty(spcIndicatorDOList))
            redisService.setCache(facCode+":"+RedisKeyConstants.SPC_INDICATOR + spcIndicator.getMeasureCode(), JSON.toJSONString(spcIndicatorDOList));

        //4. 更新spc报警
        //4.1. 检测关键字段是否变更
        boolean criticalFieldsChanged = checkCriticalFieldsChanged(oldIndicator, spcIndicator);

        //4.2. 如果关键字段变更，处理活跃的报警事件
        if (criticalFieldsChanged) {
            handleActiveAlarmEventsOnUpdate(spcIndicator.getId(), oldIndicator, spcIndicator);
        }

        // 4.3. 如果关键字段变更，清理Redis缓存
        if (criticalFieldsChanged) {
            clearIndicatorRedisCache(spcIndicator.getId());
        }

        // 5. 发送配置刷新通知（让所有实例更新本地缓存）
        if (criticalFieldsChanged) {
            // 关键字段变更，发送单点刷新通知
            String cacheKey = buildRedisCacheKey(spcIndicator.getFacCode(), spcIndicator.getMeasureCode());

            publishRefreshNotification("SINGLE", spcIndicator.getFacCode(), spcIndicator.getMeasureCode());
        }
        return true;
    }

    /**
     * 清理指标相关的Redis缓存
     */
    private void clearIndicatorRedisCache(Long indicatorId) {
        try {
            // 清理报警状态缓存
            String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);
            String alarmStateKey = facCode+":spc:alarm_state:" + indicatorId;
            Boolean deleted = redisTemplate.delete(alarmStateKey);

            if (Boolean.TRUE.equals(deleted)) {
                log.info("清理指标Redis缓存成功: key={}", alarmStateKey);
            }

            // 可以根据需要清理其他相关缓存

        } catch (Exception e) {
            log.error("清理指标Redis缓存失败: indicatorId={}", indicatorId, e);
            // 缓存清理失败不影响主流程
        }
    }



    @Override
    public Boolean delete(IdReqDTO reqDTO) {
        //1.按ID查询SPC配置数据
        SpcPointMetadataDO spcIndicatorDO = spcIndicatorMapper.selectById(reqDTO.getId());
        if (spcIndicatorDO == null) {
            return false;
        }
        Date updateAt = new Date();
        spcIndicatorDO.setUpdatedAt(updateAt);
        spcIndicatorDO.setDeletedId(updateAt.getTime());

        spcIndicatorMapper.updateById(spcIndicatorDO);

        //2 删除Redis里Spc配置缓存
        String facCode = commonService.getThreadLocalFacCode();
        if (spcIndicatorDO != null) {
            redisService.deleteCache(facCode+":"+RedisKeyConstants.SPC_INDICATOR + spcIndicatorDO.getMeasureCode());
            // 发送缓存清除通知
            publishRefreshNotification("INVALIDATE", spcIndicatorDO.getFacCode(), spcIndicatorDO.getMeasureCode());
        }

        //3.删除Redis里Spc报警缓存
        if(spcIndicatorDO != null && spcIndicatorDO.getId() > 0) {
            clearIndicatorRedisCache(spcIndicatorDO.getId());
        }

        return true;
    }

    @Override
    public QuerySpcIndicatorDetailRespDTO queryDetail(IdReqDTO reqDTO) {
        SpcPointMetadataDO spcIndicatorDO = spcIndicatorMapper.selectById(reqDTO.getId());
        if (spcIndicatorDO == null) {
            return new QuerySpcIndicatorDetailRespDTO();
        }
        QuerySpcIndicatorDetailRespDTO respDTO = ObjectConvertUtil.convert(spcIndicatorDO, QuerySpcIndicatorDetailRespDTO.class);


        //补充新增加的字段
        respDTO.setPoint(respDTO.getMeasureCode());
        respDTO.setStatus(respDTO.getEnableRealtimeAlarm());
        respDTO.setOcap(respDTO.getOcapTemplateId());
        respDTO.setStartValue(respDTO.getYAxisMin());
        respDTO.setEndValue(respDTO.getYAxisMax());
        respDTO.setStep(respDTO.getYAxisStep());

        // 将 tags 字符串转换为 List<String>
        if (StrUtil.isNotBlank(spcIndicatorDO.getTags())) {
            respDTO.setTags(Arrays.asList(spcIndicatorDO.getTags().split(",")));
        }

        // 查询采样策略配置（通过jobId关联，支持多个相同measureCode的点位配置）
        List<com.px.ifp.spc.entity.SpcSamplingStrategy> strategies = spcSamplingStrategyService.selectByJobId(spcIndicatorDO.getJobId());
        // 如果通过jobId未查到（兼容旧数据），尝试通过measureCode查询
        if (CollectionUtil.isEmpty(strategies)) {
            LambdaQueryWrapper<com.px.ifp.spc.entity.SpcSamplingStrategy> strategyWrapper = new LambdaQueryWrapper<>();
            strategyWrapper.eq(com.px.ifp.spc.entity.SpcSamplingStrategy::getMeasureCode, spcIndicatorDO.getMeasureCode());
            strategies = spcSamplingStrategyService.list(strategyWrapper);
        }
        if (CollectionUtil.isNotEmpty(strategies)) {
            List<SamplingStrategyDTO> samplingStrategies = strategies.stream()
                    .map(strategy -> {
                        SamplingStrategyDTO dto = ObjectConvertUtil.convert(strategy, SamplingStrategyDTO.class);
                        // 将 features 字符串转换为 List<String>
                        if (StrUtil.isNotBlank(strategy.getFeatures())) {
                            dto.setFeatures(strategy.getFeatures());
                        }
                        return dto;
                    })
                    .collect(Collectors.toList());
            respDTO.setSamplingStrategies(samplingStrategies);
        }

        Map<String, Object> classDictMap = dictService.getLocalCache("className");
        if (MapUtil.isNotEmpty(classDictMap) && classDictMap.get(spcIndicatorDO.getClassCode()) != null) {
            respDTO.setClassName(JSONUtil.parseObj(classDictMap.get(spcIndicatorDO.getClassCode())).get("itemName").toString());
        }

        //查询出systemName
        QuerySystemCategoryByCodesDTO querySysCodeDTO = new QuerySystemCategoryByCodesDTO();
        querySysCodeDTO.setCodeList(Arrays.asList(respDTO.getSystemCode()));
        BaseResponseData<List<SystemCategoryDTO>> response = digitaltwinsFeign.queryByCodeList(querySysCodeDTO);
        //没有查到子系统信息 则不处理子系统名称了，直接返回
        if(CollectionUtil.isEmpty(response.getData()))
            return respDTO;

        List<SystemCategoryDTO> systemCategoryDTOList = response.getData();

        if (systemCategoryDTOList != null && systemCategoryDTOList.size() > 0) {
            SystemCategoryDTO systemCategory = systemCategoryDTOList.get(0);
            respDTO.setSystemName(systemCategory.getName());
        }

        return respDTO;
    }

    @Override
    public QuerySpcIndicatorDetailRespDTO querySpcIndicatorDetail(QuerySpcIndicatorDTO reqDTO) {
        LambdaQueryWrapper<SpcPointMetadataDO> queryWrapper = new LambdaQueryWrapper<>();
        if (Objects.nonNull(reqDTO.getId()))
            queryWrapper.eq(SpcPointMetadataDO::getId, reqDTO.getId());

        if (Objects.nonNull(reqDTO.getJobId()))
            queryWrapper.eq(SpcPointMetadataDO::getJobId, reqDTO.getJobId());

        if (Objects.nonNull(reqDTO.getIndicatorName()))
            queryWrapper.eq(SpcPointMetadataDO::getIndicatorName, reqDTO.getIndicatorName());

        if (Objects.nonNull(reqDTO.getIndicatorLevel()))
            queryWrapper.eq(SpcPointMetadataDO::getIndicatorLevel, reqDTO.getIndicatorLevel());

        if (Objects.nonNull(reqDTO.getClassCode()))
            queryWrapper.eq(SpcPointMetadataDO::getClassCode, reqDTO.getClassCode());

        if (Objects.nonNull(reqDTO.getSystemCode()))
            queryWrapper.eq(SpcPointMetadataDO::getClassCode, reqDTO.getSystemCode());

        if (Objects.nonNull(reqDTO.getPoint()))
            queryWrapper.eq(SpcPointMetadataDO::getMeasureCode, reqDTO.getPoint());

        List<SpcPointMetadataDO> list = this.list(queryWrapper);
        if (CollectionUtil.isEmpty(list))
            return null;
        SpcPointMetadataDO spcIndicatorDO = list.get(0);
        QuerySpcIndicatorDetailRespDTO respDTO = ObjectConvertUtil.convert(spcIndicatorDO, QuerySpcIndicatorDetailRespDTO.class);

        // 将 tags 字符串转换为 List<String>
        if (StrUtil.isNotBlank(spcIndicatorDO.getTags())) {
            respDTO.setTags(Arrays.asList(spcIndicatorDO.getTags().split(",")));
        }

        // 查询采样策略配置（通过jobId关联，支持多个相同measureCode的点位配置）
        List<com.px.ifp.spc.entity.SpcSamplingStrategy> strategies = spcSamplingStrategyService.selectByJobId(spcIndicatorDO.getJobId());
        // 如果通过jobId未查到（兼容旧数据），尝试通过measureCode查询
        if (CollectionUtil.isEmpty(strategies)) {
            LambdaQueryWrapper<com.px.ifp.spc.entity.SpcSamplingStrategy> strategyWrapper = new LambdaQueryWrapper<>();
            strategyWrapper.eq(com.px.ifp.spc.entity.SpcSamplingStrategy::getMeasureCode, spcIndicatorDO.getMeasureCode());
            strategies = spcSamplingStrategyService.list(strategyWrapper);
        }
        if (CollectionUtil.isNotEmpty(strategies)) {
            List<SamplingStrategyDTO> samplingStrategies = strategies.stream()
                    .map(strategy -> {
                        SamplingStrategyDTO dto = ObjectConvertUtil.convert(strategy, SamplingStrategyDTO.class);
                        // 将 features 字符串转换为 List<String>
                        if (StrUtil.isNotBlank(strategy.getFeatures())) {
                            dto.setFeatures(strategy.getFeatures());
                        }
                        return dto;
                    })
                    .collect(Collectors.toList());
            respDTO.setSamplingStrategies(samplingStrategies);
        }

        return respDTO;
    }


    @Override
    public List<QuerySpcIndicatorDetailRespDTO> querySpcIndicatorList(QuerySpcIndicatorDTO reqDTO) {
        LambdaQueryWrapper<SpcPointMetadataDO> queryWrapper = new LambdaQueryWrapper<>();
        if (Objects.nonNull(reqDTO.getId()))
            queryWrapper.eq(SpcPointMetadataDO::getId, reqDTO.getId());

        if (Objects.nonNull(reqDTO.getJobId()))
            queryWrapper.eq(SpcPointMetadataDO::getJobId, reqDTO.getJobId());

        if (Objects.nonNull(reqDTO.getIndicatorName()))
            queryWrapper.eq(SpcPointMetadataDO::getIndicatorName, reqDTO.getIndicatorName());

        if (Objects.nonNull(reqDTO.getIndicatorLevel()))
            queryWrapper.eq(SpcPointMetadataDO::getIndicatorLevel, reqDTO.getIndicatorLevel());

        if (Objects.nonNull(reqDTO.getClassCode()))
            queryWrapper.eq(SpcPointMetadataDO::getClassCode, reqDTO.getClassCode());

        if (Objects.nonNull(reqDTO.getSystemCode()))
            queryWrapper.eq(SpcPointMetadataDO::getClassCode, reqDTO.getSystemCode());

        if (Objects.nonNull(reqDTO.getPoint()))
            queryWrapper.eq(SpcPointMetadataDO::getMeasureCode, reqDTO.getPoint());

        List<SpcPointMetadataDO> list = this.list(queryWrapper);

        if (CollectionUtil.isEmpty(list))
            return Collections.emptyList();

        List<QuerySpcIndicatorDetailRespDTO> respDTOList = ObjectConvertUtil.convertList(list, QuerySpcIndicatorDetailRespDTO.class);

        return respDTOList;
    }


    @Override
    public List<SpcPointMetadataDO> list(Wrapper<SpcPointMetadataDO> queryWrapper) {
        return super.list(queryWrapper);
    }

    @Override
    public List<SpcAnalysisDTO> queryList(QuerySpcIndicatorListReqDTO reqDTO) {
        //匹配systemCode，将所选子系统的下级系统code都附带上
        spcServiceHelper.matchSystemCategory(reqDTO.getClassCode(),reqDTO.getSystemCode());

        List<SpcPointMetadataDO> spcIndicatorDOList = null;

        if(reqDTO.getIgnoreFacCode()){
            //查询全厂范围指标编码
            spcIndicatorDOList = spcIndicatorMapper.selectAllList(reqDTO.getIndicatorName(),reqDTO.getClassCode(),reqDTO.getSystemCode(),reqDTO.getIndicatorLevel(),StringUtils.isNotEmpty(reqDTO.getPoint())? Arrays.asList(reqDTO.getPoint()):null,reqDTO.getJobId(),reqDTO.getConfigType());
        }else {
            //按厂区查询spc指标编码
            spcIndicatorDOList = spcIndicatorMapper.selectList(reqDTO);
        }
        if (CollectionUtil.isEmpty(spcIndicatorDOList)) {
            return null;
        }
        Map<String, Object> classDictMap = dictService.getLocalCache(DictConstant.CLASS_NAME);
        List<SpcAnalysisDTO> spcAnalysisDTOList = ObjectConvertUtil.convertList(spcIndicatorDOList, SpcAnalysisDTO.class);
        List<String> pointList = new ArrayList<>();

        // 手动映射 enableRealtimeAlarm 到 status
        for (int i = 0; i < spcAnalysisDTOList.size(); i++) {
            SpcAnalysisDTO dto = spcAnalysisDTOList.get(i);
            SpcPointMetadataDO indicatorDO = spcIndicatorDOList.get(i);
            dto.setStatus(indicatorDO.getEnableRealtimeAlarm());
        }

        if (MapUtil.isNotEmpty(classDictMap)) {
            spcAnalysisDTOList.forEach(dto -> {
                if (classDictMap.get(dto.getClassCode()) != null) {
                    dto.setClassName(JSONUtil.parseObj(classDictMap.get(dto.getClassCode())).get("itemName").toString());
                }
                if (StringUtils.isNotEmpty(dto.getPoint())) {
                    pointList.add(dto.getPoint());
                }
            });
        }

        //去掉重复的systemCode
        List<String> systemCodes = spcAnalysisDTOList.stream().map(SpcAnalysisDTO::getSystemCode).collect(Collectors.toList());
        //查询出systemName
        QuerySystemCategoryByCodesDTO querySysCodeDTO = new QuerySystemCategoryByCodesDTO();
        querySysCodeDTO.setCodeList(systemCodes);
        BaseResponseData<List<SystemCategoryDTO>> response = digitaltwinsFeign.queryByCodeList(querySysCodeDTO);

            if(CollectionUtil.isEmpty(response.getData()))
            return spcAnalysisDTOList;

        List<SystemCategoryDTO> systemCategoryDTOList = response.getData();



        // 构建 systemCode -> systemName 的映射
        Map<String, String> systemCodeToNameMap = systemCategoryDTOList.stream()
                .collect(Collectors.toMap(SystemCategoryDTO::getCode, SystemCategoryDTO::getName));

        spcAnalysisDTOList.forEach(spcDataBO -> {
            String systemName = systemCodeToNameMap.get(spcDataBO.getSystemCode());
            spcDataBO.setSystemName(systemName); // 匹配并赋值 systemName
        });

        //不需要统计Spc报警数据
        if (reqDTO.getQuerySpcCount() == null || !reqDTO.getQuerySpcCount())
            return spcAnalysisDTOList;

        //没有时间范围，则不查报警数据 和 最大值、平均值、最小值、cp值、cpk值
        if (Objects.isNull(reqDTO.getStartTime()) || Objects.isNull(reqDTO.getEndTime()))
            return spcAnalysisDTOList;

        // 统计spc报警数据
        List<String> jobIdList = spcAnalysisDTOList.stream().map(SpcAnalysisDTO::getJobId).collect(Collectors.toList());
//        List<SpcAnalysisResultBO> countList = spcAnalysisResultMapper.selectResultCount(jobIdList, null, null, reqDTO.getIndicatorLevel(), reqDTO.getStartTime(), reqDTO.getEndTime(),reqDTO.getFacCode());
        List<SpcAnalysisResultBO> countList = spcAlarmEventMapper.selectResultCount(jobIdList, null, null, reqDTO.getIndicatorLevel(), reqDTO.getStartTime(), reqDTO.getEndTime(),reqDTO.getFacCode());
        if (CollectionUtil.isNotEmpty(countList)) {
            spcAnalysisDTOList.forEach(data -> {
                SpcAnalysisResultBO resultBO = countList.stream().filter(count -> count.getJobId().equals(data.getJobId())).findFirst().orElse(null);
                if (resultBO != null) {
                    data.setOocCount(resultBO.getOocCount());
                    data.setOowCount(resultBO.getOowCount());
                    data.setOosCount(resultBO.getOosCount());
                    data.setOo3Count(resultBO.getOo3Count());
                }
            });
        }

        //按分钟 查询指标的具体点位值，分析出 最大值、平均值、最小值、cp值、cpk值
        QuerySpcAnalysisReqDTO queryDTO = new QuerySpcAnalysisReqDTO();
        queryDTO.setStartTime(reqDTO.getStartTime());
        queryDTO.setEndTime(reqDTO.getEndTime());
        queryDTO.setTimePeriod("minutes");

        Map<String, List<CommonTimeStat>> dataStatMap = getScadaPointValueMap(null, 1,new String[]{"MAX","MIN","AVG","STDDEV_POP"}, queryDTO.getStartTime(), queryDTO.getEndTime(), pointList);
        getSpcFeatureData(dataStatMap,queryDTO.getTimePeriod(),spcAnalysisDTOList,new String[]{"MAX","MIN","AVG","STDDEV_POP"},queryDTO.getRadio(),queryDTO.getScale());
        return spcAnalysisDTOList;
    }



    @Override
    public Map<String, String> querySpcJobsBySystemCodeAndPoint(List<String> systemCodeAndPointList) {
        List<QuerySpcIndicatorDTO> conditions = new ArrayList<>();
        for (String key : systemCodeAndPointList) {
            QuerySpcIndicatorDTO condition = new QuerySpcIndicatorDTO();
            String splitKey[] = key.split(",");
            if (splitKey.length != 2) {
                continue;
            }
            condition.setSystemCode(splitKey[0]);
            condition.setPoint(splitKey[1]);
            conditions.add(condition);
        }
        String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);
        List<QuerySpcIndicatorDTO> list = spcIndicatorMapper.getJobSystemPointByConditions(conditions,facCode);

        Map<String, String> jobIdsMap = new HashMap<>();

        if (CollectionUtils.isEmpty(list))
            return jobIdsMap;

        for (QuerySpcIndicatorDTO spcIndicatorDTO : list) {
            String sysCode = spcIndicatorDTO.getSystemCode();
            String point = spcIndicatorDTO.getPoint();
            String key = sysCode + "," + point;
            jobIdsMap.put(key, spcIndicatorDTO.getJobId());
        }

        return jobIdsMap;

    }


    @Override
    public List<SpcAnalysisDTO> querySpcAnalysis(QuerySpcAnalysisReqDTO reqDTO) {

        if (CollectionUtil.isEmpty(reqDTO.getPointList()) && CollectionUtil.isEmpty(reqDTO.getJobIdList())) {
            throw new BusinessException("作业编号和点位必须传一个");
        }

        List<SpcPointMetadataDO> spcIndicatorDOList = null;

        if(reqDTO.getIgnoreFacCode()){
            //查询全厂范围指标编码
            spcIndicatorDOList = spcIndicatorMapper.selectAllList(null,null,null,null,reqDTO.getPointList(),reqDTO.getJobIdList(),reqDTO.getConfigType());
        }else {
            //按厂区查询spc指标编码
            spcIndicatorDOList = spcIndicatorMapper.selectList(reqDTO.getJobIdList(), reqDTO.getPointList());
        }

        List<SpcAnalysisDTO> spcAnalysisDTOList = new ArrayList<>();

        //1.查询到的spcIndicatorDOList指标信息 转换成 待查询对象 spcAnalysisDTOList
        //  要查询的Scada点位清单
        List<String> pointList = new ArrayList<>();
        if (!CollectionUtil.isEmpty(spcIndicatorDOList)) {
            spcAnalysisDTOList = ObjectConvertUtil.convertList(spcIndicatorDOList, SpcAnalysisDTO.class);
            // 手动映射 enableRealtimeAlarm 到 status
            for (int i = 0; i < spcAnalysisDTOList.size(); i++) {
                spcAnalysisDTOList.get(i).setStatus(spcIndicatorDOList.get(i).getEnableRealtimeAlarm());
            }
            spcAnalysisDTOList.stream().forEach(e -> pointList.add(e.getMeasureCode()));
        }

        //2.检查 入参 point是否都在 要查询的spcIndicatorDOList 里了
        if (CollectionUtil.isNotEmpty(reqDTO.getPointList())) {
            List<MeaDTO> measMetaData = meaService.getListByMeasureCodes(reqDTO.getPointList());
            for (String point : reqDTO.getPointList()) {
                // 匹配入参 point 不包含在 spcIndicatorDOList里，则需要新建一个 SpcAnalysisDTO 对象放到 spcAnalysisDTOList里，等待一下步被查询
                if (!pointList.contains(point)) {
                    SpcAnalysisDTO spcAnalysisDTO = new SpcAnalysisDTO();
                    spcAnalysisDTO.setPoint(point);
                    if (CollectionUtil.isNotEmpty(measMetaData))
                        measMetaData.forEach(meas -> {
                            if (Objects.nonNull(meas) && meas.getMeasureCode().equalsIgnoreCase(point)) {
                                spcAnalysisDTO.setPointName(meas.getMeasureName());
                                spcAnalysisDTO.setPointUnit(meas.getMeasureUnit());
                                if(StringUtils.isEmpty(spcAnalysisDTO.getIndicatorName())) {
                                    spcAnalysisDTO.setIndicatorName(meas.getMeasureName());
                                }
                            }
                        });
                    spcAnalysisDTOList.add(spcAnalysisDTO);
                    pointList.add(point);
                }
            }
        }
        //添加系统名称
        if(CollectionUtil.isNotEmpty(spcIndicatorDOList)) {
            QuerySystemCategoryByCodesDTO querySysCodeDTO = new QuerySystemCategoryByCodesDTO();
            querySysCodeDTO.setCodeList(spcIndicatorDOList.stream().map(SpcPointMetadataDO::getSystemCode).collect(Collectors.toList()));
            if (CollectionUtil.isNotEmpty(querySysCodeDTO.getCodeList())) {
                BaseResponseData<List<SystemCategoryDTO>> response = digitaltwinsFeign.queryByCodeList(querySysCodeDTO);
                if (response != null && response.getData() != null) {
                    List<SystemCategoryDTO> systemCodeResp = response.getData();
                    if (CollectionUtil.isNotEmpty(systemCodeResp)) {
                        Map<String, String> systemCodeToNameMap = systemCodeResp.stream()
                                .collect(Collectors.toMap(SystemCategoryDTO::getCode, SystemCategoryDTO::getName));

                        spcAnalysisDTOList.forEach(spcDataBO -> {
                            String systemName = systemCodeToNameMap.get(spcDataBO.getSystemCode());
                            spcDataBO.setSystemName(systemName); // 匹配并赋值 systemName
                        });
                    }
                }
            }
        }

        //3. 按 points 查询scada数据,计算 spc indication list中的每个指标的 最大值、平均值、最小值、cp值、cpk值
        Map<String, List<CommonTimeStat>> scadaDataStatMap = getScadaPointValueMap(reqDTO.getTimePeriod(), reqDTO.getTimeInterval(),reqDTO.getStatFunction(), reqDTO.getStartTime(), reqDTO.getEndTime(), pointList);
        if (Objects.isNull(scadaDataStatMap))
            return spcAnalysisDTOList;//没有数据直接返回

        //3.2补充报警事件的峰值数据，数仓的聚合表会丢失峰值关键数据，所以这里要把峰值数据补充到SPC曲线上

        QuerySpcAnalysisResultListReqDTO queryDTO = ObjectConvertUtil.convert(reqDTO,QuerySpcAnalysisResultListReqDTO.class);
        List<SpcAlarmEvent> spcAlarmResultDataList = spcAlarmEventMapper.selectList(queryDTO);
        /**
        if(CollectionUtil.isNotEmpty(spcAlarmResultDataList)){
            if (Objects.nonNull(scadaDataStatMap))
                scadaDataStatMap.keySet().forEach(pointKey->{
                    List<SpcAlarmEvent> spcAlarmDataList = spcAlarmResultDataList.stream().filter(spcAlarmData -> spcAlarmData.getPoint().equals(pointKey)).collect(Collectors.toList());
                    List<CommonTimeStat> mergeScadaDataList = SpcServiceHelper.mergeAnalysisResultAndScada(reqDTO.getStartTime(),reqDTO.getEndTime(),spcAlarmDataList, scadaDataStatMap.get(pointKey));
                    scadaDataStatMap.put(pointKey,mergeScadaDataList);
                });
        }**/

        //3.3 准备曲线数据
        getSpcAnalysisDTOS(scadaDataStatMap, reqDTO.getTimePeriod(), spcAnalysisDTOList,reqDTO.getRadio(),reqDTO.getScale());
        //3.4 计算最大值、最小值、平均值、CP值、CPK值这些特征数据
        Map<String, List<CommonTimeStat>> dataFeatureStatMap = getScadaPointValueMap(null, null,new String[]{"MAX","MIN","AVG","STDDEV_POP"}, reqDTO.getStartTime(), reqDTO.getEndTime(), pointList);
        getSpcFeatureData(dataFeatureStatMap, reqDTO.getTimePeriod(),spcAnalysisDTOList,new String[]{"MAX","MIN","AVG","STDDEV_POP"}, reqDTO.getRadio(),reqDTO.getScale());


        //4. 关联scada点位与报警点位的批注信息,
        if (!reqDTO.getRequireAlarmNote())
            return spcAnalysisDTOList;

        //循环设置每个point曲线的 报警批注Note数据
        for (SpcAnalysisDTO spcAnalysisDTO : spcAnalysisDTOList) {
            //4.1 获取点位的scada曲线数据
            List<CommonTimeStat> scadaDataStatList = scadaDataStatMap.get(spcAnalysisDTO.getMeasureCode());
            //4.2 按点位查询批注信息
            List<SpcAlarmNoteBO> spcAlarmNoteList = null;
            if (Objects.isNull(reqDTO.getSpcAnalysisResultId())) {
                spcAlarmNoteList = spcNoteMapper.selectAnalysisAlarmNote(spcAnalysisDTO.getJobId(), reqDTO.getStartTime(), reqDTO.getEndTime(), null, 1,reqDTO.getFacCode());
            } else {
                spcAlarmNoteList = spcNoteMapper.selectAnalysisAlarmNote(spcAnalysisDTO.getJobId(), null, null, reqDTO.getSpcAnalysisResultId(), null,reqDTO.getFacCode());
            }
            if(CollectionUtil.isEmpty(spcAlarmNoteList))
                return spcAnalysisDTOList;

            //4.3 添加批注Note信息到曲线数据上
            List<SpcAlarmEvent> spcAlarmEvents = spcAlarmResultDataList.stream().filter(event->event.getJobId().equalsIgnoreCase(spcAnalysisDTO.getJobId())).collect(Collectors.toList());
            SpcTimeAlignmentStrategy spcTimeAlignmentStrategy = SpcServiceHelper.getTimeAlignmentStrategy(reqDTO.getTimePeriod());
            List<CommonTimeStat> commonTimeStats = spcServiceHelper.enrichSpcCurveData(spcAlarmEvents,spcAlarmNoteList,scadaDataStatList,spcTimeAlignmentStrategy);
            spcAnalysisDTO.setPointValues(commonTimeStats);

            List<AlarmInterval> alarmIntervals = spcServiceHelper.buildAlarmIntervals(spcAlarmNoteList,spcTimeAlignmentStrategy);
            if(alarmIntervals != null) {
                List<AlarmXis> alarmXisDTOS = new ArrayList<>();
                spcAnalysisDTO.setAlarmTags(alarmXisDTOS);
                for (AlarmInterval alarmInterval : alarmIntervals) {

                    if(alarmInterval.getStartTime() != null && alarmInterval.getStartTime() > 0) {
                        AlarmXis startXisDTO = new AlarmXis();
                        startXisDTO.setXAxis(alarmInterval.getStartTime());
                        alarmXisDTOS.add(startXisDTO);
                    }
                    if(alarmInterval.getEndTime() != null && alarmInterval.getEndTime() > 0) {
                        AlarmXis endXisDTO = new AlarmXis();
                        endXisDTO.setXAxis(alarmInterval.getEndTime());
                        alarmXisDTOS.add(endXisDTO);
                    }
                }
            }

        }
        return spcAnalysisDTOList;
    }




    /**
     * 废弃不用这个方法了
     * 查询指标的具体点位值，分析出 最大值、平均值、最小值、cp值、cpk值
     * @param scadaDataStatMap
     * @param timePeriod
     * @param spcAnalysisDTOList 计算 spc indication list中的每个指标的 最大值、平均值、最小值、cp值、cpk值
     * @param radio 放大、缩小倍率 正数 放大，负数缩小
     * @param scale 保留小数位
     * @return
     */
    @Deprecated
    private void getSpcAnalysisDTOS(Map<String, List<CommonTimeStat>> scadaDataStatMap, String timePeriod, List<SpcAnalysisDTO> spcAnalysisDTOList,int radio,int scale) {
        if (scadaDataStatMap == null) return;
        String timeFormat = "HH:mm";
        if ("hour".equalsIgnoreCase(timePeriod)) {
            timeFormat = "MM-dd HH:mm";
        } else if ("day".equalsIgnoreCase(timePeriod)) {
            timeFormat = "yyyy-MM-dd";
        } else if ("months".equalsIgnoreCase(timePeriod)) {
            timeFormat = "yyyy-MM";
        }
        for (SpcAnalysisDTO spcAnalysisDTO : spcAnalysisDTOList) {
            List<CommonTimeStat> dataStatList = scadaDataStatMap.get(spcAnalysisDTO.getMeasureCode());
            for(CommonTimeStat commonTimeStat:dataStatList){
                if(Objects.nonNull(commonTimeStat.getValue())) {
                    BigDecimal pointValue = NumberUtil.autoAdjustByRatioAndScale(commonTimeStat.getValue(), radio, scale);
                    commonTimeStat.setValue(pointValue);
                }
                commonTimeStat.setPoint(spcAnalysisDTO.getMeasureCode());
            }
            spcAnalysisDTO.setPointValues(dataStatList);
            if (CollectionUtil.isEmpty(dataStatList)){
                continue;
            }
            List<CommonStat<BigDecimal>> commonStatList = Lists.newArrayList();
            for (CommonTimeStat commonTimeStat : dataStatList) {
                String name = DateUtil.format(DateUtils.convertLongToDate(commonTimeStat.getTime()), timeFormat);
                //处理点位数据的放大，缩小倍率，以及精度 ，radio为0不处理倍率， scale 为0不处理精度
                if(Objects.nonNull(commonTimeStat.getValue())) {
                    BigDecimal pointValue = NumberUtil.autoAdjustByRatioAndScale(commonTimeStat.getValue(), radio, scale);
                    commonStatList.add(new CommonStat<>(name, pointValue));
                }
            }
            spcAnalysisDTO.setFormatPointValues(commonStatList);
            //为了计算结果的准确性，排除value = null的数据
            dataStatList = dataStatList.stream().filter(stat -> stat.getValue() != null).collect(Collectors.toList());
            if (CollectionUtil.isEmpty(dataStatList)) {
                continue;
            }
        }
    }


    private void getSpcFeatureData(Map<String, List<CommonTimeStat>> scadaDataStatMap, String timePeriod, List<SpcAnalysisDTO> spcAnalysisDTOList,String[] indicators,int radio,int scale) {
        if (scadaDataStatMap == null) return;
        for (SpcAnalysisDTO spcAnalysisDTO : spcAnalysisDTOList) {
            List<CommonTimeStat> dataStatList = scadaDataStatMap.get(spcAnalysisDTO.getMeasureCode());
//            for(CommonTimeStat commonTimeStat:dataStatList){
//                if(Objects.nonNull(commonTimeStat.getValue())) {
//                    BigDecimal pointValue = NumberUtil.autoAdjustByRatioAndScale(commonTimeStat.getValue(), radio, scale);
//                    commonTimeStat.setValue(pointValue);
//                }
//                commonTimeStat.setPoint(spcAnalysisDTO.getPoint());
//            }
//            spcAnalysisDTO.setPointValues(dataStatList);
            if (CollectionUtil.isEmpty(dataStatList)){
                continue;
            }
            //为了计算结果的准确性，排除value = null的数据
            dataStatList = dataStatList.stream().filter(stat -> stat.getValue() != null).collect(Collectors.toList());
            if (CollectionUtil.isEmpty(dataStatList)) {
                continue;
            }
            //指标特征数据：最大值、最小值、平均值、方差值
            BigDecimal maxValue=null, minValue=null, avgValue=null, stddevPopValue=null;

            for(String indicator:indicators) {
                String aliasName = indicator.toLowerCase().concat("_value");
                Optional<CommonTimeStat> optional;
                switch (indicator){
                    case "MAX":
                        optional = dataStatList.stream().filter(stat -> stat.getAlias().equalsIgnoreCase(aliasName)).findFirst();
                        if(optional.isPresent()){
                            maxValue = optional.get().getValue();
                            spcAnalysisDTO.setMaxValue(maxValue.setScale(3, RoundingMode.HALF_UP));
                        }
                        break;
                    case "MIN":
                        optional = dataStatList.stream().filter(stat -> stat.getAlias().equalsIgnoreCase(aliasName)).findFirst();
                        if(optional.isPresent()){
                            minValue = optional.get().getValue();
                            spcAnalysisDTO.setMinValue(minValue.setScale(3, RoundingMode.HALF_UP));
                        }
                        break;
                    case "AVG":
                        optional = dataStatList.stream().filter(stat -> stat.getAlias().equalsIgnoreCase(aliasName)).findFirst();
                        if(optional.isPresent()){
                            avgValue = optional.get().getValue();
                            spcAnalysisDTO.setAvgValue(avgValue);
                        }
                        break;
                    case "STDDEV_POP":
                        optional = dataStatList.stream().filter(stat -> stat.getAlias().equalsIgnoreCase(aliasName)).findFirst();
                        if(optional.isPresent()){
                            stddevPopValue = optional.get().getValue();
                        }
                        break;
                }

                if (spcAnalysisDTO.getUslValue() != null && spcAnalysisDTO.getLslValue() != null) {
                    double upperValue = spcAnalysisDTO.getUslValue().doubleValue();
                    double lowerValue = spcAnalysisDTO.getLslValue().doubleValue();
                    //cp值
                    if(Objects.nonNull(stddevPopValue)) {
                        double cpValue = SpcUtils.calculateCP(stddevPopValue.doubleValue(), upperValue, lowerValue);
                        if (Double.isInfinite(cpValue) || Double.isNaN(cpValue)) {
                            spcAnalysisDTO.setCpValue(BigDecimal.valueOf(0.000));
                        } else {
                            spcAnalysisDTO.setCpValue(BigDecimal.valueOf(cpValue).setScale(3, RoundingMode.HALF_UP));
                        }
                    }
                    //cpk值
                    if(Objects.nonNull(stddevPopValue)) {
                        double cpkValue = SpcUtils.calculateCpk(stddevPopValue.doubleValue(), avgValue.doubleValue(), upperValue, lowerValue);
                        if (Double.isInfinite(cpkValue) || Double.isNaN(cpkValue)) {
                            spcAnalysisDTO.setCpkValue(BigDecimal.valueOf(0.000));
                        } else {
                            spcAnalysisDTO.setCpkValue(BigDecimal.valueOf(cpkValue).setScale(3, RoundingMode.HALF_UP));
                        }
                    }
                    //标准差
                    if(Objects.nonNull(stddevPopValue)) {
                        double sdValue = stddevPopValue.doubleValue();
                        if (Double.isInfinite(sdValue) || Double.isNaN(sdValue)) {
                            spcAnalysisDTO.setSigma3Value(BigDecimal.valueOf(0.000));
                        } else {
                            spcAnalysisDTO.setSigma3Value(BigDecimal.valueOf(sdValue * 3).setScale(3, RoundingMode.HALF_UP));
                        }
                    }
                }
            }
        }
    }

    //查询scada点位数据
    private Map<String, List<CommonTimeStat>> getScadaPointValueMap(String timePeriod,Integer timeInteveral, String statFunction, Date startTime, Date endTime, List<String> pointList) {
        String[] points = pointList.toArray(new String[pointList.size()]);//要查询指标对应的所有点位
        return commonService.queryScadaData(points, timePeriod, timeInteveral, new String[]{statFunction}, startTime, endTime);
    }

    private Map<String, List<CommonTimeStat>> getScadaPointValueMap(String timePeriod,Integer timeInteveral, String[] statFunctions, Date startTime, Date endTime, List<String> pointList) {
        String[] points = pointList.toArray(new String[pointList.size()]);//要查询指标对应的所有点位
        return commonService.queryScadaData(points, timePeriod, timeInteveral, statFunctions, startTime, endTime);
    }

        @Override
    public QuerySpcAnalysisResultCountRespDTO queryResultCount(QuerySpcAnalysisResultCountReqDTO reqDTO) {
        String facCode = null;
        //是否查询要忽略厂区条件
        if(!reqDTO.getIgnoreFacCode()){
             if(StringUtils.isNotEmpty(reqDTO.getFacCode()))
                 facCode = reqDTO.getFacCode();
             else
                facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);
        }
        List<SpcAnalysisResultBO> resultCountBOList = spcAlarmEventMapper.selectResultCount(reqDTO.getJobIdList(), reqDTO.getPointList(), reqDTO.getClassCode(), reqDTO.getIndicatorLevel(), reqDTO.getStartTime(), reqDTO.getEndTime(),facCode);
//        List<SpcAnalysisResultBO> resultCountBOList = spcAnalysisResultMapper.selectResultCount(reqDTO.getJobIdList(), reqDTO.getPointList(), reqDTO.getClassCode(), reqDTO.getIndicatorLevel(), reqDTO.getStartTime(), reqDTO.getEndTime(),facCode);
        if (CollectionUtil.isEmpty(resultCountBOList)) {
            return null;
        }
        List<SpcAnalysisResultCountBO> resultCountDTOList = ObjectConvertUtil.convertList(resultCountBOList, SpcAnalysisResultCountBO.class);
        int totalOowCount = resultCountDTOList.stream().mapToInt(SpcAnalysisResultCountBO::getOowCount).sum();
        int totalOocCount = resultCountDTOList.stream().mapToInt(SpcAnalysisResultCountBO::getOocCount).sum();
        int totalOosCount = resultCountDTOList.stream().mapToInt(SpcAnalysisResultCountBO::getOosCount).sum();
        int totalOo3Count = resultCountDTOList.stream().mapToInt(SpcAnalysisResultCountBO::getOo3Count).sum();
        int totalNormalCount = resultCountDTOList.stream().mapToInt(SpcAnalysisResultCountBO::getNormalCount).sum();

        QuerySpcAnalysisResultCountRespDTO respDTO = new QuerySpcAnalysisResultCountRespDTO();
        respDTO.setResultCountDTOList(resultCountDTOList);
        respDTO.setTotalOowCount(totalOowCount);
        respDTO.setTotalOocCount(totalOocCount);
        respDTO.setTotalOosCount(totalOosCount);
        respDTO.setTotalOo3Count(totalOo3Count);
        respDTO.setTotalNormalCount(totalNormalCount);
        //oo3不统计
        respDTO.setTotalCount(totalOowCount + totalOocCount + totalOosCount + totalNormalCount);
        return respDTO;
    }

    @Override
    public List<SpcAnalysisResultDTO> querySpcAnalysisResultList(QuerySpcAnalysisResultListReqDTO reqDTO) {

        List<SpcAlarmEvent> spcAlarmEventList = null;
        if(reqDTO.getIgnoreFacCode()){
            spcAlarmEventList = spcAlarmEventMapper.selectAllList(reqDTO.getJobIdList(),reqDTO.getStartTime(),reqDTO.getEndTime(),reqDTO.getAlarmType());
//            analysisResultDOList = spcAnalysisResultMapper.selectAllList(reqDTO.getJobIdList(),reqDTO.getStartTime(),reqDTO.getEndTime(),reqDTO.getAlarmType());
        }else {
            spcAlarmEventList = spcAlarmEventMapper.selectList(reqDTO);
//            analysisResultDOList = spcAnalysisResultMapper.selectList(reqDTO);
        }
        if (CollectionUtil.isEmpty(spcAlarmEventList)) {
            return null;
        }
        List<SpcAnalysisResultDTO> resultDTOList = Lists.newArrayListWithCapacity(spcAlarmEventList.size());
        spcAlarmEventList.forEach(spcAlarmEvent -> {
            SpcAnalysisResultDTO resultDTO = ObjectConvertUtil.convert(spcAlarmEvent, SpcAnalysisResultDTO.class);
            if (spcAlarmEvent.getOoc()) {
                resultDTO.setAlarmType("OOC");
            } else if (spcAlarmEvent.getOow()) {
                resultDTO.setAlarmType("OOW");
            } else if (spcAlarmEvent.getOos()) {
                resultDTO.setAlarmType("OOS");
            } else if (spcAlarmEvent.getOo3()) {
                resultDTO.setAlarmType("OO3");
            } else if (spcAlarmEvent.getNormal()) {
                resultDTO.setAlarmType("NORMAL");
            }
            resultDTO.setCreateTime(spcAlarmEvent.getStartTime());//报警事件开始时间，前端用了 createTime
            resultDTO.setEventTime(spcAlarmEvent.getPeekValueTime());//报警事件 的事件点 用报警峰值的时间来标记，每个报警事件只有一个事件点
            resultDTO.setEventEndTime(spcAlarmEvent.getEndTime());//当前报警事件的结束时间
            resultDTO.setDuration(DateExtUtils.secondsToChineseFormat(spcAlarmEvent.getDurationSeconds() == null? 0:spcAlarmEvent.getDurationSeconds()));
            resultDTOList.add(resultDTO);
            QuerySpcNoteListReqDTO querySpcNoteDTO = new QuerySpcNoteListReqDTO();
            querySpcNoteDTO.setBusinessKey(String.valueOf(resultDTO.getId()));
            List<SpcNoteDO> spcNoteList = spcNoteMapper.selectList(querySpcNoteDTO);
            if (!spcNoteList.isEmpty()) {
                SpcNoteDO lastSpcDO = spcNoteList.get(0);//取最新一条
                SpcNoteDTO lastSpcNoteDTO = ObjectConvertUtil.convert(lastSpcDO, SpcNoteDTO.class);
                resultDTO.setLastSpcNote(lastSpcNoteDTO);
            }
        });


        List<SpcPointMetadataDO> spcIndicatorDOList = null;

        if(CollectionUtils.isNotEmpty(reqDTO.getJobIdList()))
            spcIndicatorDOList = spcIndicatorMapper.selectByJobIdList(reqDTO.getJobIdList());
        else if(CollectionUtils.isNotEmpty(reqDTO.getPointList()))
            spcIndicatorDOList = spcIndicatorMapper.selectByPointList(reqDTO.getPointList());
        else
            throw new BusinessException(OperationError.PARAM_ERROR.getErrorCode(),"查询条件错误，jobIdList 和 pointIdList必须要填写一个 ");

        if(CollectionUtil.isEmpty(spcIndicatorDOList))
            return resultDTOList;

        Map<String,String> spcIndicatorSystemCodeMap = spcIndicatorDOList.stream().collect(Collectors.toMap(SpcPointMetadataDO::getJobId,SpcPointMetadataDO::getSystemCode,(existing,replacement)->existing
        ));

        if(MapUtils.isEmpty(spcIndicatorSystemCodeMap))
            return resultDTOList;

        List<String> systemCodeList = spcIndicatorSystemCodeMap.values().stream().distinct().collect(Collectors.toList());

        QuerySystemCategoryByCodesDTO querySystemCategroyDTO = new QuerySystemCategoryByCodesDTO();
        querySystemCategroyDTO.setCodeList(systemCodeList);

        BaseResponseData<List<SystemCategoryDTO>> response = digitaltwinsFeign.queryByCodeList(querySystemCategroyDTO);

        if(CollectionUtil.isEmpty(response.getData()))
            return resultDTOList;

        List<SystemCategoryDTO> systemCategoryDTOList = response.getData();

        Map<String,SystemCategoryDTO> systemCategoryDTOMap = systemCategoryDTOList.stream()
                .collect(Collectors.toMap(SystemCategoryDTO::getCode,value->value));

        resultDTOList.forEach(spcAnalysisResultData -> {
            String jobId = spcAnalysisResultData.getJobId();
            //获取jobId配置的systgemCode
            String systemCode = spcIndicatorSystemCodeMap.get(jobId);
            //根据systemCode 获取 systemName
            SystemCategoryDTO systemCategoryDTO = systemCategoryDTOMap.get(systemCode);
            spcAnalysisResultData.setSystemName(systemCategoryDTO.getName());
            spcAnalysisResultData.setSystemCode(systemCategoryDTO.getCode());
            spcAnalysisResultData.setPointValue(spcAnalysisResultData.getPeekValue());
        });

        return resultDTOList;
    }

    //    @Async
    @Override
    @Deprecated
    public void spcCalculate(DataEventDTO dataEventDTO) {
        if (dataEventDTO == null) {
            return;
        }
        if (CURRENT_HOUR != LocalDateTime.now().getHour()) {
            CURRENT_HOUR = LocalDateTime.now().getHour();
            refreshRedisCache(new QuerySpcIndicatorListReqDTO());
        }
        List<DataDTO> dataDTOList = JSONObject.parseArray(JSON.toJSONString(dataEventDTO.getData()), DataDTO.class);
        if (CollectionUtil.isEmpty(dataDTOList)) {
            return;
        }
        // TODO: 2024/5/11 数据量大话此处考虑多线程分批处理
        String facCode = commonService.getThreadLocalFacCode();
        for (DataDTO dataDTO : dataDTOList) {
            String strSpcIndicator = "";
            try {
                strSpcIndicator = redisService.getCacheString(facCode+":"+RedisKeyConstants.SPC_INDICATOR + dataDTO.getKey());
            } catch (Exception ex) {
                log.error(ex.getMessage() + "," + facCode+":"+RedisKeyConstants.SPC_INDICATOR + dataDTO.getKey());
            }
            if (StrUtil.isBlank(strSpcIndicator)) {
                continue;
            }
            List<SpcPointMetadataDO> spcIndicatorList = JSON.parseArray(strSpcIndicator, SpcPointMetadataDO.class);
            if (CollectionUtil.isEmpty(spcIndicatorList)) {
                continue;
            }
            BigDecimal value = null;
            try {
                value = BigDecimalUtil.toBigDecimal(dataDTO.getValue());
            } catch (IllegalArgumentException ex) {
                log.error(ex.getMessage() + ",value:" + dataDTO.getValue(), ex);
            }
            Date eventTime = null;
            try{
                eventTime = DateUtils.parse(dataDTO.getTime(), DateUtils.DatePatternEnum.DEFAULT_PATTERN);
            }catch(RuntimeException ex){
                log.error(ex.getMessage()+",event time:"+dataDTO.getTime(),ex);
            }

            if (value == null) {
                continue;
            }
            for (SpcPointMetadataDO spcIndicator : spcIndicatorList) {
                boolean ooc = false;
                boolean oos = false;
                boolean oow = false;
                //这个指标状态是禁用 ，则不计算报警规则
                if(spcIndicator.getEnableRealtimeAlarm()){
                    continue;
                }
                if ((spcIndicator.getUslValue() != null && value.compareTo(spcIndicator.getUslValue()) >= 0)
                        || (spcIndicator.getLslValue() != null && value.compareTo(spcIndicator.getLslValue()) <= 0)) {
                    oos = true;
                } else if ((spcIndicator.getUclValue() != null && value.compareTo(spcIndicator.getUclValue()) >= 0)
                        || (spcIndicator.getLclValue() != null && value.compareTo(spcIndicator.getLclValue()) <= 0)) {
                    ooc = true;
                } else if ((spcIndicator.getUwlValue() != null && value.compareTo(spcIndicator.getUwlValue()) >= 0)
                        || (spcIndicator.getLwlValue() != null && value.compareTo(spcIndicator.getLwlValue()) <= 0)) {
                    oow = true;
                }
                if (ooc || oow || oos) {
                    SpcAnalysisResult spcAnalysisResultDO = ObjectConvertUtil.convert(spcIndicator, SpcAnalysisResult.class);
                    spcAnalysisResultDO.setId(null);
                    spcAnalysisResultDO.setCreateTime(null);
                    if (Objects.nonNull(eventTime) && dataDTO.getTime().length() > 0) {
                        spcAnalysisResultDO.setEventTime(eventTime);
                    }
                    spcAnalysisResultDO.setIndicatorId(spcIndicator.getId());
                    spcAnalysisResultDO.setPointValue(value);
                    spcAnalysisResultDO.setOoc(ooc);
                    spcAnalysisResultDO.setOow(oow);
                    spcAnalysisResultDO.setOos(oos);
                    spcAnalysisResultDO.setOo3(false);
                    spcAnalysisResultDO.setFacCode(dataDTO.getFacCode());
                    spcAnalysisResultMapper.insert(spcAnalysisResultDO);
                    spcServiceHelper.startOcapFlow(spcIndicator.getOcapTemplateId(),spcAnalysisResultDO);
                }
            }
        }
    }

    @Transactional
    @Override
    public void importData(List<SpcIndicatorExcelBO> dtoList) {
        if (CollectionUtil.isEmpty(dtoList)) {
            return;
        }
        Map<String, String> classDictMap = getDictItemMap(DictConstant.CLASS_NAME);
        Map<String, String> spcLevelMap = getDictItemMap(DictConstant.SPC_LEVEL);
        dtoList.forEach(dto -> {
            if (StrUtil.isBlank(dto.getIndicatorName())) {
                throw new BusinessException("excel中有【指标名称】未填写");
            }
            if (StrUtil.isBlank(dto.getClassCode())) {
                throw new BusinessException("excel中有【课室】未填写");
            } else {
                if (CollectionUtil.isNotEmpty(classDictMap)) {
                    Map.Entry<String, String> entry = classDictMap.entrySet().stream().filter(e -> e.getValue().equals(dto.getClassCode())).findFirst().orElse(null);
                    if (entry == null) {
                        throw new BusinessException("excel中有【课室】填写不正确");
                    }
                    dto.setClassCode(entry.getKey());
                }
            }
            if (StrUtil.isBlank(dto.getSystemCode())) {
                throw new BusinessException("excel中有【系统】未填写");
            }
            if (StrUtil.isBlank(dto.getIndicatorLevel())) {
                throw new BusinessException("excel中有【指标级别】未填写");
            } else {
                if (CollectionUtil.isNotEmpty(spcLevelMap)) {
                    if (spcLevelMap.get(dto.getIndicatorLevel()) == null) {
                        throw new BusinessException("excel中有【指标级别】填写不正确");
                    }
                }
            }
            if (StrUtil.isBlank(dto.getPoint())) {
                throw new BusinessException("excel中有【绑定点位】未填写");
            }
        });
        //验证是否有重复指标名
        List<String> indicatorNameList = dtoList.stream().map(SpcIndicatorExcelBO::getIndicatorName).collect(Collectors.toList());
        Map<String, Long> indicatorNameMap = indicatorNameList.stream().collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        List<String> repeatNameList = Lists.newArrayList();
        indicatorNameMap.forEach((key, value) -> {
            if (value > 1) {
                repeatNameList.add(key);
            }
        });
        if (CollectionUtil.isNotEmpty(repeatNameList)) {
            String errMsg = "excel中有重复的指标名：" + StrUtil.join(",", repeatNameList);
            throw new BusinessException(errMsg);
        }
        List<SpcPointMetadataDO> indicatorDOList = spcIndicatorMapper.selectByIndicatorName(indicatorNameList);
        if (CollectionUtil.isNotEmpty(indicatorDOList)) {
            String errMsg = "excel中有已存在的指标名：" + indicatorDOList.stream().map(SpcPointMetadataDO::getIndicatorName).collect(Collectors.joining(","));
            throw new BusinessException(errMsg);
        }
        //验证点位是否有重复
        Map<String, Long> pointMap = dtoList.stream().map(SpcIndicatorExcelBO::getPoint).collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        List<String> repeatPointList = Lists.newArrayList();
        pointMap.forEach((key, value) -> {
            if (value > 1) {
                repeatPointList.add(key);
            }
        });
        if (CollectionUtil.isNotEmpty(repeatPointList)) {
            String errMsg = "excel中有重复点位，点位：" + StrUtil.join(",", repeatPointList);
            throw new BusinessException(errMsg);
        }

        List<String> pointList = dtoList.stream().map(SpcIndicatorExcelBO::getPoint).collect(Collectors.toList());
        List<SpcPointMetadataDO> existsIndicatorDOList = spcIndicatorMapper.selectList(null, pointList);
        Map<String, Long> existsPointMap = Maps.newHashMap();
        if (CollectionUtil.isNotEmpty(existsIndicatorDOList)) {
            existsPointMap = existsIndicatorDOList.stream().map(SpcPointMetadataDO::getMeasureCode).collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        }
        for (SpcIndicatorExcelBO dto : dtoList) {
            SpcPointMetadataDO entity = ObjectConvertUtil.convert(dto, SpcPointMetadataDO.class);
            if (entity.getYAxisMin() == null) {
                entity.setYAxisMin(BigDecimal.ZERO);
            }
            if (entity.getYAxisStep() == null) {
                entity.setYAxisStep(BigDecimal.ZERO);
            }
            Long pointCount = existsPointMap.get(dto.getPoint());
            entity.setJobId(dto.getPoint() + "_" + (pointCount == null ? 1 : pointCount + 1));
            entity.setCreatedBy(RequestHeaderUtil.getCurrentAccount().getAccountName());
            spcIndicatorMapper.insert(entity);
        }
    }

    private Map<String, String> getDictItemMap(String typeCode) {
        Map<String, Object> dictMap = dictService.getLocalCache(typeCode);
        if (CollectionUtil.isEmpty(dictMap)) {
            return Collections.emptyMap();
        }
        Map<String, String> dictItemMap = Maps.newHashMap();
        dictMap.values().forEach(o -> {
            dictItemMap.put(JSONUtil.parseObj(o).get("itemValue").toString(), JSONUtil.parseObj(o).get("itemName").toString());
        });
        return dictItemMap;
    }

    @Override
    public void refreshRedisCache(QuerySpcIndicatorListReqDTO reqDTO) {
        List<SpcPointMetadataDO> spcIndicatorDOList = spcIndicatorMapper.selectList(reqDTO);
        if (CollectionUtil.isEmpty(spcIndicatorDOList)) {
            return;
        }
        Map<String, List<SpcPointMetadataDO>> spcIndicatorDOMap = spcIndicatorDOList.stream().collect(Collectors.groupingBy(SpcPointMetadataDO::getMeasureCode));
        String facCode = commonService.getThreadLocalFacCode();

        //删除全部老的spc key数据
        Set<String> keys = redisService.keys(facCode+":"+RedisKeyConstants.SPC_INDICATOR+"**");
        if(CollectionUtil.isNotEmpty(keys)){
            keys.stream().forEach(key->redisService.deleteCache(key));
        }
        spcIndicatorDOMap.forEach((point, values) -> {
            redisService.setCache(facCode+":"+RedisKeyConstants.SPC_INDICATOR + point, JSON.toJSONString(values));
        });
    }

    @Override
    public void refreshAllRedisCache(){
        List<SpcPointMetadataDO> spcIndicatorDOList = spcIndicatorMapper.selectAllList(null, null, null, null, null, null, null);

        if (CollectionUtil.isEmpty(spcIndicatorDOList)) {
            return;
        }
        Map<String, List<SpcPointMetadataDO>> spcIndicatorDOMap = spcIndicatorDOList.stream().collect(Collectors.groupingBy(SpcPointMetadataDO::getMeasureCode));

        Map<String, Object> factoryAreaMap = dictService.getLocalCache(DictConstant.FACTORY_AREA);

        if(MapUtils.isEmpty(factoryAreaMap))
            return;

        //按创区刷新所有redis中spc配置
        for(Map.Entry<String,Object> entry : factoryAreaMap.entrySet()){
            String facKey = entry.getKey();
            String facCode = (String)JSONUtil.parseObj(entry.getValue()).get("itemValue");
            if(StringUtils.isEmpty(facCode))
                continue;

            //删除全部老的spc key数据
            Set<String> keys = redisService.keys(facCode+":"+RedisKeyConstants.SPC_INDICATOR+"**");
            if(CollectionUtil.isNotEmpty(keys)){
                keys.stream().forEach(key->redisService.deleteCache(key));
            }
            spcIndicatorDOMap.forEach((point, values) -> {
                redisService.setCache(facCode+":"+RedisKeyConstants.SPC_INDICATOR + point, JSON.toJSONString(values));
            });
        }


    }

    @Override
    public List<SpcAnalysisDTO> queryDetailByJobId(List<String> jobIds, List<String> points) {
        List<SpcPointMetadataDO> list = spcIndicatorMapper.selectList(jobIds, points);

        if (CollectionUtil.isEmpty(list))
            return Collections.emptyList();

        List<SpcAnalysisDTO> spcAnalysisDTOList = ObjectConvertUtil.convertList(list, SpcAnalysisDTO.class);
        if (CollectionUtil.isNotEmpty(spcAnalysisDTOList))
            for (int i = 0; i < spcAnalysisDTOList.size(); i++) {
                SpcAnalysisDTO spcAnalysisDTO = spcAnalysisDTOList.get(i);
                SpcPointMetadataDO spcIndicatorDO = list.get(i);
                spcAnalysisDTO.setIndicatorId(spcIndicatorDO.getId());
                // 手动映射 enableRealtimeAlarm 到 status
                spcAnalysisDTO.setStatus(spcIndicatorDO.getEnableRealtimeAlarm());
            }
        return spcAnalysisDTOList;
    }

    @Override
    public SpcAnalysisDTO getDetailByJobId(String jobId) {
        // 按jobId 应该只能查询出一条记录
        List<SpcAnalysisDTO> list = queryDetailByJobId(Arrays.asList(jobId), null);

        if (CollectionUtil.isEmpty(list))
            return null;
        return list.get(0);
    }

    @Override
    public List<SpcAnalysisResultNoteDTO> queryspcAnalysisResultNote(QuerySpcAnalysisResultNotesDTO query) {
        String facCode = null;
        //是否查询要忽略厂区条件
        if(!query.getIgnoreFacCode()){
            if(StringUtils.isNotEmpty(query.getFacCode()))
                facCode = query.getFacCode();
            else
                facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);
        }

        //将查询参数中的子系统进行填充，查出下级子系统并添加到 查询参数systemCode列表中
        spcServiceHelper.matchSystemCategory(query.getClassCode(),query.getSystemCode());

//        List<SpcAnalysisResultNoteBO> spcDataList = spcAnalysisResultMapper.selectAnalysisResultNotes(query.getJobIdList(), query.getPointList(), query.getClassCode(), query.getSystemCode(), query.getStartTime(), query.getEndTime(),facCode,query.getConfigType());
        List<SpcAnalysisResultNoteBO> spcDataList = spcAlarmEventMapper.selectAnalysisResultNotes(query.getJobIdList(), query.getPointList(), query.getClassCode(), query.getSystemCode(), query.getStartTime(), query.getEndTime(),facCode,query.getConfigType());

        if (CollectionUtil.isEmpty(spcDataList))
            return Collections.emptyList();

        Map<String, List<SpcAnalysisResultNoteBO>> groupedByIndicatorId = spcDataList.stream().collect(Collectors.groupingBy(SpcAnalysisResultNoteBO::getIndicatorName)).entrySet().stream()
                                                                                        .collect(Collectors.toMap(
                                                                                                Map.Entry::getKey,
                                                                                                entry -> entry.getValue().stream()
                                                                                                        .limit(query.getLimit())
                                                                                                        .collect(Collectors.toList()),
                                                                                                (existing, replacement) -> existing,
                                                                                                LinkedHashMap::new
                                                                                        ));


        List<SpcAnalysisResultNoteDTO> resultDTOList = groupedByIndicatorId.values().stream()
                .map(this::mergeGroupedData)
                .collect(Collectors.toList());

        List<SpcAnalysisResultNoteDTO> distinctBySystemCodeList = resultDTOList.stream().collect(Collectors.toMap(SpcAnalysisResultNoteDTO::getSystemCode, dto -> dto, (existing, replacement) -> existing)).values().stream().collect(Collectors.toList());
        //查找SystemCode对应的SystemName
        List<String> systemCodes = distinctBySystemCodeList.stream().map(SpcAnalysisResultNoteDTO::getSystemCode).collect(Collectors.toList());
//        QuerySystemCategoryByCodesDTO querySysCodeDTO = new QuerySystemCategoryByCodesDTO();
//        querySysCodeDTO.setCodeList(systemCodes);
        BaseResponseData<List<SystemCategoryDTO>> response = digitaltwinsFeign.queryByAllList();
        if(CollectionUtil.isEmpty(response.getData()))
            return resultDTOList;
        List<SystemCategoryDTO> systemCategoryDTOList = response.getData();
        // 构建 systemCode -> systemName 的映射
        Map<String, String> systemCodeToNameMap = systemCategoryDTOList.stream()
                .collect(Collectors.toMap(SystemCategoryDTO::getCode, SystemCategoryDTO::getName));
        Map<String,SystemCategoryDTO> systemCategoryDTOMap = systemCategoryDTOList.stream()
                        .collect(Collectors.toMap(SystemCategoryDTO::getCode,value->value));
        resultDTOList.forEach(spcDataBO -> {
            SystemCategoryDTO systemCategoryDTO = systemCategoryDTOMap.get(spcDataBO.getSystemCode());
            String systemName = systemCodeToNameMap.get(spcDataBO.getSystemCode());
            if(StringUtils.isNotEmpty(systemCategoryDTO.getPath())){
                StringBuilder systemFullName = new StringBuilder();
                Lists.newArrayList(systemCategoryDTO.getPath().split("/")).forEach(item->{
                    if(systemCodeToNameMap.containsKey(item)){
                        systemFullName.append(systemCodeToNameMap.get(item)).append("/");
                    }
                });
                systemName = systemFullName.toString();
            }
            spcDataBO.setSystemName(systemName); // 匹配并赋值 systemName
        });

        return resultDTOList;
    }

    @Override
    public List<SpcIndicatorDataSummaryDTO> queryAnalysisResultsBySystemAndPoints(QuerySpcIndicatorSummaryDTO queryDTO) {
        // 1.按系统和点位列表，查询指标报警异常数据;systemCode+point 可能有多个JobId记录，jobId是唯一的
        List<SpcAnalysisSimpleResultBO> list = spcAnalysisResultMapper.queryAnalysisResultsBySystemAndPoints(queryDTO.getSystemCode(), queryDTO.getPoints(), queryDTO.getStartTime(), queryDTO.getEndTime(),queryDTO.getFacCode());
        List<SpcIndicatorDataSummaryDTO> respDTOList = ObjectConvertUtil.convertList(list, SpcIndicatorDataSummaryDTO.class);

        if (CollectionUtil.isEmpty(respDTOList))
            return Collections.emptyList();

        List<String> foundPoints = new ArrayList<>();
        respDTOList.stream().forEach(dto -> {
            if (dto.getOow() > 0)
                dto.setAlarmType("oo2");
            else if (dto.getOos() > 0)
                dto.setAlarmType("oos");
            else if (dto.getOoc() > 0)
                dto.setAlarmType("ooc");
            else if (dto.getOo3() > 0)
                dto.setAlarmType("oo3");
            foundPoints.add(dto.getPoint());
        });

        // 2.补充没有查找到的point点位的值，用最新值赋值
        List<String> missingPoints = queryDTO.getPoints().stream().filter(item -> !foundPoints.contains(item)).collect(Collectors.toList());

        if (CollectionUtil.isEmpty(missingPoints))
            return respDTOList;

        String[] points = new String[missingPoints.size()];
        ScadaIndicatorDTO scadaQueryDTO = new ScadaIndicatorDTO();
        scadaQueryDTO.setIndicators(new String[]{"LAST"});
        scadaQueryDTO.setPoint(missingPoints.toArray(points));
        scadaQueryDTO.setContentType(DataQueryContentType.LIST);
        scadaQueryDTO.setStartTime(queryDTO.getStartTime());
        scadaQueryDTO.setEndTime(queryDTO.getEndTime());
        scadaQueryDTO.setBucketUnit("days");
        scadaQueryDTO.setBucketWidth(1);
        BaseResponseData<DataResultDTO> responseData = dataFeign.queryScadaIndicator(scadaQueryDTO);
        DataResultDTO pointValueResults = responseData.getData();

        //没有查到值，则返回
        if (pointValueResults == null)
            return respDTOList;

        for (Map<String, Object> pointValueMap : pointValueResults.getRows()) {
            long time = 0L;
            if (pointValueMap.get("days") != null) {
                time = DateUtils.parse(pointValueMap.get("days").toString()).getTime();
            }

            BigDecimal value = BigDecimal.ZERO;
            if (pointValueMap.get("last_value") != null) {
                value = new BigDecimal(pointValueMap.get("last_value").toString());
            }
            String point = pointValueMap.get("point").toString();
            Optional<SpcIndicatorDataSummaryDTO> optional = respDTOList.stream().filter(dto -> dto.getPoint().equalsIgnoreCase(point)).findFirst();
            if (optional.isPresent())
                optional.get().setPointValue(value);//从respDTOList拿出对象引用，并对pointValue赋值
        }
        return respDTOList;
    }

    @Override
    public List<NoModelExcelBO> querySpcAnalysisExport(QuerySpcAnalysisReqDTO reqDTO) {
        List<NoModelExcelBO> result = new ArrayList<>();
        List<SpcAnalysisDTO> spcAnalysisDTOS = this.querySpcAnalysis(reqDTO);
        if (CollectionUtil.isEmpty(spcAnalysisDTOS)) {
            return result;
        }
        for (SpcAnalysisDTO spcAnalysisDTO : spcAnalysisDTOS) {
            NoModelExcelBO modelExcelBO = new NoModelExcelBO();
            List<List<String>> head = new ArrayList<>();
            List<List<Object>> dataList = new ArrayList<>();
            modelExcelBO.setHead(head);
            modelExcelBO.setData(dataList);
            List<String> dateHeard = new ArrayList<>();
            dateHeard.add("量(" + spcAnalysisDTO.getPointUnit() + ")\\" + reqDTO.getTimePeriod());
            head.add(dateHeard);
            List<Object> hourDate = new ArrayList<>();
            hourDate.add(null);
            dataList.add(hourDate);
            if (CollectionUtil.isNotEmpty(spcAnalysisDTO.getFormatPointValues())) {
                for (CommonStat<BigDecimal> commonStat : spcAnalysisDTO.getFormatPointValues()) {
                    List<String> hourHeard = new ArrayList<>();
                    hourHeard.add(commonStat.getName());
                    head.add(hourHeard);
                    hourDate.add(commonStat.getNum());
                }
            }

            String sheetName = spcAnalysisDTO.getIndicatorName() + "统计";
            modelExcelBO.setSheetName(sheetName);
            result.add(modelExcelBO);
        }
        return result;
    }

    private SpcAnalysisResultNoteDTO mergeGroupedData(List<SpcAnalysisResultNoteBO> group) {

        SpcAnalysisResultNoteDTO result = new SpcAnalysisResultNoteDTO();
        SpcAnalysisResultNoteBO first = group.get(0);


        // 设置基础属性（从第一个数据直接取值，不需要合并）
        result.setJobId(first.getJobId());
        result.setIndicatorName(first.getIndicatorName());
        result.setClassCode(first.getClassCode());
        result.setSystemCode(first.getSystemCode());
        result.setIndicatorLevel(first.getIndicatorLevel());
        result.setPoint(first.getPoint());
        result.setPointUnit(first.getPointUnit());
        result.setStartValue(first.getStartValue());
        result.setStep(first.getStep());
        result.setTargetValue(first.getTargetValue());
        result.setUclValue(first.getUclValue());
        result.setLclValue(first.getLclValue());
        result.setUwlValue(first.getUwlValue());
        result.setLwlValue(first.getLwlValue());
        result.setUslValue(first.getUslValue());
        result.setLslValue(first.getLslValue());
        result.setU3lValue(first.getU3lValue());
        result.setL3lValue(first.getL3lValue());
        result.setEndValue(first.getEndValue());


        // 动态属性合并逻辑
        int totalOos = 0, totalOow = 0, totalOoc = 0, totalOo3 = 0;
        List<ExceptionBO> exceptionDTOList = new ArrayList<>();
        // 动态属性合并逻辑
        for (SpcAnalysisResultNoteBO data : group) {
            // 累加动态属性
            totalOos += data.getOos();
            totalOow += data.getOow();
            totalOoc += data.getOoc();
            totalOo3 += data.getOo3();

            // 构造 ExceptionBO 对象并添加到列表中
            ExceptionBO exceptionDTO = new ExceptionBO();
            if (data.getOos() == 1)
                exceptionDTO.setAlermType("oos");
            else if (data.getOow() == 1)
                exceptionDTO.setAlermType("oow");
            else if (data.getOoc() == 1)
                exceptionDTO.setAlermType("ooc");
            else if (data.getOo3() == 1)
                exceptionDTO.setAlermType("oo3");
            exceptionDTO.setPointValue(data.getPointValue()); // 使用 EndValue 示例，可改为其他字段
            exceptionDTO.setEventTime(data.getEventTime());
            exceptionDTO.setContent(data.getContent());
            exceptionDTOList.add(exceptionDTO);

        }

        // 设置累加的动态属性
        result.setOos(totalOos);
        result.setOow(totalOow);
        result.setOoc(totalOoc);
        result.setOo3(totalOo3);

        // 设置 ExceptionBO 列表
        result.setExceptionList(exceptionDTOList);

        return result;
    }

    @Override
    public String calculateSpcAlarm(String point, BigDecimal value, Date eventTime){
        String strSpcIndicator = "";
        String facCode = commonService.getThreadLocalFacCode();
        try {
            strSpcIndicator = redisService.getCacheString(facCode+":"+RedisKeyConstants.SPC_INDICATOR + point);
        } catch (Exception ex) {
            log.error(ex.getMessage() + "," + facCode+":"+RedisKeyConstants.SPC_INDICATOR + point);
        }
        if (StrUtil.isBlank(strSpcIndicator)) {
            return null;
        }
        List<SpcPointMetadataDO> spcIndicatorList = JSON.parseArray(strSpcIndicator, SpcPointMetadataDO.class);
        if (CollectionUtil.isEmpty(spcIndicatorList)) {
            return null;
        }
        for (SpcPointMetadataDO spcIndicator : spcIndicatorList) {
            boolean ooc = false;
            boolean oos = false;
            boolean oow = false;
            if ((spcIndicator.getUslValue() != null && value.compareTo(spcIndicator.getUslValue()) >= 0)
                    || (spcIndicator.getLslValue() != null && value.compareTo(spcIndicator.getLslValue()) <= 0)) {
                return "oos";

            } else if ((spcIndicator.getUclValue() != null && value.compareTo(spcIndicator.getUclValue()) >= 0)
                    || (spcIndicator.getLclValue() != null && value.compareTo(spcIndicator.getLclValue()) <= 0)) {
                return "ooc";
            } else if ((spcIndicator.getUwlValue() != null && value.compareTo(spcIndicator.getUwlValue()) >= 0)
                    || (spcIndicator.getLwlValue() != null && value.compareTo(spcIndicator.getLwlValue()) <= 0)) {
                return "oow";
            }
        }
        return null;
    }

    @Override
    public List<SpcIndicatorTreeDTO> querySpcIndicatorTree(QuerySpcIndicatorTreeDTO reqDTO) {
        List<SpcPointMetadataDO> spcIndicatorDOList = spcIndicatorMapper.selectAllList(null, reqDTO.getClassCode(), reqDTO.getSystemCode(), reqDTO.getIndicatorLevel(), null, null, reqDTO.getConfigType());
        Map<String, List<SpcPointMetadataDO>> groupByFacCode = spcIndicatorDOList.stream().sorted(Comparator.comparing(SpcPointMetadataDO::getFacCode)).collect(Collectors.groupingBy(SpcPointMetadataDO::getFacCode,LinkedHashMap::new, Collectors.toList()));
        List<SpcIndicatorTreeDTO> tree = new ArrayList<>();
        List<DictItemDTO> facCodeTypes = dictService.getDictItemByTypeCodes(Arrays.asList("factoryArea"));
        for (Map.Entry<String,List<SpcPointMetadataDO> > entry : groupByFacCode.entrySet()) {
            SpcIndicatorTreeDTO facNode = new SpcIndicatorTreeDTO();
            if(CollectionUtil.isNotEmpty(facCodeTypes)){
                Optional<String> matchfacCode = facCodeTypes.stream().filter(dictItemDTO -> entry.getKey().equalsIgnoreCase(dictItemDTO.getItemValue())).map(DictItemDTO::getItemName).findFirst();
                if(matchfacCode.isPresent()){
                    facNode.setLabel(matchfacCode.get());
                }else
                    facNode.setLabel(entry.getKey());
            }else
                facNode.setLabel(entry.getKey());
            facNode.setFacCode(entry.getKey());
            facNode.setChildren(entry.getValue().stream().map(r -> {
                SpcIndicatorTreeDTO child = new SpcIndicatorTreeDTO();
                child.setClassCode(r.getClassCode());
                child.setJobId(r.getJobId());
                child.setLabel(r.getIndicatorName());
                child.setPoint(r.getMeasureCode());
                child.setPointUnit(r.getPointUnit());
                return child;
            }).collect(Collectors.toList()));
            tree.add(facNode);
        }
        return tree;
    }

    @Override
    public List<SpcAlarmRelTimeDTO> querySpcAlarmRealTime(QuerySpcAlarmRealTimeDTO reqDTO) {
        List<SpcPointMetadataDO> spcIndicatorDOList = null;
        if(reqDTO.getIgnoreFacCode()){
            //查询全厂范围指标编码
            spcIndicatorDOList = spcIndicatorMapper.selectAllList(reqDTO.getIndicatorName(),reqDTO.getClassCode(),reqDTO.getSystemCode(),reqDTO.getIndicatorLevel(),reqDTO.getPoint(),reqDTO.getJobId(),reqDTO.getConfigType());
        }else {
            //按厂区查询spc指标编码
            spcIndicatorDOList = spcIndicatorMapper.selectList(reqDTO);
        }
        if (CollectionUtil.isEmpty(spcIndicatorDOList)) {
            return null;
        }

        List<String> specIds = spcIndicatorDOList.stream().map(SpcPointMetadataDO::getMeasureCode).collect(Collectors.toList());
        BaseResponseData<DataResultDTO> response = dataFeign.queryLastScadaValue(specIds);
        if(Objects.isNull(response.getData()) && CollectionUtils.isEmpty(response.getData().getRows()))
            return Collections.emptyList();
        List<Map<String, Object>> rows = response.getData().getRows();
        List<SpcAlarmRelTimeDTO> spcAlarmRelTimeDTOList = new ArrayList<>();
        for(Map row:rows){
            SpcAlarmRelTimeDTO spcAlarmRelTimeDTO = new SpcAlarmRelTimeDTO();
            String point = (String)row.get("point");
            spcAlarmRelTimeDTO.setPoint(point);

            String ctime = (String)row.get("ctime");
            spcAlarmRelTimeDTO.setCtime(DateUtils.parse(ctime));

            Object last_value = row.get("last_value");
            if(last_value instanceof  BigDecimal) {
                spcAlarmRelTimeDTO.setPointValue((BigDecimal) last_value);
            }else if(last_value instanceof Double) {
                Double doubleValue = (Double) last_value;
                spcAlarmRelTimeDTO.setPointValue(new BigDecimal(doubleValue));
            }else if(last_value instanceof String){
                String stringValue = (String) row.get("last_value");
                try {
                    spcAlarmRelTimeDTO.setPointValue(new BigDecimal(stringValue));
                }catch(Throwable t){
                    log.error(" number format exception:",t);
                    continue;
                }
            } else  continue;
            String alarmType = calculateSpcAlarm(spcAlarmRelTimeDTO.getPoint(), spcAlarmRelTimeDTO.getPointValue(), spcAlarmRelTimeDTO.getCtime());
            spcAlarmRelTimeDTO.setAlarmType(alarmType);
            spcAlarmRelTimeDTOList.add(spcAlarmRelTimeDTO);
        }

        return spcAlarmRelTimeDTOList;
    }

    @Override
    public void synchronizeSpcIndcatorUnit() {
        List<SpcPointMetadataDO> list = list();
        if(CollectionUtil.isEmpty(list))
            return;

        List<String> points = list.stream().map(SpcPointMetadataDO::getMeasureCode).collect(Collectors.toList());
        List<MeaDTO> measureMetaDataList = meaService.getListByMeasureCodes(points);
        Map<String,MeaDTO> measureMap = new HashMap<>();
        for(MeaDTO meaDTO:measureMetaDataList){
            MeaDTO tempMeaDTO = measureMap.get(meaDTO.getMeasureCode());
            //没有则放入map中
            if(Objects.isNull(tempMeaDTO) && Objects.nonNull(meaDTO)) {
                measureMap.put(meaDTO.getMeasureCode(), meaDTO);
            }
        }

        list.stream().forEach(spcIndicator -> {
            String point = spcIndicator.getMeasureCode();
            MeaDTO meaDTO = measureMap.get(point);
            if(Objects.nonNull(meaDTO))
                spcIndicator.setPointUnit(meaDTO.getMeasureUnit());
        });
        updateBatchById(list);
    }

    @Override
    public Boolean updateStatus(SpcStatusChangeDTO statusChangeDTO){

        //1.查找spc配置
        SpcPointMetadataDO oldIndicator = getById(statusChangeDTO.getId());
        if(Objects.isNull(oldIndicator))
            return true;

        //2.更新开关状态
        LambdaUpdateWrapper<SpcPointMetadataDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SpcPointMetadataDO::getId,statusChangeDTO.getId());
        updateWrapper.set(SpcPointMetadataDO::getEnableRealtimeAlarm,statusChangeDTO.isStatus());
        this.update(updateWrapper);

        // 2.更新Redis Spc配置缓存
        // 2.1 查找更新后的数据
        SpcPointMetadataDO spcIndicator = getById(statusChangeDTO.getId());
        // 2.2 更新Redis缓存
        String facCode = commonService.getThreadLocalFacCode();
        redisService.setCache(facCode+":"+ RedisKeyConstants.SPC_INDICATOR + oldIndicator.getMeasureCode(), JSON.toJSONString(Arrays.asList(spcIndicator)));

        // 3. 更新spc报警
        // 3.1. 检测关键字段是否变更
        boolean criticalFieldsChanged = checkCriticalFieldsChanged(oldIndicator, spcIndicator);

        // 3.2. 如果关键字段变更，处理活跃的报警事件
        if (criticalFieldsChanged) {
            handleActiveAlarmEventsOnUpdate(spcIndicator.getId(), oldIndicator, spcIndicator);
        }

        // 3.3. 如果关键字段变更，清理Redis缓存
        if (criticalFieldsChanged) {
            clearIndicatorRedisCache(spcIndicator.getId());
        }

        return true;
    }

    /**
     * 获取所有有效的指标ID（用于状态恢复）
     */
    @Override
    public List<Long> getAllActiveIndicatorIds() {
        return spcIndicatorMapper.selectAllActiveIndicatorIds();
    }

    /**
     * 获取所有活跃的SPC指标（用于XXL Job一致性检查）
     */
    public List<SpcPointMetadataDO> getAllActiveIndicators() {
        return spcIndicatorMapper.selectAllActiveIndicators();
    }

    /**
     * 获取所有指标（包括非活跃的）
     */
    public List<SpcPointMetadataDO> getAllIndicators() {
        return spcIndicatorMapper.selectAllIndicators();
    }

    /**
     * 根据指标ID列表获取指标信息
     */
    public List<SpcPointMetadataDO> getIndicatorsByIds(List<Long> indicatorIds) {
        if (indicatorIds == null || indicatorIds.isEmpty()) {
            return Collections.emptyList();
        }
        return spcIndicatorMapper.selectByIds(indicatorIds);
    }

    @Override
    public List<SpcPointMetadataDO> getIndicatorsByPoint(String facCode,String point) {
        return spcIndicatorMapper.selectByPoint(facCode,point);
    }
    /**
     * 检查关键字段是否发生变更
     * 关键字段包括：point, USL, UCL, UWL, LSL, LCL, LWL, status
     */
    private boolean checkCriticalFieldsChanged(SpcPointMetadataDO oldIndicator, SpcPointMetadataDO newIndicator) {
        // 检查point变更
        if (!Objects.equals(oldIndicator.getMeasureCode(), newIndicator.getMeasureCode())) {
            return true;
        }

        // 检查状态变更
        if (!Objects.equals(oldIndicator.getEnableRealtimeAlarm(), newIndicator.getEnableRealtimeAlarm())) {
            return true;
        }

        // 检查控制限变更（使用BigDecimal的compareTo方法）
        if (isLimitChanged(oldIndicator.getUslValue(), newIndicator.getUslValue(), "USL")) return true;
        if (isLimitChanged(oldIndicator.getLslValue(), newIndicator.getLslValue(), "LSL")) return true;
        if (isLimitChanged(oldIndicator.getUclValue(), newIndicator.getUclValue(), "UCL")) return true;
        if (isLimitChanged(oldIndicator.getLclValue(), newIndicator.getLclValue(), "LCL")) return true;
        if (isLimitChanged(oldIndicator.getUwlValue(), newIndicator.getUwlValue(), "UWL")) return true;
        if (isLimitChanged(oldIndicator.getLwlValue(), newIndicator.getLwlValue(), "LWL")) return true;

        return false;
    }

    /**
     * 检查单个限值是否变更
     */
    private boolean isLimitChanged(BigDecimal oldValue, BigDecimal newValue, String limitName) {
        // 都为null，未变更
        if (oldValue == null && newValue == null) {
            return false;
        }
        // 一个为null，另一个不为null，已变更
        if (oldValue == null || newValue == null) {
            return true;
        }
        // 都不为null，比较值
        if (oldValue.compareTo(newValue) != 0) {
            return true;
        }
        return false;
    }

    /**
     * 处理指标更新时的活跃报警事件
     */
    private void handleActiveAlarmEventsOnUpdate(Long indicatorId, SpcPointMetadataDO oldIndicator, SpcPointMetadataDO newIndicator) {
        try {
            // 查找该指标的所有活跃报警事件
            QueryWrapper<SpcAlarmEvent> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("indicator_id", indicatorId)
                    .eq("status", "ACTIVE")
                    .isNull("end_time");

            List<SpcAlarmEvent> activeEvents = alarmEventMapper.selectList(queryWrapper);

            if (activeEvents.isEmpty()) {
                return;
            }

            Date now = new Date();
            String changeReason = buildChangeReason(oldIndicator, newIndicator);

            // 强制结束所有活跃的报警事件
            for (SpcAlarmEvent event : activeEvents) {
                forceEndAlarmEventOnConfigChange(event, now, changeReason);
            }

        } catch (Exception e) {
            // 不抛出异常，允许配置更新继续进行
        }
    }

    /**
     * 强制结束报警事件（配置变更）
     */
    private void forceEndAlarmEventOnConfigChange(SpcAlarmEvent event, Date endTime, String reason) {
        try {
            // 计算持续时间
            long durationMs = endTime.getTime() - event.getStartTime().getTime();
            int durationSeconds = (int) (durationMs / 1000);

            // 更新事件状态
            SpcAlarmEvent updateEvent = new SpcAlarmEvent();
            updateEvent.setId(event.getId());
            updateEvent.setEndTime(endTime);
            updateEvent.setDurationSeconds(durationSeconds);
            updateEvent.setStatus("RESOLVED");
            updateEvent.setHandledBy("SYSTEM_CONFIG_CHANGE");
            updateEvent.setHandleTime(endTime);
            updateEvent.setHandleNote(reason);
            updateEvent.setUpdateTime(endTime);

            // 如果没有结束值，使用最后值
            if (event.getEndValue() == null && event.getLastValue() != null) {
                updateEvent.setEndValue(event.getLastValue());
                updateEvent.setEndValueTime(event.getLastValueTime());
            }

            int updateCount = alarmEventMapper.updateById(updateEvent);

            if (updateCount > 0) {
                log.info("配置变更强制结束报警事件: segmentId={}, 原因={}",
                        event.getSegmentId(), reason);
            }

        } catch (Exception e) {
            log.error("强制结束报警事件失败: segmentId={}", event.getSegmentId(), e);
        }
    }

    /**
     * 构建变更原因说明
     */
    private String buildChangeReason(SpcPointMetadataDO oldIndicator, SpcPointMetadataDO newIndicator) {
        StringBuilder reason = new StringBuilder("指标配置变更：");

        if (!Objects.equals(oldIndicator.getMeasureCode(), newIndicator.getMeasureCode())) {
            reason.append("point[").append(oldIndicator.getMeasureCode())
                    .append("->").append(newIndicator.getMeasureCode()).append("] ");
        }

        if (!Objects.equals(oldIndicator.getEnableRealtimeAlarm(), newIndicator.getEnableRealtimeAlarm())) {
            reason.append("status[").append(oldIndicator.getEnableRealtimeAlarm())
                    .append("->").append(newIndicator.getEnableRealtimeAlarm()).append("] ");
        }

        // 添加限值变更信息
        appendLimitChange(reason, "USL", oldIndicator.getUslValue(), newIndicator.getUslValue());
        appendLimitChange(reason, "LSL", oldIndicator.getLslValue(), newIndicator.getLslValue());
        appendLimitChange(reason, "UCL", oldIndicator.getUclValue(), newIndicator.getUclValue());
        appendLimitChange(reason, "LCL", oldIndicator.getLclValue(), newIndicator.getLclValue());
        appendLimitChange(reason, "UWL", oldIndicator.getUwlValue(), newIndicator.getUwlValue());
        appendLimitChange(reason, "LWL", oldIndicator.getLwlValue(), newIndicator.getLwlValue());

        return reason.toString();
    }

    /**
     * 追加限值变更信息
     */
    private void appendLimitChange(StringBuilder reason, String limitName,
                                   BigDecimal oldValue, BigDecimal newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            reason.append(limitName).append("[").append(oldValue)
                    .append("->").append(newValue).append("] ");
        }
    }

    /**
     * 发送配置刷新通知（Redis Pub/Sub）
     *
     * @param type 刷新类型：ALL-全量刷新, SINGLE-单点刷新, INVALIDATE-清除缓存
     * @param facCode 工厂代码（单点刷新时使用）
     * @param point 点位（单点刷新时使用）
     */
    private void publishRefreshNotification(String type, String facCode, String point) {
        try {
            // 构建刷新消息（需要同时传递 facCode 和 point）
            SpcConfigRefreshListener.RefreshMessage message =
                    new SpcConfigRefreshListener.RefreshMessage(type, facCode, point);

            String messageJson = objectMapper.writeValueAsString(message);

            // 发送到 Redis Pub/Sub 频道
            redisTemplate.convertAndSend(SpcConfigRefreshListener.CHANNEL_NAME, messageJson);

            log.info("发送配置刷新通知: type={}, facCode={}, point={}, channel={}",
                    type, facCode, point, SpcConfigRefreshListener.CHANNEL_NAME);

        } catch (Exception e) {
            log.error("发送配置刷新通知失败: type={}, facCode={}, point={}", type, facCode, point, e);
            // 通知发送失败不影响主流程
        }
    }

    /**
     * 构建 Redis 缓存 Key：spc:indicator:${facCode}:${point}
     *
     * @param facCode 工厂代码
     * @param point 点位
     * @return Redis 缓存 key
     */
    private String buildRedisCacheKey(String facCode, String point) {
        if (facCode == null || facCode.isEmpty()) {
            // 兼容 facCode 为空的情况
            return "spc:indicator:" + point;
        }
        return facCode + ":spc:indicator:" + point;
    }

    /**
     * 处理采样策略配置：根据 periodLabel 自动计算并填充 periodS 和 windowSizeS
     *
     * @param samplingStrategies 采样策略列表
     */
    private void processSamplingStrategiesPeriodLabel(List<SamplingStrategyDTO> samplingStrategies) {
        if (samplingStrategies == null || samplingStrategies.isEmpty()) {
            return;
        }

        for (SamplingStrategyDTO strategy : samplingStrategies) {
            // 设置 windowType 默认值为 tumble
            if (strategy.getWindowType() == null || strategy.getWindowType().trim().isEmpty()) {
                strategy.setWindowType("tumble");
            }

            // 只处理 periodic 类型的策略
            if (!"periodic".equalsIgnoreCase(strategy.getStrategyType())) {
                continue;
            }

            String periodLabel = strategy.getPeriodLabel();
            if (periodLabel == null || periodLabel.trim().isEmpty()) {
                continue;
            }

            // 根据 periodLabel 计算秒数
            Integer periodSeconds = parsePeriodLabelToSeconds(periodLabel);
            if (periodSeconds != null) {
                // 设置 periodS
                strategy.setPeriodS(periodSeconds);

                // 对于 periodic 策略，windowSizeS 默认等于 periodS
                strategy.setWindowSizeS(periodSeconds);

                log.info("自动填充采样策略参数: periodLabel={}, periodS={}, windowSizeS={}, windowType={}",
                        periodLabel, periodSeconds, periodSeconds, strategy.getWindowType());
            }
        }
    }

    /**
     * 将 periodLabel 解析为秒数
     * 支持的格式：1m, 5m, 1h 等
     *
     * @param periodLabel 周期标签（如 "1m", "5m", "1h"）
     * @return 对应的秒数，如果格式不支持则返回 null
     */
    private Integer parsePeriodLabelToSeconds(String periodLabel) {
        if (periodLabel == null || periodLabel.trim().isEmpty()) {
            return null;
        }

        periodLabel = periodLabel.trim().toLowerCase();

        // 解析数字和单位
        try {
            // 匹配格式：数字 + 单位（如 "1m", "5m", "1h"）
            if (periodLabel.matches("^\\d+[mh]$")) {
                String numPart = periodLabel.substring(0, periodLabel.length() - 1);
                String unitPart = periodLabel.substring(periodLabel.length() - 1);

                int value = Integer.parseInt(numPart);

                switch (unitPart) {
                    case "m":  // 分钟
                        return value * 60;
                    case "h":  // 小时
                        return value * 3600;
                    default:
                        log.warn("不支持的 periodLabel 单位: {}", periodLabel);
                        return null;
                }
            } else {
                log.warn("不支持的 periodLabel 格式: {}，应为类似 '1m', '5m', '1h' 的格式", periodLabel);
                return null;
            }
        } catch (Exception e) {
            log.error("解析 periodLabel 失败: {}", periodLabel, e);
            return null;
        }
    }

    public static void main(String[] args) {
    }
}
