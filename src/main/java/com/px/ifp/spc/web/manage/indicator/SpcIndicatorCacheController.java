package com.px.ifp.spc.web.manage.indicator;

import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.spc.dto.publish.request.QuerySpcIndicatorListReqDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "SPC指标缓存相关接口")
@RestController
@RequestMapping("/api/v2/indicator/cach")
public class SpcIndicatorCacheController extends SpcPointMetaDataController {

    @Operation(summary = "refreshRedisCache")
    @RequestMapping(value = "/refreshRedisCache", method = RequestMethod.POST)
    public BaseResponseData<Boolean> refreshRedisCache(@RequestBody QuerySpcIndicatorListReqDTO reqDTO){
        spcPointMetaDataService.refreshRedisCache(reqDTO);
        return BaseResponseData.success(true);
    }

    @Operation(summary = "refreshAllRedisCache")
    @RequestMapping(value = "/refreshAllRedisCache",method = RequestMethod.GET)
    public BaseResponseData<Boolean> refreshAllRedisCache(){
        spcPointMetaDataService.refreshAllRedisCache();
        return BaseResponseData.success(true);
    }

    @Operation(summary = "更新spc指标单位（同步指标库的单位到对的spc指标编码配置中）")
    @GetMapping(value = "/refreshUnit")
    public BaseResponseData<Boolean> synchronizeSpcIndcatorUnit(){
        spcPointMetaDataService.synchronizeSpcIndcatorUnit();
        return BaseResponseData.success(Boolean.TRUE);
    }
}
