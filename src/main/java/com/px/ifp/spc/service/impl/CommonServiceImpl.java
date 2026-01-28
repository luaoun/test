package com.px.ifp.spc.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.constant.CommonConstants;
import com.px.ifp.common.dto.dataquery.request.ScadaIndicatorDTO;
import com.px.ifp.common.dto.dataquery.response.DataResultDTO;
import com.px.ifp.common.enums.DataQueryContentType;
import com.px.ifp.common.utils.DateUtils;
import com.px.ifp.common.utils.ThreadLocalUtils;
import com.px.ifp.spc.bo.CommonTimeStat;
import com.px.ifp.spc.remote.StoreFeign;
import com.px.ifp.spc.service.common.CommonService;
import com.px.ifp.spc.util.ResponseDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-04-12
 */
@Service
@Slf4j
public class CommonServiceImpl implements CommonService {

    @Autowired
    private StoreFeign storeFeign;

    @Override
    public String getThreadLocalFacCode() {
        return (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);
    }

    @Override
    public Map<String, List<CommonTimeStat>> queryScadaData(String[] points, String bucketUnit, String indicator, Date startTime, Date endTime) {
        return queryScadaData(points,bucketUnit,1,new String[]{indicator},startTime,endTime);
    }

    @Override
    public Map<String, List<CommonTimeStat>> queryScadaData(String[] points, String bucketUnit, Integer bucketWidth,String[] indicators, Date startTime, Date endTime) {
        ScadaIndicatorDTO scadaIndicatorDTO = new ScadaIndicatorDTO();
        if(bucketUnit != null)
            scadaIndicatorDTO.setBucketUnit(bucketUnit);
        if(bucketWidth != null)
            scadaIndicatorDTO.setBucketWidth(bucketWidth);
        scadaIndicatorDTO.setStartTime(startTime);
        scadaIndicatorDTO.setEndTime(endTime);
        scadaIndicatorDTO.setPoint(points);
        scadaIndicatorDTO.setIndicators(indicators);
        scadaIndicatorDTO.setPageNum(1);
        scadaIndicatorDTO.setPageSize(Integer.MAX_VALUE);
        scadaIndicatorDTO.setContentType(DataQueryContentType.LIST);
        String facCode = (String) ThreadLocalUtils.get(CommonConstants.FACTORY_CODE);
        scadaIndicatorDTO.setFacCode(facCode);
        DataResultDTO dataResultDTO = queryScadaIndicator(scadaIndicatorDTO);
        if (dataResultDTO == null || CollectionUtil.isEmpty(dataResultDTO.getRows())) {
            return null;
        }
        Map<String, List<CommonTimeStat>> dataStatMap = MapUtil.newHashMap();
        for (String point : points) {
            dataStatMap.put(point, Lists.newArrayList());
        }
        for (Map<String, Object> stringObjectMap : dataResultDTO.getRows()) {
            long time = 0L;
            if (stringObjectMap.get(bucketUnit) != null) {
                time = DateUtils.parse(stringObjectMap.get(bucketUnit).toString()).getTime();
            }

            for(String indicator:indicators) {
                BigDecimal value = null;
                String aliasName = indicator.toLowerCase().concat("_value");
                if (stringObjectMap.get(aliasName) != null) {
                    value = new BigDecimal(stringObjectMap.get(aliasName).toString()).setScale(3, RoundingMode.HALF_UP);
                }
                String point = stringObjectMap.get("point").toString();
                if (CollectionUtil.isEmpty(dataStatMap.get(point))) {
                    CommonTimeStat commonTimeStat = new CommonTimeStat(aliasName,time, value);
                    dataStatMap.put(point, Lists.newArrayList(commonTimeStat));
                } else {
                    dataStatMap.get(point).add(new CommonTimeStat(aliasName,time, value));
                }
            }
        }
        return dataStatMap;
    }

    @Override
    public DataResultDTO queryScadaIndicator(ScadaIndicatorDTO scadaIndicatorDTO) {
        try {
            BaseResponseData<DataResultDTO> responseData = storeFeign.queryScadaIndicator(scadaIndicatorDTO);
            if (ResponseDataUtil.containsData(responseData)) {
                return responseData.getData();
            }
        } catch (Exception ex) {
            log.error("storeFeign.queryScadaIndicator exception. req:{}", JSON.toJSONString(scadaIndicatorDTO), ex);
            throw new RuntimeException(ex);
        }
        return null;
    }
}
