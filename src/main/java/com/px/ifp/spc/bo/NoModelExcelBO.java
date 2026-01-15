package com.px.ifp.spc.bo;

import lombok.Data;

import java.util.List;

@Data
public class NoModelExcelBO {
    List<List<String>> head;
    List<List<Object>> data;
    String sheetName;
}
