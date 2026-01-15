package com.px.ifp.spc.web.legacy;

import com.px.ifp.common.annotation.Log;
import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.bean.common.PageResponse;
import com.px.ifp.common.enums.BusinessType;
import com.px.ifp.common.web.BaseController;
import com.px.ifp.spc.dto.IdReqDTO;
import com.px.ifp.spc.dto.manager.request.*;
import com.px.ifp.spc.dto.manager.response.GasImportDataRespDTO;
import com.px.ifp.spc.dto.manager.response.QuerySpcAnalysisResultListRespDTO;
import com.px.ifp.spc.dto.manager.response.QuerySpcIndicatorDetailRespDTO;
import com.px.ifp.spc.dto.publish.request.*;
import com.px.ifp.spc.dto.publish.response.*;
import com.px.ifp.spc.web.manage.analysis.SpcAnalysisController;
import com.px.ifp.spc.web.manage.chart.SpcCurveController;
import com.px.ifp.spc.web.manage.indicator.SpcIndicatorCacheController;
import com.px.ifp.spc.web.manage.indicator.SpcIndicatorExportController;
import com.px.ifp.spc.web.manage.indicator.SpcIndicatorImportController;
import com.px.ifp.spc.web.manage.indicator.SpcPointMetaDataController;
import com.px.ifp.spc.web.publish.SpcIndicatorPublishController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-08
 */
@Tag(name = "SPC指标相关接口")
@RestController
@RequestMapping("/api/v1/spcIndicator")
public class SpcIndicatorController extends BaseController {

    @Autowired
    private SpcIndicatorPublishController publishController;

    @Autowired
    private SpcPointMetaDataController spcIndicatorMgrController;

    @Autowired
    private SpcIndicatorCacheController spcIndicatorCacheController;

    @Autowired
    private SpcAnalysisController spcAnalysisController;

    @Autowired
    private SpcIndicatorExportController spcIndicatorExportController;

    @Autowired
    private SpcIndicatorImportController spcIndicatorImportController;

    @Autowired
    private SpcCurveController spcCurveController;



