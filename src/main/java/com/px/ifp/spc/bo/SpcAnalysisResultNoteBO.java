package com.px.ifp.spc.bo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SpcAnalysisResultNoteBO {

    private Integer id;
    private String jobId;
    private String systemCode;
    private int indicatorId;
    private String indicatorLevel;
    private String indicatorName;
    private String point;
    private String pointUnit;
    private String classCode;
    private BigDecimal startValue;
    private BigDecimal step;
    private BigDecimal targetValue;
    private BigDecimal endValue;
    private BigDecimal uclValue;
    private BigDecimal lclValue;
    private BigDecimal uwlValue;
    private BigDecimal lwlValue;
    private BigDecimal uslValue;
    private BigDecimal lslValue;
    private BigDecimal u3lValue;
    private BigDecimal l3lValue;
    private Integer oos = 0;
    private Integer ooc = 0;
    private Integer oow = 0;
    private Integer oo3 = 0;
    private Integer normal = 0;
    private BigDecimal pointValue;
    private Date eventTime;
    private String content;
}
