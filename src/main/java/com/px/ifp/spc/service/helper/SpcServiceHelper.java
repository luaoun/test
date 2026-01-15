package com.px.ifp.spc.service.helper;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.constant.CommonConstants;
import com.px.ifp.common.dto.account.response.MeaDTO;
import com.px.ifp.common.dto.digitaltwins.response.SystemCategoryTreeDTO;
import com.px.ifp.common.exception.BusinessException;
import com.px.ifp.common.service.kafka.KafkaService;
import com.px.ifp.common.service.measure.MeaService;
import com.px.ifp.common.utils.ObjectConvertUtil;
import com.px.ifp.common.utils.StringUtils;
import com.px.ifp.common.utils.ThreadLocalUtils;
import com.px.ifp.spc.bo.AlarmBoundary;
import com.px.ifp.spc.bo.AlarmInterval;
import com.px.ifp.spc.bo.SpcAlarmNoteBO;
import com.px.ifp.spc.bo.CommonTimeStat;
import com.px.ifp.spc.bo.OcapFlowEventBO;
import com.px.ifp.spc.entity.SpcAlarmEvent;
import com.px.ifp.spc.entity.SpcAnalysisResult;
import com.px.ifp.spc.error.OperationError;
import com.px.ifp.spc.remote.BaseFeign;
import com.px.ifp.spc.remote.dto.QuerySystemCategoryDTO;
import com.px.ifp.spc.util.NumberUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RefreshScope
public class SpcServiceHelper {
    @Value("${spring.kafka.spc.topics}")
    private String topics;

    @Value("${spc.enableOcapFlow}")
    private Boolean enableOcapFlow;

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private BaseFeign digitaltwinsFeign;

    @Autowired
    private MeaService meaService;

    public List<String> getOcapFlowTopics(){
        if(Objects.nonNull(topics))
            return Arrays.asList(topics.split(","));
        return CollectionUtil.empty(String.class);
    }

