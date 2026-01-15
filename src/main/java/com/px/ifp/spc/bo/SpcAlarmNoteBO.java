package com.px.ifp.spc.bo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SpcAlarmNoteBO {
    //spcAlarmEvent.id
    private Long analysisId;
    //spcIndicator.jobIDd
    private String jobId;
    //spcNote.id
    private Long spcNoteId;
    //spcAlarmEvent 报警类型
    private String alarmType;
    //是否oos报警
    private boolean oos;
    //是否ooc报警
    private boolean ooc;
    //是否oow报警
    private boolean oow;
    //是否oo3报警
    private boolean oo3;
    //是否normal自定义批注
    private boolean normal;
    //记录的是报警事件的峰值(peakValue)
    private BigDecimal pointValue;
    //记录的是报警事件的峰值时间(peakValueTime)
    private Date eventTime;
    //报警事件段ID
    private String alarmSegmentId;
    //记录的是报警事件的开始时间
    private Date alarmStartTime;
    //记录的是报警事件的结束时间
    private Date alarmEndTime;
    //标记批注的更新时间
    private Date spcNoteUpadteTime;
    //标记批注内容
    private String content;
    //是否要在SPC曲线上做标记
    private String tag;

    public String getAlarmType(){
        if(oos)
            alarmType = "oos";
        else if(ooc)
            alarmType = "ooc";
        else if (oow)
            alarmType = "oow";
        else if(oo3)
            alarmType = "oo3";
        else if(normal)
            alarmType = "normal";
       return alarmType;
    }
}