    @Log(title = "SPC指标", businessType = BusinessType.INSERT)
    @Operation(summary = "新增SPC指标设定")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public BaseResponseData<Boolean> add(@RequestBody @Valid AddSpcPointMetaReqDTO reqDTO){
        BaseResponseData<Boolean> resp = spcIndicatorMgrController.addPointMeta(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Log(title = "SPC指标", businessType = BusinessType.UPDATE)
    @Operation(summary = "更新SPC指标设定")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public BaseResponseData<Boolean> update(@RequestBody @Valid UpdateSpcPointMetaReqDTO reqDTO){
        BaseResponseData<Boolean> resp = spcIndicatorMgrController.updatePointMeta(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Log(title = "SPC指标", businessType = BusinessType.DELETE)
    @Operation(summary = "删除SPC指标设定")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public BaseResponseData<Boolean> delete(@RequestBody @Valid IdReqDTO reqDTO){
        BaseResponseData<Boolean> resp = spcIndicatorMgrController.deletePointMeta(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "查询SPC指标详情")
    @RequestMapping(value = "/queryDetail", method = RequestMethod.POST)
    public BaseResponseData<QuerySpcIndicatorDetailRespDTO> queryDetail(@RequestBody @Valid IdReqDTO reqDTO){
        BaseResponseData<QuerySpcIndicatorDetailRespDTO> resp = spcIndicatorMgrController.queryPointMetaDetail(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "查询SPC指标详情")
    @RequestMapping(value = "/commonQueryDetail", method = RequestMethod.POST)
    public BaseResponseData<QuerySpcIndicatorDetailRespDTO> queryDetail(@RequestBody @Valid QuerySpcIndicatorDTO reqDTO){
        BaseResponseData<QuerySpcIndicatorDetailRespDTO> resp = spcIndicatorMgrController.queryDetail(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "根据systemCode和Point组合条件查询JobId。返回值 key= 入参字符串，value=对应的jobId")
    @RequestMapping(value = "/queryJobsBySystemCodeAndPoint", method = RequestMethod.POST)
    public BaseResponseData<Map<String,String>> queryJobsBySystemCodeAndPoint(@RequestParam("sysCodeAndPoints") List<String> sysCodeAndPointList){
        BaseResponseData<Map<String, String>> resp = publishController.queryJobsBySystemCodeAndPoint(sysCodeAndPointList);
        return BaseResponseData.success(resp.getData());
    }


    //TOTO：根据systemCode 和 point 查询指标分析结果
    @Operation(summary = "根据systemCode和Point组合条件查询JobId。返回值 key= 入参字符串，value=对应的jobId")
    @RequestMapping(value = "/querySpcAlarmDataBySystemCodeAndPoint", method = RequestMethod.POST)
    public BaseResponseData<List<SpcIndicatorDataSummaryDTO>> querySpcAlarmDataBySystemCodeAndPoint(@RequestBody QuerySpcIndicatorSummaryDTO querySpcIndicatorSummaryDTO){
        BaseResponseData<List<SpcIndicatorDataSummaryDTO>> resp = publishController.querySpcAlarmDataBySystemCodeAndPoint(querySpcIndicatorSummaryDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "查询SPC指标列表")
    @RequestMapping(value = "/queryList", method = RequestMethod.POST)
    public BaseResponseData<PageResponse<SpcAnalysisDTO>> queryList(@RequestBody QuerySpcIndicatorListReqDTO reqDTO){
        BaseResponseData<PageResponse<SpcAnalysisDTO>> resp = spcIndicatorMgrController.queryPointMetaList(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "下载模板")
    @RequestMapping(value = "/downloadTemplate", method = RequestMethod.POST)
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        spcIndicatorImportController.downloadTemplate(response);
    }

    @Log(title = "SPC指标批量导入", businessType = BusinessType.IMPORT)
    @RequestMapping(value = "/importData", method = RequestMethod.POST)
    @Operation(summary = "导入数据")
    public BaseResponseData<GasImportDataRespDTO> importData(@RequestParam("file") MultipartFile file) throws IOException {
        BaseResponseData<GasImportDataRespDTO> resp = spcIndicatorImportController.importData(file);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "查询告警数量")
    @RequestMapping(value = "/queryResultCount", method = RequestMethod.POST)
    public BaseResponseData<QuerySpcAnalysisResultCountRespDTO> queryResultCount(@RequestBody @Valid QuerySpcAnalysisResultCountReqDTO reqDTO){
        BaseResponseData<QuerySpcAnalysisResultCountRespDTO> resp = publishController.queryResultCount(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "查询SPC曲线")
    @RequestMapping(value = "/querySpcAnalysis", method = RequestMethod.POST)
    public BaseResponseData<List<SpcAnalysisDTO>> querySpcAnalysis(@RequestBody @Valid QuerySpcAnalysisReqDTO reqDTO){
        BaseResponseData<List<SpcAnalysisDTO>> resp = spcCurveController.getSpcCurveData(reqDTO);
        return BaseResponseData.success(resp.getData());
    }
    @Operation(summary = "SPC曲线数据-导出")
    @RequestMapping(value = "/querySpcAnalysis/export", method = RequestMethod.POST)
    public void emsSpecExport(HttpServletResponse response,@RequestBody @Valid QuerySpcAnalysisReqDTO reqDTO) {
        spcIndicatorExportController.emsSpecExport(response,reqDTO);
    }


    @Operation(summary = "查询SPC分析结果列表")
    @RequestMapping(value = "/querySpcAnalysisResultList", method = RequestMethod.POST)
    public BaseResponseData<QuerySpcAnalysisResultListRespDTO> querySpcAnalysisResultList(@RequestBody @Valid QuerySpcAnalysisResultListReqDTO reqDTO){
        BaseResponseData<QuerySpcAnalysisResultListRespDTO> resp = spcAnalysisController.querySpcAnalysisResultList(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "refreshRedisCache")
    @RequestMapping(value = "/refreshRedisCache", method = RequestMethod.POST)
    public BaseResponseData<Boolean> refreshRedisCache(@RequestBody QuerySpcIndicatorListReqDTO reqDTO){
        BaseResponseData<Boolean> resp = spcIndicatorCacheController.refreshRedisCache(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "refreshAllRedisCache")
    @RequestMapping(value = "/refreshAllRedisCache",method = RequestMethod.GET)
    public BaseResponseData<Boolean> refreshAllRedisCache(){
        BaseResponseData<Boolean> resp = spcIndicatorCacheController.refreshAllRedisCache();
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "查询SPC分析结果及标注信息(弃用接口)")
    @RequestMapping(value = "/querySpcAnalysisResultNoteListPage",method = RequestMethod.POST)
    @Deprecated
    public BaseResponseData<PageResponse<SpcAnalysisResultNoteDTO>> querySpcAnalysisResultNoteListPage(@RequestBody QuerySpcAnalysisResultNotesPageDTO reqDTO){
        BaseResponseData<PageResponse<SpcAnalysisResultNoteDTO>> resp = publishController.querySpcAnalysisResultNoteListPage(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "按指标编码查询SPC分析结果及标注信息")
    @RequestMapping(value = "/querySpcAnalysisResultNoteList",method = RequestMethod.POST)
    public BaseResponseData<List<SpcAnalysisResultNoteDTO>> querySpcAnalysisResultNoteList(@RequestBody QuerySpcAnalysisResultNotesDTO reqDTO){
        BaseResponseData<List<SpcAnalysisResultNoteDTO>> resp = spcAnalysisController.querySpcAnalysisResultNoteList(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Log(title = "导出spc分析查询结果", businessType = BusinessType.EXPORT)
    @Operation(summary = "Spc分析-导出分析结果列表excel")
    @PostMapping("/exportSpcAnalysisResult")
    public void exportSpcAnalysisResult(HttpServletResponse response, @Validated @RequestBody ExportSpcAnalysisResultExcelReqDTO reqDTO) {
        spcIndicatorExportController.exportSpcAnalysisResult(response,reqDTO);
    }

    @Operation(summary = "查询spc指标的实时数据报警信息")
    @RequestMapping(value = "/querySpcAlarmRealTime", method = RequestMethod.POST)
    public BaseResponseData<List<SpcAlarmRelTimeDTO>> querySpcAlarmRealTime(@RequestBody QuerySpcAlarmRealTimeDTO reqDTO){
        BaseResponseData<List<SpcAlarmRelTimeDTO>> resp = publishController.querySpcAlarmRealTime(reqDTO);
        return BaseResponseData.success(resp.getData());
    }


    @Operation(summary = "厂区分类-spc树")
    @PostMapping(value = "/querySpcIndicatorTree")
    public BaseResponseData<List<SpcIndicatorTreeDTO>> querySpcIndicatorTree(@RequestBody QuerySpcIndicatorTreeDTO reqDTO) {
        BaseResponseData<List<SpcIndicatorTreeDTO>> resp = publishController.querySpcIndicatorTree(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "更新spc指标单位（同步指标库的单位到对的spc指标编码配置中）")
    @GetMapping(value = "/refreshUnit")
    public BaseResponseData<Boolean> synchronizeSpcIndcatorUnit(){
        BaseResponseData<Boolean> resp = spcIndicatorCacheController.synchronizeSpcIndcatorUnit();
        return BaseResponseData.success(resp.getData());
    }


    @Operation(summary = "更新spc状态（status = 0:禁用| 1:启用）")
    @PostMapping(value = "/updateStatus")
    public BaseResponseData<Boolean> updateStatus(@RequestBody SpcStatusChangeDTO statusChangeDTO){
        BaseResponseData<Boolean> resp = spcIndicatorMgrController.updateStatus(statusChangeDTO);
        return BaseResponseData.success(resp.getData());
    }

}
