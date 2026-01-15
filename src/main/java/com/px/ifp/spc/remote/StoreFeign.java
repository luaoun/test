package com.px.ifp.spc.remote;

import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.dto.dataquery.request.ScadaIndicatorDTO;
import com.px.ifp.common.dto.dataquery.response.DataResultDTO;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "${remoteService.store}", fallback = StoreFeinFallback.class)
@Headers("Content-Type: application/json")
public interface StoreFeign {

    @PostMapping("/api/v1/dataQuery/queryScadaIndicator")
    BaseResponseData<DataResultDTO> queryScadaIndicator(@RequestBody ScadaIndicatorDTO scadaIndicatorDTO);

    @PostMapping(value="/api/v1/dataQuery/queryLastScadaValue")
    public BaseResponseData<DataResultDTO> queryLastScadaValue(@RequestBody List<String> specIds);

}
