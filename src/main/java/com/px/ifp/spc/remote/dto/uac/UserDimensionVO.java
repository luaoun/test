package com.px.ifp.spc.remote.dto.uac;

import lombok.Data;

import java.util.List;

@Data
public class UserDimensionVO {
    private String sysCode;
    private List<Long> dimensionIdList;
}
