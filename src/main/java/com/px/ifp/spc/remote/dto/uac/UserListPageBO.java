package com.px.ifp.spc.remote.dto.uac;


import lombok.Data;

import java.util.List;

@Data
public class UserListPageBO extends BasePageModel {

    private String usernameLike;
    private String nickNameLike;
    private String realNameLike;
    private String phoneLike;
    private List<Long> deptIdList;
    private List<String> deptNameList;
    private Long roleId;
    private Integer status;
    private String sysCode;
}