    public static List<CommonTimeStat> mergeAnalysisResultAndScada(Date startTime, Date endTime, List<SpcAlarmEvent> analysisResultDOList, List<CommonTimeStat> scadaDataList){
        if(CollectionUtil.isEmpty(analysisResultDOList))
            return scadaDataList;
        Map<Long,CommonTimeStat> mergePeekValueCommonTimeStatMap = new HashMap<>();
        Map<Long,CommonTimeStat> mergeEndValueCommonTimeStatMap = new HashMap<>();
        for(SpcAlarmEvent spcAlarmEvent:analysisResultDOList){
            //补充峰值数据
            if(Objects.nonNull(spcAlarmEvent.getPeekValueTime()) && Objects.nonNull(spcAlarmEvent.getPeekValueTime())) {

                Date peekValueTime = spcAlarmEvent.getPeekValueTime();
                if (peekValueTime.compareTo(startTime) < 0 || peekValueTime.compareTo(endTime) > 0) {
                   // peekValueTime 不在 startTime 和 endTime 之间,不需要补数据
                    continue;//跳过后续处理
                }
                CommonTimeStat newPeekCommonTimeStat = new CommonTimeStat("", peekValueTime.getTime(), spcAlarmEvent.getPeekValue());
                newPeekCommonTimeStat.setPoint(spcAlarmEvent.getPoint());
                //峰值全加到 spc曲线上
                if(mergePeekValueCommonTimeStatMap.get(peekValueTime.getTime()) == null)
                     mergePeekValueCommonTimeStatMap.put(peekValueTime.getTime(),newPeekCommonTimeStat);
                else {
                    CommonTimeStat oldPeekCommonTimeStat = mergePeekValueCommonTimeStatMap.get(peekValueTime.getTime());
                    if(oldPeekCommonTimeStat.getValue() != null && newPeekCommonTimeStat.getValue() != null){
                        int result = newPeekCommonTimeStat.getValue().compareTo(oldPeekCommonTimeStat.getValue());
                        if(result > 0){
                            mergePeekValueCommonTimeStatMap.put(peekValueTime.getTime(),newPeekCommonTimeStat);
                        }
                    }
                }
            }
            //补充事件结束的最后一个值（是一个正常值，这个正常值结束了整个报警事件）
            if(Objects.nonNull(spcAlarmEvent.getEndValueTime()) && Objects.nonNull(spcAlarmEvent.getEndValueTime())) {

                Date endValueTime = spcAlarmEvent.getEndValueTime();
                if (endValueTime.compareTo(startTime) < 0 || endValueTime.compareTo(endTime) > 0) {
                    // endValueTime 不在 startTime 和 endTime 之间,不需要补数据
                    continue;//跳过后续处理
                }
                CommonTimeStat newEndCommonTimeStat = new CommonTimeStat("", endValueTime.getTime(), spcAlarmEvent.getEndValue());
                newEndCommonTimeStat.setPoint(spcAlarmEvent.getPoint());
                //事件结束的最后一个值 加到spc曲线上，为了和后面的异常事件曲线对比
                if(mergeEndValueCommonTimeStatMap.get(endValueTime.getTime()) == null)
                    mergeEndValueCommonTimeStatMap.put(endValueTime.getTime(),newEndCommonTimeStat);
                else {
                    CommonTimeStat oldEndCommonTimeStat = mergeEndValueCommonTimeStatMap.get(endValueTime.getTime());
                    if(oldEndCommonTimeStat.getValue() != null && newEndCommonTimeStat.getValue() != null){
                        int result = newEndCommonTimeStat.getValue().compareTo(oldEndCommonTimeStat.getValue());
                        if(result > 0){
                            mergeEndValueCommonTimeStatMap.put(endValueTime.getTime(),newEndCommonTimeStat);
                        }
                    }
                }
            }
        }
        // 替换已存在的时间轴数据
        for (int i = 0; i < scadaDataList.size(); i++) {
            CommonTimeStat commonTimeStat = scadaDataList.get(i);
            //当前时间轴存在 异常峰值，则替换成异常峰值
            if (commonTimeStat != null && commonTimeStat.getTime() != null) {
                CommonTimeStat newCommonTimeStat = mergePeekValueCommonTimeStatMap.get(commonTimeStat.getTime());
                if (newCommonTimeStat != null) {
                    scadaDataList.set(i, newCommonTimeStat);
                    // 把有重复的替换掉后，从map里移除已处理的报警事件的时间轴。不存在的要按新增处理
                    mergePeekValueCommonTimeStatMap.remove(commonTimeStat.getTime());
                }
            }
        }
        // 新增不存在的报警时间轴
        for (CommonTimeStat newCommonTimeStat : mergePeekValueCommonTimeStatMap.values()) {
            scadaDataList.add(newCommonTimeStat);
        }

        //补充最后一个正常值（可能数仓会丢失这个正常值，要标记到Spc曲线上做对比）
        for (CommonTimeStat newCommonTimeStat : mergeEndValueCommonTimeStatMap.values()) {
            boolean find = false;
            for(CommonTimeStat scadaCommonTimeStat:scadaDataList){
                // 判断spc曲线上 EndValue（正常结束值）的时间不存在，则添加，存在则不添加
                if(scadaCommonTimeStat.getTime().equals(newCommonTimeStat.getTime())) {
                    find = true;
                    break;
                }
            }
            if(!find)
                scadaDataList.add(newCommonTimeStat);
        }
        Collections.sort(scadaDataList, new Comparator<CommonTimeStat>() {
            @Override
            public int compare(CommonTimeStat u1, CommonTimeStat u2) {
                return u1.getTime().compareTo(u2.getTime());
            }
        });
        return scadaDataList;
    }

