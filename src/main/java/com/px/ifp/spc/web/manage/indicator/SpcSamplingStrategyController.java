package com.px.ifp.spc.web.manage.indicator;

import com.px.ifp.common.annotation.Log;
import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.enums.BusinessType;
import com.px.ifp.spc.dto.manager.request.AddSpcPointMetaReqDTO;
import com.px.ifp.spc.dto.manager.request.SaveOrUpdateSamplingStrategyDTO;
import com.px.ifp.spc.entity.SpcSamplingStrategy;
import com.px.ifp.spc.error.OperationError;
import com.px.ifp.spc.service.indicator.SpcSamplingStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * SPC 采样策略配置表 前端控制器
 * </p>
 *
 * @author liuxj
 * @since 2026-01-08
 */
@Tag(name = "SPC策略配置")
@RestController
@RequestMapping("/api/v2/spcSamplingStrategy")
public class SpcSamplingStrategyController {

    @Autowired
    private SpcSamplingStrategyService spcSamplingStrategyService;

    @Log(title = "Spc策略配置", businessType = BusinessType.INSERT)
    @Operation(summary = "新增或更新SPC采样策略")
    @PostMapping("/saveOrUpdate")
    public BaseResponseData<Boolean> saveOrUpdate(@RequestBody @Valid SaveOrUpdateSamplingStrategyDTO reqDTO){
        return BaseResponseData.success(spcSamplingStrategyService.saveOrUpdate(reqDTO));
    }

    @Log(title = "Spc策略配置", businessType = BusinessType.OTHER)
    @Operation(summary = "根据ID查询SPC采样策略")
    @GetMapping("/getById")
    public BaseResponseData<SpcSamplingStrategy> getById(@RequestParam Long id){
        SpcSamplingStrategy strategy = spcSamplingStrategyService.getById(id);
        return BaseResponseData.success(strategy);
    }

    @Log(title = "Spc策略配置", businessType = BusinessType.OTHER)
    @Operation(summary = "根据指标编码和采样周期查询SPC采样策略")
    @GetMapping("/getByMeasureCodeAndPeriod")
    public BaseResponseData<SpcSamplingStrategy> getByMeasureCodeAndPeriod(
            @RequestParam String measureCode,
            @RequestParam Integer periodS){
        SpcSamplingStrategy strategy = spcSamplingStrategyService.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SpcSamplingStrategy>()
                .eq("measure_code", measureCode)
                .eq("period_s", periodS)
                .last("limit 1")
        );
        return BaseResponseData.success(strategy);
    }

    @Log(title = "Spc策略配置", businessType = BusinessType.OTHER)
    @Operation(summary = "查询所有SPC采样策略列表")
    @GetMapping("/list")
    public BaseResponseData<java.util.List<SpcSamplingStrategy>> list(){
        java.util.List<SpcSamplingStrategy> list = spcSamplingStrategyService.list();
        return BaseResponseData.success(list);
    }

    @Log(title = "Spc策略配置", businessType = BusinessType.OTHER)
    @Operation(summary = "根据指标编码查询SPC采样策略列表")
    @GetMapping("/listByMeasureCode")
    public BaseResponseData<java.util.List<SpcSamplingStrategy>> listByMeasureCode(@RequestParam String measureCode){
        java.util.List<SpcSamplingStrategy> list = spcSamplingStrategyService.list(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SpcSamplingStrategy>()
                .eq("measure_code", measureCode)
        );
        return BaseResponseData.success(list);
    }

    @Log(title = "Spc策略配置", businessType = BusinessType.UPDATE)
    @Operation(summary = "更新SPC采样策略")
    @PostMapping("/update")
    public BaseResponseData<Boolean> update(@RequestBody @Valid SaveOrUpdateSamplingStrategyDTO reqDTO){
        return BaseResponseData.success(spcSamplingStrategyService.saveOrUpdate(reqDTO));
    }

    @Log(title = "Spc策略配置", businessType = BusinessType.DELETE)
    @Operation(summary = "根据ID删除SPC采样策略")
    @PostMapping("/deleteById")
    public BaseResponseData<Boolean> deleteById(@RequestParam Long id){
        boolean result = spcSamplingStrategyService.removeById(id);
        return BaseResponseData.success(result);
    }

    @Log(title = "Spc策略配置", businessType = BusinessType.DELETE)
    @Operation(summary = "批量删除SPC采样策略")
    @PostMapping("/deleteBatch")
    public BaseResponseData<Boolean> deleteBatch(@RequestBody java.util.List<Long> ids){
        boolean result = spcSamplingStrategyService.removeByIds(ids);
        return BaseResponseData.success(result);
    }
}
