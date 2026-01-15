package com.px.ifp.spc.handle;

import com.px.ifp.common.bean.common.BaseBean;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @Author: wjwei
 * @Date: 2024-04-24
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Getter
@Setter
public class ReportTypeDTO extends BaseBean {

    @Schema(description = "报表类型")
    private List<String> reportList;

}
