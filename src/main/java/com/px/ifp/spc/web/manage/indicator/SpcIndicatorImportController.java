package com.px.ifp.spc.web.manage.indicator;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.px.ifp.common.annotation.Log;
import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.enums.BusinessType;
import com.px.ifp.spc.bo.SpcIndicatorExcelBO;
import com.px.ifp.spc.dto.manager.response.GasImportDataRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "SPC指标配置数据导入相关接口")
@RestController
@RequestMapping("/api/v2/indicator/import")
public class SpcIndicatorImportController extends SpcPointMetaDataController {

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
}
