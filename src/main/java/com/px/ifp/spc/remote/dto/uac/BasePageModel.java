package com.px.ifp.spc.remote.dto.uac;

import lombok.Data;

import java.io.Serializable;

@Data
public class BasePageModel implements Serializable {
    private Integer page;
    private Integer pageSize;
    private String sortName;
    private String sortBy = "asc";
}
