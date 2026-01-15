package com.px.ifp.spc.bo;

import lombok.Data;

import java.util.Date;

@Data
public class SpcAnalysisSimpleResultBO {

    private String jobId;
    private String point;
    private String pointValue;
    private Integer oos;
    private Integer oow;
    private Integer ooc;
    private Integer oo3;
    private Integer normal;
    private Date eventTime;

}
