package com.px.ifp.spc.web.manage.analysis;

import cn.hutool.core.collection.CollectionUtil;
import com.github.pagehelper.Page;
import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.exception.BusinessException;
import com.px.ifp.common.utils.ObjectConvertUtil;
import com.px.ifp.common.web.BaseController;
import com.px.ifp.spc.dto.manager.request.QuerySpcAnalysisResultListReqDTO;
import com.px.ifp.spc.dto.manager.request.QuerySpcAnalysisResultNotesDTO;
import com.px.ifp.spc.dto.manager.response.QuerySpcAnalysisResultListRespDTO;
import com.px.ifp.spc.dto.manager.response.SpcAnalysisResultDTO;
import com.px.ifp.spc.dto.publish.request.QuerySpcAnalysisResultCountReqDTO;
import com.px.ifp.spc.dto.publish.response.QuerySpcAnalysisResultCountRespDTO;
import com.px.ifp.spc.dto.publish.response.SpcAnalysisResultNoteDTO;
import com.px.ifp.spc.entity.SpcPointMetadataDO;
import com.px.ifp.spc.mapper.SpcPointMetadataMapper;
import com.px.ifp.spc.service.indicator.SpcPointMetaDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "SPC数据分析相关接口(V2)")
@RestController
@RequestMapping("/api/v2/analysis")
public class SpcAnalysisController extends BaseController {

    @Autowired
    private SpcPointMetaDataService spcPointMetaDataService;

    @Autowired
    private SpcPointMetadataMapper spcPointMetaDataMapper;


    @Operation(summary = "查询SPC分析结果列表")
    @RequestMapping(value = "/querySpcAnalysisResultList", method = RequestMethod.POST)
    public BaseResponseData<QuerySpcAnalysisResultListRespDTO> querySpcAnalysisResultList(@RequestBody @Valid QuerySpcAnalysisResultListReqDTO reqDTO){
        if (CollectionUtil.isEmpty(reqDTO.getPointList()) && CollectionUtil.isEmpty(reqDTO.getJobIdList())){
            throw new BusinessException("作业编号和点位必须传一个");
        }
        //按jobId list 或者 point list 查询指标信息
        List<SpcPointMetadataDO> spcIndicatorDOList = null;
        if(reqDTO.getIgnoreFacCode()){
            //忽略厂区
            spcIndicatorDOList = spcPointMetaDataMapper.selectAllList(null,null,null,null,reqDTO.getPointList(),reqDTO.getJobIdList(),reqDTO.getConfigType());
        }else {
            //默认带上厂区条件
            spcIndicatorDOList = spcPointMetaDataMapper.selectList(reqDTO.getJobIdList(), reqDTO.getPointList());
        }
        if(CollectionUtils.isEmpty(spcIndicatorDOList))
            return BaseResponseData.success(new QuerySpcAnalysisResultListRespDTO());

        List<String> jobIdList = spcIndicatorDOList.stream().map(SpcPointMetadataDO::getJobId).collect(Collectors.toList());
        reqDTO.setJobIdList(jobIdList);
        //统计count
        QuerySpcAnalysisResultCountReqDTO resultCountReqDTO = ObjectConvertUtil.convert(reqDTO, QuerySpcAnalysisResultCountReqDTO.class);
        QuerySpcAnalysisResultCountRespDTO resultCountRespDTO = spcPointMetaDataService.queryResultCount(resultCountReqDTO);
        if (resultCountRespDTO == null){
            return BaseResponseData.success(null);
        }
        QuerySpcAnalysisResultListRespDTO respDTO = ObjectConvertUtil.convert(resultCountRespDTO, QuerySpcAnalysisResultListRespDTO.class);
        //查询列表
        Page page = startPage(reqDTO);
        List<SpcAnalysisResultDTO> list = spcPointMetaDataService.querySpcAnalysisResultList(reqDTO);
        respDTO.setPageResponse(getData(list, page));

        return BaseResponseData.success(respDTO);
    }

    @Operation(summary = "按指标编码查询SPC分析结果及标注信息")
    @RequestMapping(value = "/querySpcAnalysisResultNoteList",method = RequestMethod.POST)
    public BaseResponseData<List<SpcAnalysisResultNoteDTO>> querySpcAnalysisResultNoteList(@RequestBody QuerySpcAnalysisResultNotesDTO reqDTO){
        List<SpcAnalysisResultNoteDTO> list = spcPointMetaDataService.queryspcAnalysisResultNote(reqDTO);
        return BaseResponseData.success(list);
    }
}
