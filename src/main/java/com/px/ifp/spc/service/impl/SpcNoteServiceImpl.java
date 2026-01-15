package com.px.ifp.spc.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.px.ifp.common.constant.CommonConstants;
import com.px.ifp.common.exception.BusinessException;
import com.px.ifp.common.utils.ObjectConvertUtil;
import com.px.ifp.common.utils.RequestHeaderUtil;
import com.px.ifp.common.utils.ThreadLocalUtils;
import com.px.ifp.spc.bo.AlarmSegment;
import com.px.ifp.spc.dto.manager.request.AddSpcNormalNoteReqDTO;
import com.px.ifp.spc.dto.manager.request.AddSpcNoteReqDTO;
import com.px.ifp.spc.dto.manager.request.QuerySpcNoteListReqDTO;
import com.px.ifp.spc.dto.manager.response.AddTagDTO;
import com.px.ifp.spc.dto.manager.response.LastSpcNoteDTO;
import com.px.ifp.spc.dto.publish.response.SpcAnalysisDTO;
import com.px.ifp.spc.dto.manager.response.SpcNoteDTO;
import com.px.ifp.spc.entity.LastSpcNoteDO;
import com.px.ifp.spc.entity.SpcAlarmEvent;
import com.px.ifp.spc.entity.SpcAnalysisResult;
import com.px.ifp.spc.entity.SpcNoteDO;
import com.px.ifp.spc.error.OperationError;
import com.px.ifp.spc.mapper.SpcNoteMapper;
import com.px.ifp.spc.service.alarm.SpcAlarmEventService;
import com.px.ifp.spc.service.analysis.SpcAnalysisResultService;
import com.px.ifp.spc.service.common.CommonUserService;
import com.px.ifp.spc.service.indicator.SpcPointMetaDataService;
import com.px.ifp.spc.service.note.SpcNoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-07-10
 */
@Service
@Slf4j
public class SpcNoteServiceImpl extends ServiceImpl<SpcNoteMapper, SpcNoteDO> implements SpcNoteService {
    @Autowired
    private SpcNoteMapper spcNoteMapper;

    @Autowired
    private SpcPointMetaDataService spcIndicatorService;

    @Autowired
    private SpcAnalysisResultService spcAnalysisResultService;

    @Autowired
    private CommonUserService commonUserService;

    @Autowired
    private SpcAlarmEventService alarmEventService;

    @Override
    @Transactional
    public Boolean add(AddSpcNoteReqDTO reqDTO) {
        //1. 按BusinessKey查询 spcAlarmEvent
        SpcAlarmEvent spcAlarmEvent = alarmEventService.getById(reqDTO.getBusinessKey());
        if(spcAlarmEvent == null)
            throw new BusinessException(OperationError.DATA_INVALID.getErrorCode(),"报警事件不存在");
        Long indicatorId = spcAlarmEvent.getIndicatorId();

        //2. 按IndicatorId 全部更新 tag = false
        LambdaUpdateWrapper<SpcNoteDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(SpcNoteDO::getTag, false);
        updateWrapper.eq(SpcNoteDO::getIndicatorId,indicatorId);
        updateWrapper.eq(SpcNoteDO::getFacCode,reqDTO.getFacCode());
        update(updateWrapper);

        //3. 添加新的spc标注，并标记新增的spc标注的 tag =true (按接口传参决定）
        SpcNoteDO spcNoteDO = ObjectConvertUtil.convert(reqDTO, SpcNoteDO.class);
        spcNoteDO.setCreator(RequestHeaderUtil.getCurrentAccount().getAccountName());
        spcNoteDO.setFacCode(reqDTO.getFacCode());
        return spcNoteMapper.insert(spcNoteDO) > 0;
    }

