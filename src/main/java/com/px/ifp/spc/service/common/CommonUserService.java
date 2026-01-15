package com.px.ifp.spc.service.common;

import com.px.ifp.common.dto.account.request.QueryUserDTO;
import com.px.ifp.common.dto.account.response.UacUserDTO;
import com.px.ifp.common.dto.account.response.UserProfileDTO;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface CommonUserService {


    Map<String, UserProfileDTO> queryByEmpNoList(List<String> empNoList);

    Map<String, UserProfileDTO> queryByAccountIdList(List<String> accountIdList);

    /**
     *
     * @param empNoList
     * @return key：员工工号，value：用户对象
     */
    Map<String, UserProfileDTO> mapByEmpNoList(List<String> empNoList);

    Map<String, UacUserDTO> queryByUserNameList(QueryUserDTO queryUserDTO);

    <T> void enrichUserNameWithRealName(List<T> list, Function<T, String> creatorExtractor, BiConsumer<T, String> creatorSetter);

}