    public  void mergeAlarmAndScada(List<SpcAlarmNoteBO> alarmDataList, List<CommonTimeStat> scadaDataList) {

        List<SpcAlarmNoteBO> result = new ArrayList<>();

        for (SpcAlarmNoteBO alarm : alarmDataList) {
            // 查找匹配的 SCADA 数据
            List<CommonTimeStat> matchingScada = scadaDataList.stream().filter(scada->scada.getTime().equals(alarm.getEventTime().getTime())).collect(Collectors.toList());

            // 如果找到匹配的 SCADA 数据，则更新报警数据
            if(!matchingScada.isEmpty())

                for (CommonTimeStat matchedScadaData:matchingScada) {
                    if(Objects.isNull(matchedScadaData.getSpcAlarmNote())) {
                        matchedScadaData.setSpcAlarmNote(alarm);
                        matchedScadaData.setTag(true);
                    } else {
                        // 2 比较时间 新增加的alarmNote对象updatetime比已经关联的新，则替换
                        if(matchedScadaData.getSpcAlarmNote().getSpcNoteUpadteTime().before(alarm.getSpcNoteUpadteTime()))
                            matchedScadaData.setSpcAlarmNote(alarm);
                            matchedScadaData.setTag(true);
                    }
                }
        }
    }

    public void startOcapFlow(String ocapFlowName, SpcAnalysisResult data){
        List<String> spcOcapTopics = getOcapFlowTopics();
        if(spcOcapTopics.isEmpty() || !enableOcapFlow || Objects.isNull(data) || StringUtils.isEmpty(ocapFlowName))
            return;

        if(CollectionUtil.isEmpty(spcOcapTopics))
            return;

        String spcOcapTopic = spcOcapTopics.get(0);//目前在FFP的消息集群中只配置了一个spcOcap topic
        //
        List<MeaDTO> measureList = meaService.getListByMeasureCodes(Arrays.asList(data.getPoint()));

        String measureCode=data.getPoint(),pointCode=data.getPoint();
        //从指标库获取指标编码 、点位编码信息
        if(CollectionUtil.isNotEmpty(measureList)){
            MeaDTO meaDTO = measureList.get(0);
            if(Objects.nonNull(meaDTO)){
                measureCode = meaDTO.getMeasureCode();
                pointCode = meaDTO.getTagCode();
            }
        }
        String descPattern = "【报警SPC指标名称】：%s,【报警指标编码】：%s,【报警点位编码】：%s,【报警类型】：%s,【报警值】：%s,【目标值】：%s";
        String alarmType = "";
        if(data.getOos())
            alarmType= "oos";
        else if(data.getOow())
            alarmType = "oow";
        else if(data.getOoc())
            alarmType = "ooc";
        String spcDesc = String.format(descPattern,data.getIndicatorName(),measureCode,pointCode,alarmType,data.getPointValue(),data.getTargetValue());
        //
        OcapFlowEventBO ocapFlowEventDTO = new OcapFlowEventBO();
        ocapFlowEventDTO.setFlowName(ocapFlowName);
        ocapFlowEventDTO.setSpcDesc(spcDesc);
        ocapFlowEventDTO.setFacCode(data.getFacCode());

        String sendMsg = JSONObject.toJSONString(ocapFlowEventDTO);
        kafkaService.sendMessage(spcOcapTopic,String.valueOf(data.getId()), sendMsg);
        log.debug("Spc Ocap Flow Start Event Message:{}", sendMsg);

    }



    private List<SystemCategoryTreeDTO> findChildrenTree(SystemCategoryTreeDTO parent, Map<String,SystemCategoryTreeDTO> sysCodeInClassMap) {
        List<SystemCategoryTreeDTO> children = new ArrayList<>();
        for (SystemCategoryTreeDTO systemCategory : sysCodeInClassMap.values()) {
            if (systemCategory.getParentId().equals(parent.getId())) {
                children.add(systemCategory);
                children.addAll(findChildrenTree(systemCategory,sysCodeInClassMap));
            }
        }
        return children;
    }

