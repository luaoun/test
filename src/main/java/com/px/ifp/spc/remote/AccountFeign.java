package com.px.ifp.spc.remote;

import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.dto.account.request.QueryUserDTO;
import com.px.ifp.common.dto.account.request.UacUserIdListDTO;
import com.px.ifp.common.dto.account.response.UacUserDTO;
import com.px.ifp.common.dto.account.response.UserProfileDTO;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-23
 */
@FeignClient(name = "${remoteService.account}", fallback = AccountFeignFallback.class)
@Headers("Content-Type: application/json")
public interface AccountFeign {

    /**
     * 根据用户ID获取用户信息接口
     *
     * @param dto
     * @return
     */
    @PostMapping(value = "/api/v1/user/queryByUserIdList")
    BaseResponseData<List<UserProfileDTO>> queryByUserIdList(UacUserIdListDTO dto);

    @PostMapping(value = "/api/v1/user/queryByUserNameList")
    BaseResponseData<List<UacUserDTO>> queryByUserNameList(@RequestBody QueryUserDTO dto);

    @PostMapping(value = "/api/v1/user/queryByEmpNoList")
    public BaseResponseData<List<UserProfileDTO>> queryByEmpNoList(@RequestBody QueryUserDTO dto);

}

