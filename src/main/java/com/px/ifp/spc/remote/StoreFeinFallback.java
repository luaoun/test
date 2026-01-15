package com.px.ifp.spc.remote;

import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.dto.dataquery.request.ScadaIndicatorDTO;
import com.px.ifp.common.dto.dataquery.response.DataResultDTO;

import java.util.List;

public class StoreFeinFallback implements  StoreFeign{
    @Override
    public BaseResponseData<DataResultDTO> queryScadaIndicator(ScadaIndicatorDTO scadaIndicatorDTO) {
        return BaseResponseData.fail("001", "查询失败，请重试", null);
    }

    @Override
    public BaseResponseData<DataResultDTO> queryLastScadaValue(List<String> specIds) {
        return BaseResponseData.fail("001", "查询失败，请重试", null);
    }
}