    public void matchSystemCategory(String classCode,List<String> pageQuerySysCodeList) {
        Map<String,String> sysCodeMap = new HashMap<>();

        //查询条件中含有子系统，则需要把下级子系统都查出来
        if(CollectionUtil.isNotEmpty(pageQuerySysCodeList)) {
            List<SystemCategoryTreeDTO> systemCategoryTreeRootList = getSystemCategoryTreeDTOS(classCode);
            //递归获取子系统的Name和Code
            if(CollectionUtil.isNotEmpty(systemCategoryTreeRootList)){
                List<SystemCategoryTreeDTO> matchingSystems = systemCategoryTreeRootList.stream()
//                        .filter(factory -> factory != null && "factoryArea".equals(factory.getType()))
//                        .flatMap(factory -> factory.getChildren() != null
//                                ? factory.getChildren().stream()
//                                : Stream.empty())
                        .filter(classCategory -> classCategory != null && "className".equals(classCategory.getType()))
                        .flatMap(classCategory -> classCategory.getChildren() != null
                                ? classCategory.getChildren().stream()
                                : Stream.empty())
                        .filter(system -> system != null
                                && "systemCategory".equals(system.getType())
                                && system.getCode() != null
                                && pageQuerySysCodeList.contains(system.getCode()))
                        .collect(Collectors.toList());

                List<SystemCategoryTreeDTO> result = new ArrayList<>();

                for (SystemCategoryTreeDTO system : matchingSystems) {
                    SystemCategoryTreeDTO copySystemCategoryTreeDTO = ObjectConvertUtil.convert(system, SystemCategoryTreeDTO.class);
                    result.add(copySystemCategoryTreeDTO);
                    collectAllDescendants(system, result);
                }
                //匹配结果加到请求参数里返回
                if(CollectionUtil.isNotEmpty(result)) {
                    result.stream().map(SystemCategoryTreeDTO::getCode)
                            .filter(sysTreeDTO-> ! pageQuerySysCodeList.contains(sysTreeDTO))
                            .forEach(pageQuerySysCodeList::add);
                }
            }
        }
    }

