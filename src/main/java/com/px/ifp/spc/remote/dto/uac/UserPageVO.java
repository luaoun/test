package com.px.ifp.spc.remote.dto.uac;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserPageVO implements Serializable {
    private Long userId;
    private Long deptId;
    private String empNo;
    private String email;
    private String username;
    private String password;
    private String nickName;
    private String realName;
    private Integer sex;
    private String phone;
    private String deptName;
    private String roleNames;
    private Integer status;
    private Date lastLoginTime;
    private String createBy;
    private Date createTime;
    private Boolean sysBindFlag;
    private Boolean supperAdminFlag;
    private String userExpireTime;
    private String remarks;
}
