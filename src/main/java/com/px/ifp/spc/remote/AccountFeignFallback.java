package com.px.ifp.spc.remote;

import cn.hutool.core.lang.tree.Tree;
import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.bean.common.PageResponse;
import com.px.ifp.common.dto.account.request.*;
import com.px.ifp.common.dto.account.response.UacUserDTO;
import com.px.ifp.common.dto.account.response.UserProfileDTO;
import com.px.ifp.common.dto.account.response.WeatherHourlyDTO;
import com.px.ifp.spc.remote.dto.*;
import com.px.ifp.spc.remote.dto.uac.UserDetailVO;
import com.px.ifp.spc.remote.dto.uac.UserListPageBO;
import com.px.ifp.spc.remote.dto.uac.UserPageVO;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-23
 */
@Component
public class AccountFeignFallback implements AccountFeign {


    @Override
    public BaseResponseData<List<UserProfileDTO>> queryByUserIdList(UacUserIdListDTO dto) {
        return BaseResponseData.fail("001", "查询失败", null);
    }

    @Override
    public BaseResponseData<List<UacUserDTO>> queryByUserNameList(QueryUserDTO dto) {
        return BaseResponseData.fail("001", "查询失败", null);
    }

    @Override
    public BaseResponseData<List<UserProfileDTO>> queryByEmpNoList(QueryUserDTO dto) {
        return BaseResponseData.fail("001", "查询失败", null);
    }
}