    private List<SystemCategoryTreeDTO> getSystemCategoryTreeDTOS(String classCode) {
        QuerySystemCategoryDTO querySystemCategoryDTO =  new QuerySystemCategoryDTO();
        querySystemCategoryDTO.setClassName(classCode);
        String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);
        querySystemCategoryDTO.setFactoryArea(facCode);
        //查询科室下的全部子系统Tree
        BaseResponseData<List<SystemCategoryTreeDTO>> responseData = digitaltwinsFeign.querySystemModelCondition(querySystemCategoryDTO);
        List<SystemCategoryTreeDTO> systemCategoryTreeRootList = responseData.getData();
        return systemCategoryTreeRootList;
    }


    private  void collectAllDescendants(SystemCategoryTreeDTO parent, List<SystemCategoryTreeDTO> result) {
        if (parent == null || parent.getChildren() == null) {
            return;
        }
        for (SystemCategoryTreeDTO child : parent.getChildren()) {
            SystemCategoryTreeDTO copySystemCategoryTreeDTO = ObjectConvertUtil.convert(child, SystemCategoryTreeDTO.class);
            result.add(copySystemCategoryTreeDTO);
            collectAllDescendants(child, result);
        }
    }


    public static int getCalendarField(String timePeriod) {
        int calendarField = Calendar.HOUR;
        switch (timePeriod) {
            case "minute":
            case "minutes":
                calendarField = Calendar.MINUTE;
                break;
            case "hour":
            case "hours":
                calendarField = Calendar.HOUR;
                break;
            case "day":
            case "days":
                calendarField = Calendar.DAY_OF_MONTH;
                break;
            case "week":
            case "weeks":
                calendarField = Calendar.WEEK_OF_YEAR;
                break;
            case "month":
            case "months":
                calendarField = Calendar.MONTH;
                break;
            case "year":
            case "years":
                throw new BusinessException(OperationError.PARAM_ERROR.getErrorCode(), "不支持按年跨度查询");
            default:
                throw new BusinessException(OperationError.PARAM_ERROR.getErrorCode(), "不支持的时间跨度: " + timePeriod);

        }
        return calendarField;
    }

    public List<CommonTimeStat> enrichSpcCurveData(List<SpcAlarmEvent> spcAlarmEvents,List<SpcAlarmNoteBO> spcAlarmNoteList, List<CommonTimeStat> scadaDataStatList, SpcTimeAlignmentStrategy alignmentStrategy){
        // spc曲线上的报警边界范围
        Map<Long, List<AlarmBoundary>> alarmBoundaryMap = buildAlarmBoundaryMap(spcAlarmEvents, alignmentStrategy);
        // 当前spc指标被标记的注释信息
        Map<Long, SpcAlarmNoteBO> alarmNoteTimeMap = buildAlarmNoteTimeMap(spcAlarmNoteList,alignmentStrategy);
        // spc曲线上报警事件的所有峰值
        Map<Long, SpcAlarmEvent> peakTimeMap = buildPeakTimeMap(spcAlarmEvents,alignmentStrategy);
        // spc曲线上所有报警事件的结束值
        Map<Long, CommonTimeStat> endValueCandidates = buildEndValueCandidatesMap(alarmBoundaryMap, alignmentStrategy);

        Map<Long,CommonTimeStat> enrichedPointsMap = new HashMap<>();
        // 遍历曲线点
        for (CommonTimeStat scadaData : scadaDataStatList) {
            long timestamp = scadaData.getTime();

            // 将当前点的时间戳对齐,确保与报警边界点和峰值使用相同的粒度
            long alignedTimestamp = alignmentStrategy.align(timestamp);

            // 查找是否为报警边界点 (开始点或结束点)
            List<AlarmBoundary> boundaries = alarmBoundaryMap.get(alignedTimestamp);

            // 查找是否有报警注释
            SpcAlarmNoteBO alarmNote = alarmNoteTimeMap.get(alignedTimestamp);

            // 查找是否有峰值
            SpcAlarmEvent peakEvent = peakTimeMap.get(alignedTimestamp);

            // 填充&补充spc曲线数据
            CommonTimeStat  commonTimeStat = buildEnrichedPoint(scadaData, boundaries,alarmNote,peakEvent,null);

            // put会自动覆盖:多个SCADA点对齐到同一时间戳时,保留最后一个
            enrichedPointsMap.put(alignedTimestamp,commonTimeStat);

            // 标记: 该时间戳已有SCADA数据,从endValue候选中移除
            endValueCandidates.remove(alignedTimestamp);
        }

        // 如果时间点有峰值,不需要插入endValue
        for (Long peakTime : peakTimeMap.keySet()) {
            endValueCandidates.remove(peakTime);
        }

        // 此时endValueCandidates中剩下的就是真正需要插入的点
        for (Map.Entry<Long, CommonTimeStat> entry : endValueCandidates.entrySet()) {
            long alignedTime = entry.getKey();
            CommonTimeStat endCommonTimeStat = entry.getValue();
            CommonTimeStat scadaData = enrichedPointsMap.get(alignedTime);
            if(scadaData == null) continue;
            buildEnrichedPoint(
                    scadaData,
                    null,
                    null,
                    null,
                    endCommonTimeStat
            );
            enrichedPointsMap.put(alignedTime, scadaData);
        }

        List<CommonTimeStat> result = new ArrayList<>(enrichedPointsMap.values());
        result.sort(Comparator.comparing(CommonTimeStat::getTime));
        return result;
    }

    private Map<Long, CommonTimeStat> buildEndValueCandidatesMap(
            Map<Long, List<AlarmBoundary>> alarmBoundaryMap,
            SpcTimeAlignmentStrategy alignmentStrategy) {

        Map<Long, CommonTimeStat> candidates = new HashMap<>();

        // 遍历所有报警边界点
        for (Map.Entry<Long, List<AlarmBoundary>> entry : alarmBoundaryMap.entrySet()) {
            long alignedTime = entry.getKey();
            List<AlarmBoundary> boundaries = entry.getValue();

            // 检查是否有结束边界
            for (AlarmBoundary boundary : boundaries) {
                if (boundary.isEnd()) {
                    SpcAlarmEvent event = boundary.getAlarmEvent();

                    // 只收集有endValue且已结束的报警
                    if (event.getEndValue() != null && event.getEndTime() != null) {
                        candidates.put(alignedTime, new CommonTimeStat("",alignedTime,event.getPeekValue()));
                    }
                    // 每个时间点只处理一次
                    break;
                }
            }
        }

        return candidates;
    }

    private boolean shouldInsertEndValuePoint(
            long alignedTime,
            SpcAlarmEvent event,
            Set<Long> scadaTimestamps,
            Map<Long, SpcAlarmEvent> peakTimeMap) {

        // 条件1: 该时间点已有SCADA数据 → 不插入
        if (scadaTimestamps.contains(alignedTime)) {
            return false;
        }

        // 条件2: 该时间点有峰值 → 不插入
        if (peakTimeMap.containsKey(alignedTime)) {
            return false;
        }

        // 条件3: 报警事件没有endValue → 不插入
        if (event.getEndValue() == null) {
            return false;
        }

        // 条件4: 报警未结束 → 不插入
        if (event.getEndTime() == null) {
            return false;
        }

        return true;
    }

    private Map<Long, CommonTimeStat> buildEndTimeMap(List<SpcAlarmEvent> spcAlarmEvents, SpcTimeAlignmentStrategy alignmentStrategy) {

        if (spcAlarmEvents == null || spcAlarmEvents.isEmpty()) {
            return new HashMap<>();
        }

        Map<Long, CommonTimeStat> endMap = new HashMap<>();
        spcAlarmEvents.stream()
                .filter(event -> event.getEndTime() != null && event.getEndValueTime() != null)
                .forEach(event -> {
                    long alignedTime = event.getEndTime().getTime();
                    CommonTimeStat commonTimeStat = new CommonTimeStat("",alignedTime,event.getEndValue());
                    endMap.put(alignedTime, commonTimeStat);
                });

        return endMap;
    }

    private Map<Long, List<AlarmBoundary>> buildAlarmBoundaryMap(List<SpcAlarmEvent> alarmEvents, SpcTimeAlignmentStrategy alignmentStrategy) {

        if (alarmEvents == null || alarmEvents.isEmpty()) {
            return new HashMap<>();
        }

        Map<Long, List<AlarmBoundary>> boundaryMap = new HashMap<>();

        for (SpcAlarmEvent event : alarmEvents) {
            if (event.getStartTime() != null) {
                // 对齐开始时间
                long startTime = alignmentStrategy.align(event.getStartTime().getTime());

                // 判断是否为正在进行中的报警
                boolean isOngoing = (event.getEndTime() == null);

                // 对齐结束时间 (如果已结束)
                Long endTime = null;
                if (event.getEndTime() != null) {
                    endTime = alignmentStrategy.align(event.getEndTime().getTime());
                }

                // 对齐峰值时间
                Long peekTime = null;
                if(event.getPeekValueTime() != null){
                    peekTime = alignmentStrategy.align(event.getPeekValueTime().getTime());
                }

                // 如果开始和结束时间对齐后相同,只添加一个边界点,同时标记为开始和结束
                if (endTime != null && startTime == endTime) {
                    // 同一小时/天/周/月内的报警: 既是开始又是结束
                    boundaryMap.computeIfAbsent(startTime, k -> new ArrayList<>())
                            .add(new AlarmBoundary(event, true, true, false));
                } else {
                    // 分别添加开始点和结束点
                    boundaryMap.computeIfAbsent(startTime, k -> new ArrayList<>())
                            .add(new AlarmBoundary(event, true, false, isOngoing));

                    if (endTime != null) {
                        boundaryMap.computeIfAbsent(endTime, k -> new ArrayList<>())
                                .add(new AlarmBoundary(event, false, true, false));
                    }
                }
            }
        }

        return boundaryMap;
    }

    private Map<Long, SpcAlarmNoteBO> buildAlarmNoteTimeMap(List<SpcAlarmNoteBO> spcAlarmNoteList,SpcTimeAlignmentStrategy alignmentStrategy) {

        if (spcAlarmNoteList == null || spcAlarmNoteList.isEmpty()) {
            return new HashMap<>();
        }

        //从当前查询范围的 报警事件列表中，过滤出 被标记注释过的报警事件列表；
        Map<Long,SpcAlarmNoteBO>  peekAlarmNoteMap = spcAlarmNoteList.stream().collect(Collectors.toMap(SpcAlarmNoteBO::getAnalysisId,alarmNote->alarmNote));

        //按标记过的报警事件列表 组织峰值数据
        Map<Long, SpcAlarmNoteBO> peakMap = new HashMap<>();
        peekAlarmNoteMap.values().stream()
                .filter(event -> event.getEventTime() != null && event.getPointValue() != null)
                .forEach(event -> {
                    // 使用策略对齐时间
                    long alignedTime = alignmentStrategy.align(event.getEventTime().getTime());
                    peakMap.put(alignedTime, event);
                });

        return peakMap;
    }

    private Map<Long, SpcAlarmEvent> buildPeakTimeMap(List<SpcAlarmEvent> alarmEvents, SpcTimeAlignmentStrategy alignmentStrategy) {

        if (alarmEvents == null || alarmEvents.isEmpty()) {
            return new HashMap<>();
        }

        Map<Long, SpcAlarmEvent> peakMap = new HashMap<>();
        alarmEvents.stream()
                .filter(event -> event.getPeekValueTime() != null && event.getPeekValue() != null)
                .forEach(event -> {
                    // 使用策略对齐时间
                    long alignedTime = alignmentStrategy.align(event.getPeekValueTime().getTime());
                    peakMap.put(alignedTime, event);
                });

        return peakMap;
    }

    public List<AlarmInterval> buildAlarmIntervals(
            List<SpcAlarmNoteBO> spcAlarmNoteList,
            SpcTimeAlignmentStrategy alignmentStrategy) {

        if (spcAlarmNoteList == null || spcAlarmNoteList.isEmpty()) {
            return new ArrayList<>();
        }

        return spcAlarmNoteList.stream()
                .filter(event -> event.getAlarmStartTime() != null)  // 必须有开始时间
                .map(event -> AlarmInterval.builder()
                        .alarmEventId(event.getAnalysisId())
                        .segmentId(event.getAlarmSegmentId())
                        .alarmType(event.getAlarmType())
                        .startTime(event.getAlarmStartTime().getTime())
                        .endTime(event.getAlarmEndTime() != null ? event.getAlarmEndTime().getTime() : null)
                        .build())
                .collect(Collectors.toList());
    }


    private CommonTimeStat buildEnrichedPoint(CommonTimeStat scadaData, List<AlarmBoundary> boundaries, SpcAlarmNoteBO alarmNote, SpcAlarmEvent peakEvent, CommonTimeStat endCommonTimeStat) {
        CommonTimeStat.CommonTimeStatBuilder builder = CommonTimeStat.builder();
        if(scadaData == null) return null;
        builder.alias(scadaData.getAlias());
        builder.point(scadaData.getPoint());
        builder.time(scadaData.getTime());
        //spc曲线数据保留1位小数（2.6版本需求）
        builder.value(NumberUtil.autoAdjustByRatioAndScale(scadaData.getValue(),0,1));
        builder.formattedTime(scadaData.getFormattedTime());
        // 报警边界点信息
        if (boundaries != null && !boundaries.isEmpty()) {
            // 可能有多个报警事件在同一时间点开始或结束
            boolean isStart = false;
            boolean isEnd = false;
            boolean isOngoing = false;
            SpcAlarmEvent startAlarmEvent = null;  // 开始点报警事件 (取严重程度最高的)
            SpcAlarmEvent endAlarmEvent = null;    // 结束点报警事件 (取严重程度最高的)

            for (AlarmBoundary boundary : boundaries) {
                if (boundary.isStart()) {
                    isStart = true;
                    // 选择严重程度更高的开始报警
                    if (startAlarmEvent == null ||
                            getSeverityScore(boundary.getAlarmEvent()) > getSeverityScore(startAlarmEvent)) {
                        startAlarmEvent = boundary.getAlarmEvent();
                    }
                    // 如果是开始点且正在进行中
                    if (boundary.isOngoing()) {
                        isOngoing = true;
                    }
                }
                if (boundary.isEnd()) {
                    isEnd = true;
                    // 选择严重程度更高的结束报警
                    if (endAlarmEvent == null ||
                            getSeverityScore(boundary.getAlarmEvent()) > getSeverityScore(endAlarmEvent)) {
                        endAlarmEvent = boundary.getAlarmEvent();
                    }
                }
            }

            if(isStart)
                builder.isAlarmStart(isStart);
            if(isEnd)
                builder.isAlarmEnd(isEnd);

            // 设置报警批注信息: 优先使用开始点的报警事件，如果没有则使用结束点
            SpcAlarmEvent alarmEventForInfo = (startAlarmEvent != null) ? startAlarmEvent : endAlarmEvent;

            if (alarmEventForInfo != null) {
                // Do nothing
            }
        }

        // 报警批注信息
        if (alarmNote != null) {
            builder.spcAlarmNote(alarmNote);
            builder.tag(true);
        }

        //峰值数据
        if(peakEvent != null){
            builder.value(NumberUtil.autoAdjustByRatioAndScale(peakEvent.getPeekValue(),0,1));
        }

        //结束值
        if(endCommonTimeStat != null){
            //spc曲线数据保留1位小数（2.6版本需求）
            builder.value(NumberUtil.autoAdjustByRatioAndScale(endCommonTimeStat.getValue(),0,1));
        }

        //处理spc曲线上数据的精度（2.6版本需求）

        return builder.build();
    }

    private int getSeverityScore(SpcAlarmEvent event) {
        if (event == null || event.getAlarmType() == null) {
            return 0;
        }

        // 按报警类型评分
        switch (event.getAlarmType()) {
            case "OOS":  // Out of Specification (规格外) - 最严重
                return 3;
            case "OOC":  // Out of Control (控制外) - 中等严重
                return 2;
            case "OOW":  // Out of Warning (警告外) - 最轻
                return 1;
            default:
                return 0;
        }
    }


    public static SpcTimeAlignmentStrategy getTimeAlignmentStrategy(String timePeriod) {
        SpcTimeAlignmentStrategy timeAlignmentStrategy = SpcTimeAlignmentStrategy.DAY;
        switch (timePeriod) {
            case "minute":
            case "minutes":
                timeAlignmentStrategy = SpcTimeAlignmentStrategy.MINUTE;
                break;
            case "hour":
            case "hours":
                timeAlignmentStrategy = SpcTimeAlignmentStrategy.HOUR;
                break;
            case "day":
            case "days":
                timeAlignmentStrategy = SpcTimeAlignmentStrategy.DAY;
                break;
            case "week":
            case "weeks":
                timeAlignmentStrategy = SpcTimeAlignmentStrategy.WEEK;
                break;
            case "month":
            case "months":
                timeAlignmentStrategy = SpcTimeAlignmentStrategy.MONTH;
                break;
            case "year":
            case "years":
                throw new BusinessException(OperationError.PARAM_ERROR.getErrorCode(), "不支持按年跨度查询");
            default:
                throw new BusinessException(OperationError.PARAM_ERROR.getErrorCode(), "不支持的时间跨度: " + timePeriod);

        }
        return timeAlignmentStrategy;
    }
}
