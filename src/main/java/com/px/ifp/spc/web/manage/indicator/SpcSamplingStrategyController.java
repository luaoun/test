package com.px.ifp.spc.web.manage.indicator;

import com.px.ifp.common.annotation.Log;
import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.enums.BusinessType;
import com.px.ifp.spc.dto.manager.request.AddSpcPointMetaReqDTO;
import com.px.ifp.spc.dto.manager.request.SaveOrUpdateSamplingStrategyDTO;
import com.px.ifp.spc.entity.SpcSamplingStrategy;
import com.px.ifp.spc.service.indicator.SpcSamplingStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * <p>
 * SPC 采样策略配置表 前端控制器
 * </p>
 *
 * @author liuxj
 * @since 2026-01-08
 */
@RestController
@RequestMapping("/api/v2/spcSamplingStrategy")
public class SpcSamplingStrategyController {

    private SpcSamplingStrategyService spcSamplingStrategyService;

    @Log(title = "Spc策略配置", businessType = BusinessType.UPDATE)
    @Operation(summary = "新增SPC指标设定")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public BaseResponseData<Boolean> addPointMeta(@RequestBody @Valid SaveOrUpdateSamplingStrategyDTO reqDTO){
        return BaseResponseData.success(spcSamplingStrategyService.saveOrUpdate(reqDTO));
    }
}
