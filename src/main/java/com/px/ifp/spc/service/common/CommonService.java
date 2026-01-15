package com.px.ifp.spc.service.common;

import com.px.ifp.common.dto.dataquery.request.ScadaIndicatorDTO;
import com.px.ifp.common.dto.dataquery.response.DataResultDTO;
import com.px.ifp.spc.bo.CommonTimeStat;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-04-12
 */
public interface CommonService {
    String getThreadLocalFacCode();

    Map<String, List<CommonTimeStat>> queryScadaData(String[] points, String bucketUnit, String indicator, Date startTime, Date endTime);

    Map<String, List<CommonTimeStat>> queryScadaData(String[] points, String bucketUnit, Integer bucketWidth, String[] indicator, Date startTime, Date endTime);

    DataResultDTO queryScadaIndicator(ScadaIndicatorDTO scadaIndicatorDTO);
}
