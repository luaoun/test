package com.px.ifp.spc.dto.publish.response;

import lombok.Data;

import java.util.List;

@Data
public class SpcIndicatorTreeDTO {
    private String label;
    private String facCode;
    private String jobId;
    private String systemCode;
    private String classCode;
    private String point;
    private String pointUnit;
    private List<SpcIndicatorTreeDTO> children;
}
