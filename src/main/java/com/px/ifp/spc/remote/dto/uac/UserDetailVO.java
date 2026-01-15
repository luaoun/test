package com.px.ifp.spc.remote.dto.uac;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class UserDetailVO implements Serializable {
    private static final long serialVersionUID = -8029802544403883540L;
    private Long userId;
    @ApiModelProperty("用户登录名")
    private String username;
    @ApiModelProperty("用户昵称")
    private String nickName;
    @ApiModelProperty("真实姓名")
    private String realName;
    @ApiModelProperty("用户性别 1-男;2-女;0-未知")
    private Integer sex;
    private String phone;
    private String email;
    @ApiModelProperty("头像")
    private String avatar;
    @ApiModelProperty("状态 0-正常;1-停用")
    private Integer status;
    @ApiModelProperty("最后登录IP")
    private String loginIp;
    @ApiModelProperty("最后登录时间")
    private Date lastLoginTime;
    private String remarks;
    private Long deptId;
    private String deptName;
    private List<UserRoleVO> userRoles;
    private String userExpireTime;
    private String empNo;
    private List<UserDimensionVO> userDimensionVOList;
    private Long directLeaderId;
    private Integer directLeaderType;
    private List<Long> positionIdList;
    private List<Integer> chargeSystemList;
    private String detpLeaders;

}
