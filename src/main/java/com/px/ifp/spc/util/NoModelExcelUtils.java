package com.px.ifp.spc.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.px.ifp.spc.bo.NoModelExcelBO;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

public class NoModelExcelUtils {
    private static final Logger log = LoggerFactory.getLogger(NoModelExcelUtils.class);

    public static void exportExcel(HttpServletResponse response, NoModelExcelBO noModelExcelBO, String sheetName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // 设置文件名并编码为UTF-8
        String fileName = sheetName + "_" + DateFormatUtils.format(new Date(), "yyyyMMddHHmmss") + ".xlsx";
        try {
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ";" + "filename*=utf-8''" + fileName);
        try {
            EasyExcel.write(response.getOutputStream()).head(noModelExcelBO.getHead()).sheet("模板").doWrite(noModelExcelBO.getData());
        } catch (Exception e) {
            log.error("导出Excel异常{}", e.getMessage());
        }
    }


    public static void exportExcelManySheet(HttpServletResponse response, List<NoModelExcelBO> noModelExcelBOList, String excelName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // 设置文件名并编码为UTF-8
        String fileName = excelName + "_" + DateFormatUtils.format(new Date(), "yyyyMMddHHmmss") + ".xlsx";
        try {
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ";" + "filename*=utf-8''" + fileName);
        try {
            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).build();
            for (int i = 0; i < noModelExcelBOList.size(); i++) {
                NoModelExcelBO noModelExcelBO = noModelExcelBOList.get(i);
                WriteSheet writeSheet = EasyExcel.writerSheet(i, noModelExcelBO.getSheetName()).head(noModelExcelBO.getHead()).build();
                excelWriter.write(noModelExcelBO.getData(), writeSheet);
            }
            //千万别忘记关流,finish会帮忙关流
            excelWriter.finish();
        } catch (Exception e) {
            log.error("导出Excel异常{}", e.getMessage());
        }
    }
}
