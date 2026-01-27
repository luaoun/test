package com.px.ifp.spc.web.legacy;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.px.ifp.common.annotation.Log;
import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.bean.common.PageResponse;
import com.px.ifp.common.enums.BusinessType;
import com.px.ifp.common.exception.BusinessException;
import com.px.ifp.common.utils.ObjectConvertUtil;
import com.px.ifp.common.web.BaseController;
import com.px.ifp.spc.bo.ExportSpcAnalysisResultExcelBO;
import com.px.ifp.spc.bo.NoModelExcelBO;
import com.px.ifp.spc.bo.SpcIndicatorExcelBO;
import com.px.ifp.spc.dto.IdReqDTO;
import com.px.ifp.spc.dto.manager.request.*;
import com.px.ifp.spc.dto.manager.response.GasImportDataRespDTO;
import com.px.ifp.spc.dto.manager.response.QuerySpcAnalysisResultListRespDTO;
import com.px.ifp.spc.dto.manager.response.QuerySpcIndicatorDetailRespDTO;
import com.px.ifp.spc.dto.manager.response.SpcAnalysisResultDTO;
import com.px.ifp.spc.dto.publish.request.*;
import com.px.ifp.spc.dto.publish.response.*;
import com.px.ifp.spc.entity.SpcPointMetadataDO;
import com.px.ifp.spc.mapper.SpcPointMetadataMapper;
import com.px.ifp.spc.service.impl.SpcAnalysisResultExportService;
import com.px.ifp.spc.service.indicator.SpcPointMetaDataService;
import com.px.ifp.spc.util.NoModelExcelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-08
 */
@Tag(name = "SPC指标相关接口(V1-Legacy)")
@RestController
@RequestMapping("/api/v1/spcIndicator")
public class SpcIndicatorController extends BaseController {

    @Autowired
    private SpcPointMetaDataService spcPointMetaDataService;

    @Autowired
    private SpcPointMetadataMapper spcPointMetadataMapper;

    @Autowired
    private SpcAnalysisResultExportService spcAnalysisResultExportService;


    @Log(title = "SPC指标", businessType = BusinessType.INSERT)
    @Operation(summary = "新增SPC指标设定")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public BaseResponseData<Boolean> add(@RequestBody @Valid AddSpcPointMetaReqDTO reqDTO){
        return BaseResponseData.success(spcPointMetaDataService.add(reqDTO));
    }

    @Log(title = "SPC指标", businessType = BusinessType.UPDATE)
    @Operation(summary = "更新SPC指标设定")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public BaseResponseData<Boolean> update(@RequestBody @Valid UpdateSpcPointMetaReqDTO reqDTO){
        return BaseResponseData.success(spcPointMetaDataService.update(reqDTO));
    }

    @Log(title = "SPC指标", businessType = BusinessType.DELETE)
    @Operation(summary = "删除SPC指标设定")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public BaseResponseData<Boolean> delete(@RequestBody @Valid IdReqDTO reqDTO){
        return BaseResponseData.success(spcPointMetaDataService.delete(reqDTO));
    }

    @Operation(summary = "查询SPC指标详情")
    @RequestMapping(value = "/queryDetail", method = RequestMethod.POST)
    public BaseResponseData<QuerySpcIndicatorDetailRespDTO> queryDetail(@RequestBody @Valid IdReqDTO reqDTO){
        return BaseResponseData.success(spcPointMetaDataService.queryDetail(reqDTO));
    }

