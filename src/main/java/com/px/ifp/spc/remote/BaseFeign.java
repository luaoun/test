package com.px.ifp.spc.remote;

import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.dto.digitaltwins.request.QuerySystemCategoryByCodesDTO;
import com.px.ifp.common.dto.digitaltwins.response.SystemCategoryDTO;
import com.px.ifp.common.dto.digitaltwins.response.SystemCategoryTreeDTO;
import com.px.ifp.spc.remote.dto.QuerySystemCategoryDTO;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-04-12
 * 数字孪生系统接口
 */
//@FeignClient(name = "${remoteService.digitalTwins}", fallback = DigitaltwinsFeignFallback.class)
@FeignClient(name = "${remoteService.base}", fallback = BaseFeignFallback.class)
@Headers("Content-Type: application/json")
public interface BaseFeign {
    /**
     * 系统分类-条件获取系统模型树
     * @param dto
     * @return
     */
    @PostMapping(value = "api/v1/systemCategory/querySystemModelCondition")
    public BaseResponseData<List<SystemCategoryTreeDTO>> querySystemModelCondition(QuerySystemCategoryDTO dto);

    @PostMapping(value = "/api/v1/systemCategory/querySystemByCodes")
    public BaseResponseData<List<SystemCategoryDTO>> queryByCodeList(@RequestBody QuerySystemCategoryByCodesDTO dto);

    @PostMapping(value = "/api/v1/systemCategory/queryByAllList")
    public BaseResponseData<List<SystemCategoryDTO>>queryByAllList();

}
