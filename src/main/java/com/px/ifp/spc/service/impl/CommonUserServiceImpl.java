package com.px.ifp.spc.service.impl;

import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.dto.account.request.QueryUserDTO;
import com.px.ifp.common.dto.account.request.UacUserIdListDTO;
import com.px.ifp.common.dto.account.response.UacUserDTO;
import com.px.ifp.common.dto.account.response.UserProfileDTO;
import com.px.ifp.spc.remote.AccountFeign;
import com.px.ifp.spc.service.common.CommonUserService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommonUserServiceImpl implements CommonUserService {

    @Autowired
    AccountFeign accountFeign;

    public Map<String, UserProfileDTO> queryByEmpNoList(List<String> empNoList) {
        QueryUserDTO queryUserDTO = new QueryUserDTO();
        queryUserDTO.setEmpNoList(empNoList);

        BaseResponseData<List<UserProfileDTO>> responseData = accountFeign.queryByEmpNoList(queryUserDTO);
        if (responseData == null || CollectionUtils.isEmpty(responseData.getData())) {
            return new HashMap<>();
        }
        return responseData.getData().stream().collect(Collectors.toMap(UserProfileDTO::getUsername, Function.identity(), (v1, v2) -> v1));
    }

    @Override
    public Map<String, UserProfileDTO> queryByAccountIdList(List<String> accountIdList) {
        if (CollectionUtils.isEmpty(accountIdList)) {
            return new HashMap<>();
        }

        UacUserIdListDTO uacUserIdListDTO = new UacUserIdListDTO();
        List<Long> userIdList = accountIdList.stream().map(Long::valueOf).collect(Collectors.toList());
        uacUserIdListDTO.setUserIdList(userIdList);

        BaseResponseData<List<UserProfileDTO>> responseData = accountFeign.queryByUserIdList(uacUserIdListDTO);
        if (responseData == null || CollectionUtils.isEmpty(responseData.getData())) {
            return new HashMap<>();
        }
        return responseData.getData().stream().collect(Collectors.toMap(k -> {
            return String.valueOf(k.getUserId());
        }, Function.identity(), (v1, v2) -> v1));
    }

    @Override
    public Map<String, UserProfileDTO> mapByEmpNoList(List<String> empNoList) {
        if (CollectionUtils.isEmpty(empNoList)) {
            return null;
        }
        QueryUserDTO queryUserDTO = new QueryUserDTO();
        queryUserDTO.setEmpNoList(empNoList);

        BaseResponseData<List<UserProfileDTO>> responseData = accountFeign.queryByEmpNoList(queryUserDTO);
        if (responseData == null || CollectionUtils.isEmpty(responseData.getData())) {
            return new HashMap<>();
        }
        return responseData.getData().stream().collect(Collectors.toMap(UserProfileDTO::getEmpNo, Function.identity(), (v1, v2) -> v1));
    }

    @Override
    public Map<String, UacUserDTO> queryByUserNameList(QueryUserDTO queryUserDTO){
        BaseResponseData<List<UacUserDTO>> responseData = accountFeign.queryByUserNameList(queryUserDTO);
        if (responseData == null || CollectionUtils.isEmpty(responseData.getData())) {
            return new HashMap<>();
        }
        List<UacUserDTO> userList = responseData.getData();
        Map<String, UacUserDTO> userMap = userList.stream().filter(user -> Objects.nonNull(user.getUsername())).
                collect(
                        Collectors.toMap(
                                UacUserDTO::getUsername, uacUserDTO -> uacUserDTO, (existing, replacment) -> replacment));
        return userMap;
    }

    /**
     * 用用户真实姓名替换列表中对象的userName字段（userName -> userName-realName）。
     *
     * @param list 原始数据列表
     * @param creatorExtractor 获取 creator 的方法引用
     * @param creatorSetter 设置 creator 的方法引用
     * @param <T> 对象类型
     *  例如：
     *   commonUserService.enrichCreatorWithRealName(spcNoteDOList,SpcNoteDO::getCreator,SpcNoteDO::setCreator);
     */
    public  <T> void enrichUserNameWithRealName(List<T> list,
                                                     Function<T, String> creatorExtractor,
                                                     BiConsumer<T, String> creatorSetter) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        List<String> creatorList = list.stream()
                .map(creatorExtractor)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(creatorList)) {
            return;
        }

        QueryUserDTO queryUserDTO = new QueryUserDTO();
        queryUserDTO.setUserNameList(creatorList);
        Map<String, UacUserDTO> userMap = queryByUserNameList(queryUserDTO);

        list.forEach(item -> {
            String creator = creatorExtractor.apply(item);
            if (userMap.containsKey(creator)) {
                UacUserDTO user = userMap.get(creator);
                creatorSetter.accept(item, user.getUsername() + "-" + user.getRealName());
            }
        });
    }
}
