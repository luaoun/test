package com.px.ifp.spc.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.px.ifp.spc.service.common.ExcelExportService;
import com.px.ifp.spc.util.ExcelStyleHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class AbstractExcelExportServiceImpl<T> implements ExcelExportService<T> {
    private final Class<T> entityClass;

    protected AbstractExcelExportServiceImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public void exportData(List<T> data, HttpServletResponse response, String fileName) {
        try {
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
//            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName+".xlsx" + ";" + "filename*=utf-8''" + encodedFileName+".xlsx");

            // 写入Excel
            EasyExcel.write(response.getOutputStream(), entityClass)
                    .registerWriteHandler(ExcelStyleHandler.getStyleStrategy())
                    .sheet("Sheet1")
                    .doWrite(data);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }


    @Override
    public void exportMultipleSheets(Map<String, List<T>> sheetDataMap, String fileName, HttpServletResponse response) {
        try {
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
//            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName+".xlsx" + ";" + "filename*=utf-8''" + encodedFileName+".xlsx");


            // 创建Excel写入器
            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), entityClass)
                    .registerWriteHandler(ExcelStyleHandler.getStyleStrategy())
                    .build();

            // 为每个sheet写入数据
            for (Map.Entry<String, List<T>> entry : sheetDataMap.entrySet()) {
                String sheetName = entry.getKey();
                // 限制sheet名称长度，Excel最多支持31个字符
                if (sheetName.length() > 31) {
                    sheetName = sheetName.substring(0, 31);
                }
                WriteSheet writeSheet = EasyExcel.writerSheet(sheetName).build();
                excelWriter.write(entry.getValue(), writeSheet);
            }

            // 关闭写入器
            excelWriter.finish();
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    /**
     * 按指定条件分组并导出多个sheet页
     * @param data 要导出的数据列表
     * @param groupBy 分组条件
     * @param sheetNameGenerator 生成sheet名称的函数
     * @param fileName 导出文件名
     * @param response HTTP响应对象
     */
    @Override
    public void exportGroupedSheets(List<T> data,
                                    Function<T, String> groupBy,
                                    Function<List<T>, String> sheetNameGenerator,
                                    HttpServletResponse response,
                                    String fileName) {
        // 按指定条件分组
        Map<String, List<T>> groupedData = data.stream()
                .collect(java.util.stream.Collectors.groupingBy(item -> {
                    String key = groupBy.apply(item);
                    return key != null ? key : "未分类";
                }));

        // 生成sheet名称
        Map<String, List<T>> sheetDataMap = groupedData.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> sheetNameGenerator.apply(entry.getValue()),
                        Map.Entry::getValue
                ));

        // 导出多个sheet
        exportMultipleSheets(sheetDataMap, fileName, response);
    }

    @Override
    public Class<T> getEntityClass() {
        return entityClass;
    }
}
