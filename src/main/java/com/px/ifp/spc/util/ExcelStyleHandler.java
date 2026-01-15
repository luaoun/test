package com.px.ifp.spc.util;

import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import org.apache.poi.ss.usermodel.*;

public class ExcelStyleHandler {


    /**
     * 创建表头样式
     * @return 表头样式
     */
    private static WriteCellStyle createHeaderStyle() {
        WriteCellStyle headerStyle = new WriteCellStyle();

        // 设置背景色 - 使用更柔和的蓝色
        headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        headerStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);

        // 设置字体
        WriteFont headerFont = new WriteFont();
        headerFont.setFontName("微软雅黑");
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setWriteFont(headerFont);

        // 设置边框
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);

        // 设置对齐方式
        headerStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 设置自动换行
        headerStyle.setWrapped(true);

        return headerStyle;
    }

    /**
     * 创建内容样式
     * @return 内容样式
     */
    private static WriteCellStyle createContentStyle() {
        WriteCellStyle contentStyle = new WriteCellStyle();

        // 设置字体
        WriteFont contentFont = new WriteFont();
        contentFont.setFontName("微软雅黑");
        contentFont.setFontHeightInPoints((short) 11);
        contentFont.setColor(IndexedColors.BLACK.getIndex());
        contentStyle.setWriteFont(contentFont);

        // 设置边框
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);
        contentStyle.setBorderBottom(BorderStyle.THIN);

        // 设置对齐方式
        contentStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 设置自动换行
        contentStyle.setWrapped(true);

        return contentStyle;
    }

    /**
     * 获取样式策略
     * @return 样式策略
     */
    public static HorizontalCellStyleStrategy getStyleStrategy() {
        return new HorizontalCellStyleStrategy(createHeaderStyle(), createContentStyle());
    }
}
