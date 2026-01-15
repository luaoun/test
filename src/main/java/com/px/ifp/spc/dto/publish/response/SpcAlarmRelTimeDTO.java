package com.px.ifp.spc.dto.publish.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SpcAlarmRelTimeDTO {
    private String point;
    private BigDecimal pointValue;
    private Date ctime;
    private String alarmType;
}
