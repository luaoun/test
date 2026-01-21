package com.px.ifp.spc.web.publish;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.bean.common.PageResponse;
import com.px.ifp.common.utils.ObjectConvertUtil;
import com.px.ifp.common.web.BaseController;
import com.px.ifp.spc.dto.publish.request.*;
import com.px.ifp.spc.dto.publish.response.*;
import com.px.ifp.spc.dto.manager.request.QuerySpcAnalysisResultNotesDTO;
import com.px.ifp.spc.entity.SpcPointMetadataDO;
import com.px.ifp.spc.service.indicator.SpcPointMetaDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "SPC指标相关接口(发布)")
@RestController
@RequestMapping("/api/v1/publish/spcIndicator")
public class SpcIndicatorPublishController extends BaseController {

    @Autowired
    private SpcPointMetaDataService spcPointMetaDataService;

    @Operation(summary = "查询SPC指标列表(全厂)")
    @RequestMapping(value = "/queryList", method = RequestMethod.POST)
    public BaseResponseData<PageResponse<SpcAnalysisDTO>> queryList(@RequestBody QuerySpcIndicatorListReqDTO reqDTO){
        Page page = startPage(reqDTO);
        List<SpcAnalysisDTO> list = new ArrayList<>();
        return BaseResponseData.success(getData(list, page));
    }

    @Operation(summary = "根据systemCode和Point组合条件查询JobId。返回值 key= 入参字符串，value=对应的jobId")
    @RequestMapping(value = "/queryJobsBySystemCodeAndPoint", method = RequestMethod.POST)
    public BaseResponseData<Map<String,String>> queryJobsBySystemCodeAndPoint(@RequestParam("sysCodeAndPoints") List<String> sysCodeAndPointList){
        return BaseResponseData.success(spcPointMetaDataService.querySpcJobsBySystemCodeAndPoint(sysCodeAndPointList));
    }


    //TOTO：根据systemCode 和 point 查询指标分析结果
    @Operation(summary = "根据systemCode和Point组合条件查询JobId。返回值 key= 入参字符串，value=对应的jobId")
    @RequestMapping(value = "/querySpcAlarmDataBySystemCodeAndPoint", method = RequestMethod.POST)
    public BaseResponseData<List<SpcIndicatorDataSummaryDTO>> querySpcAlarmDataBySystemCodeAndPoint(@RequestBody QuerySpcIndicatorSummaryDTO querySpcIndicatorSummaryDTO){
        return BaseResponseData.success(spcPointMetaDataService.queryAnalysisResultsBySystemAndPoints(querySpcIndicatorSummaryDTO));
    }

    @Operation(summary = "查询告警数量")
    @RequestMapping(value = "/queryResultCount", method = RequestMethod.POST)
    public BaseResponseData<QuerySpcAnalysisResultCountRespDTO> queryResultCount(@RequestBody @Valid QuerySpcAnalysisResultCountReqDTO reqDTO){

        //通过pointList反查jobIdList
        if(!CollectionUtils.isEmpty(reqDTO.getPointList())){
            LambdaQueryWrapper<SpcPointMetadataDO> queryWapper =  new LambdaQueryWrapper<>();
            queryWapper.in(SpcPointMetadataDO::getMeasureCode,reqDTO.getPointList());
            List<SpcPointMetadataDO> spcIndicatorList = spcPointMetaDataService.list(queryWapper);
            //没有反查到jobIdList也没有指定jobIdList，则不查询告警数量
            if(CollectionUtils.isEmpty(spcIndicatorList) && CollectionUtil.isEmpty(reqDTO.getJobIdList()))
                return BaseResponseData.success(new QuerySpcAnalysisResultCountRespDTO());
            //jobId去重后按jobId列表查询，pointList置空不作为查询条件
            List<String> jobIdList = spcIndicatorList.stream().map(SpcPointMetadataDO::getJobId).distinct().collect(Collectors.toList());
            if(CollectionUtil.isNotEmpty(jobIdList)) {
                if(CollectionUtil.isEmpty(reqDTO.getJobIdList())){
                    reqDTO.setJobIdList(new ArrayList<>());
                }
                reqDTO.getJobIdList().addAll(jobIdList);//pointList反查的jobId合并到jobIdList入参数中

            }reqDTO.setPointList(null);//用完就清空，后面的查询用jboIdList查询
        }

        //jobIdList入参仍然为空，则查询报警数量
        if(CollectionUtil.isEmpty(reqDTO.getJobIdList()))
            return BaseResponseData.success(new QuerySpcAnalysisResultCountRespDTO());

        return BaseResponseData.success(spcPointMetaDataService.queryResultCount(reqDTO));
    }

    @Operation(summary = "查询SPC分析结果及标注信息(弃用接口)")
    @RequestMapping(value = "/querySpcAnalysisResultNoteListPage",method = RequestMethod.POST)
    @Deprecated
    public BaseResponseData<PageResponse<SpcAnalysisResultNoteDTO>> querySpcAnalysisResultNoteListPage(@RequestBody QuerySpcAnalysisResultNotesPageDTO reqDTO){
        Page page = startPage(reqDTO);
        QuerySpcAnalysisResultNotesDTO queryDTO = ObjectConvertUtil.convert(reqDTO,QuerySpcAnalysisResultNotesDTO.class);
        List<SpcAnalysisResultNoteDTO> list = spcPointMetaDataService.queryspcAnalysisResultNote(queryDTO);
        return BaseResponseData.success(getData(list,page));
    }

    @Operation(summary = "厂区分类-spc树")
    @PostMapping(value = "/querySpcIndicatorTree")
    public BaseResponseData<List<SpcIndicatorTreeDTO>> querySpcIndicatorTree(@RequestBody QuerySpcIndicatorTreeDTO reqDTO) {
        return BaseResponseData.success(spcPointMetaDataService.querySpcIndicatorTree(reqDTO));
    }

    @Operation(summary = "查询spc指标的实时数据报警信息")
    @RequestMapping(value = "/querySpcAlarmRealTime", method = RequestMethod.POST)
    public BaseResponseData<List<SpcAlarmRelTimeDTO>> querySpcAlarmRealTime(@RequestBody QuerySpcAlarmRealTimeDTO reqDTO){
        return BaseResponseData.success(spcPointMetaDataService.querySpcAlarmRealTime(reqDTO));
    }
}
