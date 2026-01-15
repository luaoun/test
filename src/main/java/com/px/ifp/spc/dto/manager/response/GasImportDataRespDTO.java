package com.px.ifp.spc.dto.manager.response;

import com.px.ifp.common.bean.common.BaseBean;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-06-28
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GasImportDataRespDTO extends BaseBean {
    @Schema(description = "导入成功数量")
    private Integer successCount;

    @Schema(description = "导入失败数量")
    private Integer failCount;
}
