package com.px.ifp.spc.service.impl;

import com.px.ifp.spc.bo.ExportSpcAnalysisResultExcelBO;
import org.springframework.stereotype.Service;

@Service
public class SpcAnalysisResultExportService extends AbstractExcelExportServiceImpl<ExportSpcAnalysisResultExcelBO> {
    protected SpcAnalysisResultExportService() {
        super(ExportSpcAnalysisResultExcelBO.class);
    }
}