    @Operation(summary = "查询SPC指标详情")
    @RequestMapping(value = "/commonQueryDetail", method = RequestMethod.POST)
    public BaseResponseData<QuerySpcIndicatorDetailRespDTO> queryDetail(@RequestBody @Valid QuerySpcIndicatorDTO reqDTO){
        return BaseResponseData.success(spcPointMetaDataService.querySpcIndicatorDetail(reqDTO));
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

    @Operation(summary = "查询SPC指标列表")
    @RequestMapping(value = "/queryList", method = RequestMethod.POST)
    public BaseResponseData<PageResponse<SpcAnalysisDTO>> queryList(@RequestBody QuerySpcIndicatorListReqDTO reqDTO){
        Page page = startPage(reqDTO);
        List<SpcAnalysisDTO> list = spcPointMetaDataService.queryList(reqDTO);
        // querySpcCount = true 会去查询 zeone，必须指定时间范围，否则查询数据量太大
        if (reqDTO.getQuerySpcCount() != null && reqDTO.getQuerySpcCount() ) {
            if(Objects.isNull(reqDTO.getStartTime()) || Objects.isNull(reqDTO.getEndTime()))
                throw new BusinessException("PARAM_ERROR","未指定 开始时间 和 结束时间");
        }
        return BaseResponseData.success(getData(list, page));
    }

    @Operation(summary = "下载模板")
    @RequestMapping(value = "/downloadTemplate", method = RequestMethod.POST)
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        OutputStream outputStream = response.getOutputStream();
        try {
            this.setExcelResponseProp(response, "SPC指标模板");
            EasyExcel.write(outputStream, SpcIndicatorExcelBO.class)
                    .excelType(ExcelTypeEnum.XLSX).sheet("Sheet1").doWrite(new ArrayList<>());
        } finally {
            response.flushBuffer();
        }
    }

