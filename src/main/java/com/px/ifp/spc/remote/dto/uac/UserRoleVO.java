package com.px.ifp.spc.remote.dto.uac;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserRoleVO {
    private static final long serialVersionUID = 6373997550677086520L;
    private List<Long> roleIdList = new ArrayList();
    private List<String> roleKeyList = new ArrayList();
    private String sysCode;
}
