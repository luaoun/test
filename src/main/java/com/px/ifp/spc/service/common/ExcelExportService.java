package com.px.ifp.spc.service.common;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface ExcelExportService<T> {
    /**
     * 导出Excel数据
     * @param data 要导出的数据列表
     * @param response HTTP响应对象
     * @param fileName 导出文件名
     */
    void exportData(List<T> data, HttpServletResponse response, String fileName);

    /**
     * 导出多个sheet页的数据
     * @param sheetDataMap 每个sheet页的数据，key为sheet名称，value为该sheet的数据列表
     * @param fileName 导出文件名
     * @param response HTTP响应对象
     */
    void exportMultipleSheets(Map<String, List<T>> sheetDataMap, String fileName, HttpServletResponse response) ;

    void exportGroupedSheets(List<T> data,
                             Function<T, String> groupBy,
                             Function<List<T>, String> sheetNameGenerator,
                             HttpServletResponse response,
                             String fileName);

    /**
     * 获取导出的实体类型
     * @return 实体类的Class对象
     */
    Class<T> getEntityClass();
}