    @Override
    public boolean tag(AddTagDTO reqDTO){
        //1. 按BusinessKey（即：spc_alarm_event 表的ID） 查询最新的一条注释
        LambdaQueryWrapper<SpcNoteDO> querySpcNote = new LambdaQueryWrapper<>();
        querySpcNote.eq(SpcNoteDO::getBusinessKey,reqDTO.getId()).orderByDesc(SpcNoteDO::getUpdateTime).last("LIMIT 1");
        List<SpcNoteDO> list = list(querySpcNote);
        SpcNoteDO spcNoteDO = list.isEmpty() ? null : list.get(0);

        SpcAlarmEvent spcAlarmEvent = alarmEventService.getById(reqDTO.getId());
        if(spcAlarmEvent == null)
            throw new BusinessException(OperationError.DATA_INVALID.getErrorCode(),"报警事件不存在");

        Long indicatorId = spcAlarmEvent.getIndicatorId();

        //2. 没有就新增一个空的spcNote
        if(Objects.isNull(spcNoteDO)){
            SpcNoteDO newSpcNote = new SpcNoteDO();
            newSpcNote.setBusinessKey(String.valueOf(reqDTO.getId()));
            newSpcNote.setIndicatorId(indicatorId);
            Date currentDate = new Date();
            newSpcNote.setCreateTime(currentDate);
            newSpcNote.setUpdateTime(currentDate);
            newSpcNote.setContent("空");
            newSpcNote.setTag(reqDTO.getTag());
            newSpcNote.setFacCode(reqDTO.getFacCode());
            return save(newSpcNote);
        }
        spcNoteDO.setTag(reqDTO.getTag());

        //3. 修复indicatorId=null的数据，同时设置 tag = 0 （都不标记在spc曲线上）
        // 说明： indicatorId，新增字段数据为null，需要实现补数据逻辑
        LambdaUpdateWrapper<SpcNoteDO> updateFillIndicatorWrapper = new LambdaUpdateWrapper<>();
        updateFillIndicatorWrapper.eq(SpcNoteDO::getBusinessKey,String.valueOf(reqDTO.getId()));
        updateFillIndicatorWrapper.eq(SpcNoteDO::getFacCode,reqDTO.getFacCode());
        updateFillIndicatorWrapper.set(SpcNoteDO::getTag, false);
        updateFillIndicatorWrapper.set(SpcNoteDO::getIndicatorId, indicatorId);
        update(updateFillIndicatorWrapper);

        //4. 更新indicator的所有tag = false
        LambdaUpdateWrapper<SpcNoteDO> updateCancelTagWrapper = new LambdaUpdateWrapper<>();
        updateCancelTagWrapper.eq(SpcNoteDO::getIndicatorId, indicatorId);
        updateCancelTagWrapper.eq(SpcNoteDO::getFacCode,reqDTO.getFacCode());
        updateCancelTagWrapper.set(SpcNoteDO::getTag, false);
        update(updateCancelTagWrapper);

        //5. 最后设置当前的spcNote tag = true（即：每个indicatorID 只有一条spcNote记录的 tag = true
        return updateById(spcNoteDO);
    }

    @Override
    public List<SpcNoteDTO> queryList(QuerySpcNoteListReqDTO reqDTO) {
        List<SpcNoteDO> spcNoteDOList = spcNoteMapper.selectList(reqDTO);
        if (CollectionUtil.isEmpty(spcNoteDOList)){
            return Collections.emptyList();
        }
        commonUserService.enrichUserNameWithRealName(spcNoteDOList,SpcNoteDO::getCreator,SpcNoteDO::setCreator);
        return spcNoteDOList.stream().map(spcNoteDO ->
                ObjectConvertUtil.convert(spcNoteDO, SpcNoteDTO.class)).collect(Collectors.toList());
    }