    @Log(title = "SPC指标批量导入", businessType = BusinessType.IMPORT)
    @RequestMapping(value = "/importData", method = RequestMethod.POST)
    @Operation(summary = "导入数据")
    public BaseResponseData<GasImportDataRespDTO> importData(@RequestParam("file") MultipartFile file) throws IOException {
        List<SpcIndicatorExcelBO> dtoList = importData(file.getInputStream(), SpcIndicatorExcelBO.class);
        spcPointMetaDataService.importData(dtoList);

        GasImportDataRespDTO respDTO = new GasImportDataRespDTO();
        respDTO.setSuccessCount(dtoList.size());
        return BaseResponseData.success(respDTO);
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
            }
            reqDTO.setPointList(null);//用完就清空，后面的查询用jboIdList查询
        }

        //jobIdList入参仍然为空，则查询报警数量
        if(CollectionUtil.isEmpty(reqDTO.getJobIdList()))
            return BaseResponseData.success(new QuerySpcAnalysisResultCountRespDTO());

        return BaseResponseData.success(spcPointMetaDataService.queryResultCount(reqDTO));
    }

    @Operation(summary = "查询SPC曲线")
    @RequestMapping(value = "/querySpcAnalysis", method = RequestMethod.POST)
    public BaseResponseData<List<SpcAnalysisDTO>> querySpcAnalysis(@RequestBody @Valid QuerySpcAnalysisReqDTO reqDTO){
        //设置截止时间为当前时间，确保SPC的数据是当前之前的，不存在未来时间的数据
        if(Objects.nonNull(reqDTO.getEndTime())){
            Date currentTime = new Date();
            if(reqDTO.getEndTime().after(currentTime)){
                reqDTO.setEndTime(currentTime);
            }
        }
        return BaseResponseData.success(spcPointMetaDataService.querySpcAnalysis(reqDTO));
    }

    @Operation(summary = "SPC曲线数据-导出")
    @RequestMapping(value = "/querySpcAnalysis/export", method = RequestMethod.POST)
    public void emsSpecExport(HttpServletResponse response,@RequestBody @Valid QuerySpcAnalysisReqDTO reqDTO) {
        List<NoModelExcelBO> noModelExcelBOList = spcPointMetaDataService.querySpcAnalysisExport(reqDTO);
        NoModelExcelUtils.exportExcelManySheet(response, noModelExcelBOList, "SPC-统计");
    }


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
            spcIndicatorDOList = spcPointMetadataMapper.selectAllList(null,null,null,null,reqDTO.getPointList(),reqDTO.getJobIdList(),reqDTO.getConfigType());
        }else {
            //默认带上厂区条件
            spcIndicatorDOList = spcPointMetadataMapper.selectList(reqDTO.getJobIdList(), reqDTO.getPointList());
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

    @Operation(summary = "refreshRedisCache")
    @RequestMapping(value = "/refreshRedisCache", method = RequestMethod.POST)
    public BaseResponseData<Boolean> refreshRedisCache(@RequestBody QuerySpcIndicatorListReqDTO reqDTO){
        spcPointMetaDataService.refreshRedisCache(reqDTO);
        return BaseResponseData.success(true);
    }

    @Operation(summary = "refreshAllRedisCache")
    @RequestMapping(value = "/refreshAllRedisCache",method = RequestMethod.GET)
    public BaseResponseData<Boolean> refreshAllRedisCache(){
        spcPointMetaDataService.refreshAllRedisCache();
        return BaseResponseData.success(true);
    }

    @Operation(summary = "查询SPC分析结果及标注信息(弃用接口)")
    @RequestMapping(value = "/querySpcAnalysisResultNoteListPage",method = RequestMethod.POST)
    @Deprecated
    public BaseResponseData<PageResponse<SpcAnalysisResultNoteDTO>> querySpcAnalysisResultNoteListPage(@RequestBody QuerySpcAnalysisResultNotesPageDTO reqDTO){
        Page page = startPage(reqDTO);
        QuerySpcAnalysisResultNotesDTO queryDTO = ObjectConvertUtil.convert(reqDTO, QuerySpcAnalysisResultNotesDTO.class);
        List<SpcAnalysisResultNoteDTO> list = spcPointMetaDataService.queryspcAnalysisResultNote(queryDTO);
        return BaseResponseData.success(getData(list, page));
    }

    @Operation(summary = "按指标编码查询SPC分析结果及标注信息")
    @RequestMapping(value = "/querySpcAnalysisResultNoteList",method = RequestMethod.POST)
    public BaseResponseData<List<SpcAnalysisResultNoteDTO>> querySpcAnalysisResultNoteList(@RequestBody QuerySpcAnalysisResultNotesDTO reqDTO){
        List<SpcAnalysisResultNoteDTO> list = spcPointMetaDataService.queryspcAnalysisResultNote(reqDTO);
        return BaseResponseData.success(list);
    }

    @Log(title = "导出spc分析查询结果", businessType = BusinessType.EXPORT)
    @Operation(summary = "Spc分析-导出分析结果列表excel")
    @PostMapping("/exportSpcAnalysisResult")
    public void exportSpcAnalysisResult(HttpServletResponse response, @Validated @RequestBody ExportSpcAnalysisResultExcelReqDTO reqDTO) {
        QuerySpcIndicatorListReqDTO querySpcIndicatorListReqDTO = ObjectConvertUtil.convert(reqDTO, QuerySpcIndicatorListReqDTO.class);
        Page page = startPage(querySpcIndicatorListReqDTO);
        page.setPageNum(1);
        page.setPageSize(Integer.MAX_VALUE);

        List<SpcAnalysisDTO> list = spcPointMetaDataService.queryList(querySpcIndicatorListReqDTO);
        List<ExportSpcAnalysisResultExcelBO> resp =  new ArrayList<>();
        if(CollectionUtil.isNotEmpty(list)) {
            resp = ObjectConvertUtil.convertList(list, ExportSpcAnalysisResultExcelBO.class);
        }
        spcAnalysisResultExportService.exportData(resp, response, "Spc分析结果列表");
    }

    @Operation(summary = "查询spc指标的实时数据报警信息")
    @RequestMapping(value = "/querySpcAlarmRealTime", method = RequestMethod.POST)
    public BaseResponseData<List<SpcAlarmRelTimeDTO>> querySpcAlarmRealTime(@RequestBody QuerySpcAlarmRealTimeDTO reqDTO){
        return BaseResponseData.success(spcPointMetaDataService.querySpcAlarmRealTime(reqDTO));
    }


    @Operation(summary = "厂区分类-spc树")
    @PostMapping(value = "/querySpcIndicatorTree")
    public BaseResponseData<List<SpcIndicatorTreeDTO>> querySpcIndicatorTree(@RequestBody QuerySpcIndicatorTreeDTO reqDTO) {
        return BaseResponseData.success(spcPointMetaDataService.querySpcIndicatorTree(reqDTO));
    }

    @Operation(summary = "更新spc指标单位（同步指标库的单位到对的spc指标编码配置中）")
    @GetMapping(value = "/refreshUnit")
    public BaseResponseData<Boolean> synchronizeSpcIndcatorUnit(){
        spcPointMetaDataService.synchronizeSpcIndcatorUnit();
        return BaseResponseData.success(Boolean.TRUE);
    }


    @Operation(summary = "更新spc状态（status = 0:禁用| 1:启用）")
    @PostMapping(value = "/updateStatus")
    public BaseResponseData<Boolean> updateStatus(@RequestBody SpcStatusChangeDTO statusChangeDTO){
        return BaseResponseData.success(spcPointMetaDataService.updateStatus(statusChangeDTO));
    }

}
