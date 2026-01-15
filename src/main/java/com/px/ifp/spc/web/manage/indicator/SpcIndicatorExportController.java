package com.px.ifp.spc.web.manage.indicator;

import cn.hutool.core.collection.CollectionUtil;
import com.github.pagehelper.Page;
import com.px.ifp.common.annotation.Log;
import com.px.ifp.common.enums.BusinessType;
import com.px.ifp.common.utils.ObjectConvertUtil;
import com.px.ifp.spc.bo.ExportSpcAnalysisResultExcelBO;
import com.px.ifp.spc.bo.NoModelExcelBO;
import com.px.ifp.spc.dto.manager.request.ExportSpcAnalysisResultExcelReqDTO;
import com.px.ifp.spc.dto.manager.request.QuerySpcAnalysisReqDTO;
import com.px.ifp.spc.dto.publish.request.QuerySpcIndicatorListReqDTO;
import com.px.ifp.spc.dto.publish.response.SpcAnalysisDTO;
import com.px.ifp.spc.service.impl.SpcAnalysisResultExportService;
import com.px.ifp.spc.util.NoModelExcelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "SPC指标配置数据导出相关接口")
@RestController
@RequestMapping("/api/v2/indicator/export")
public class SpcIndicatorExportController extends SpcPointMetaDataController {

    @Autowired
    protected SpcAnalysisResultExportService spcAnalysisResultExportService;

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
        spcAnalysisResultExportService.exportData(resp,response,"Spc分析结果列表");
    }

    @Operation(summary = "SPC曲线数据-导出")
    @RequestMapping(value = "/querySpcAnalysis/export", method = RequestMethod.POST)
    public void emsSpecExport(HttpServletResponse response,@RequestBody @Valid QuerySpcAnalysisReqDTO reqDTO) {
        List<NoModelExcelBO> noModelExcelBOList = spcPointMetaDataService.querySpcAnalysisExport(reqDTO);
        NoModelExcelUtils.exportExcelManySheet(response, noModelExcelBOList, "SPC-统计");
    }

}