    @Override
    public List<LastSpcNoteDTO> getLatestNoteByJobIds(List<String> jobIds){
        //jobIds是空则不查询
        if(Objects.isNull(jobIds) || jobIds.isEmpty())
            return Collections.emptyList();
        String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);
        List<LastSpcNoteDO> spcNoteDOList = spcNoteMapper.getLatestNoteByJobIds(jobIds,facCode);
        if(CollectionUtil.isEmpty(spcNoteDOList))
            return Collections.emptyList();
        List<LastSpcNoteDTO> list = new ArrayList<>();
        for (LastSpcNoteDO spcNoteDO:spcNoteDOList) {
            LastSpcNoteDTO dto = new LastSpcNoteDTO();
            dto.setJobId(spcNoteDO.getJobId());
            dto.setContent(spcNoteDO.getContent());
            list.add(dto);
        }
        return list;
    }

    @Override
    @Transactional
    public boolean addSpcNormalAnalysisRecord(AddSpcNormalNoteReqDTO dto){
        String jobId = dto.getJobId();
        BigDecimal pointValue = dto.getPointValue();
        String content = dto.getContent();
        Date ctime = dto.getCtime();//点位值对应的时间点
        Boolean tag = dto.getTag();

        SpcAnalysisDTO spcAnalysisDTO = spcIndicatorService.getDetailByJobId(jobId);
        if(Objects.isNull(spcAnalysisDTO))
            throw new BusinessException(OperationError.PARAM_ERROR.getErrorCode(),"没有查到spcJobId:"+dto.getJobId());
        boolean flag = false;
        String businessKey = Objects.nonNull(dto.getSpcNoteId())? String.valueOf(dto.getSpcNoteId()):null;//默认值
        //新增
        if(Objects.isNull(dto.getSpcNoteId())) {
            SpcAnalysisResult spcAnalysisResultDO = ObjectConvertUtil.convert(spcAnalysisDTO, SpcAnalysisResult.class);
            spcAnalysisResultDO.setId(null);//新建记录
            spcAnalysisResultDO.setPointValue(pointValue);
            spcAnalysisResultDO.setOoc(false);
            spcAnalysisResultDO.setOos(false);
            spcAnalysisResultDO.setOow(false);
            spcAnalysisResultDO.setOo3(false);
            spcAnalysisResultDO.setNormal(true);
            spcAnalysisResultDO.setEventTime(ctime);//标记值对应的ctime时间，而不是这个记录添加时间
            spcAnalysisResultDO.setFacCode(dto.getFacCode());
            flag = spcAnalysisResultService.save(spcAnalysisResultDO);

            // 创建新的报警段
            AlarmSegment segment = AlarmSegment.builder()
                    .segmentId(alarmEventService.generateSegmentId())
                    .startTime(spcAnalysisResultDO.getEventTime())
                    .alarmType("NORMAL")
                    .alarmDirection("NORMAL")
                    .severityLevel(0)
                    .maxSeverityLevel(0)
                    .pointCount(1)
                    .maxValue(spcAnalysisResultDO.getPointValue())
                    .maxValueTime(spcAnalysisResultDO.getEventTime())
                    .minValue(spcAnalysisResultDO.getPointValue())
                    .minValueTime(spcAnalysisResultDO.getEventTime())
                    .currentValue(spcAnalysisResultDO.getPointValue())
                    .lastUpdateTime(spcAnalysisResultDO.getEventTime())
                    .endValue(spcAnalysisResultDO.getPointValue())
                    .endValueTime(spcAnalysisResultDO.getEventTime())
                    .indicatorId(spcAnalysisResultDO.getIndicatorId())
                    .indicatorName(spcAnalysisResultDO.getIndicatorName())
                    .point(spcAnalysisResultDO.getPoint())
                    // 初始化峰值：根据报警方向设置初始峰值
                    .peekValue(spcAnalysisResultDO.getPointValue())
                    .peekTime(spcAnalysisResultDO.getEventTime())
                    .peekDirectionType("NORMAL")
                    .build();

            //创建并完成一个normal事件
            SpcAlarmEvent alarmEvent = alarmEventService.createAlarmEvent(segment,spcAnalysisResultDO);
            alarmEventService.completeAlarmEvent(segment,spcAnalysisResultDO);

            //spc analysis result添加失败 则返回
            if(!flag)  return false;
            //新建成功使用 新建的spc analysis result 的id
            // 注意：改成用spc_alarm_event的id
            businessKey = String.valueOf(alarmEvent.getId());
            //继续往spc note 里添加标记内容
            AddSpcNoteReqDTO addSpcNoteReqDTO = new AddSpcNoteReqDTO();
            addSpcNoteReqDTO.setContent(content);
            addSpcNoteReqDTO.setBusinessKey(businessKey);
            addSpcNoteReqDTO.setTag(tag);
            addSpcNoteReqDTO.setFacCode(dto.getFacCode());

            return add(addSpcNoteReqDTO);
        }

        //修改spcNote内容
        SpcNoteDO updateSpcNoteDO = new SpcNoteDO();
        updateSpcNoteDO.setId(dto.getSpcNoteId());
        updateSpcNoteDO.setTag(dto.getTag());
        updateSpcNoteDO.setUpdateTime(new Date());
        updateSpcNoteDO.setContent(dto.getContent());
        return updateById(updateSpcNoteDO);
    }
}
