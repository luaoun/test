package com.px.ifp.spc.web.manage.indicator;

import com.github.pagehelper.Page;
import com.px.ifp.common.annotation.Log;
import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.bean.common.PageResponse;
import com.px.ifp.common.enums.BusinessType;
import com.px.ifp.common.exception.BusinessException;
import com.px.ifp.common.service.measure.MeaService;
import com.px.ifp.common.web.BaseController;
import com.px.ifp.spc.dto.IdReqDTO;
import com.px.ifp.spc.dto.manager.request.AddSpcPointMetaReqDTO;
import com.px.ifp.spc.dto.manager.request.QuerySpcIndicatorDTO;
import com.px.ifp.spc.dto.manager.request.SpcStatusChangeDTO;
import com.px.ifp.spc.dto.manager.request.UpdateSpcPointMetaReqDTO;
import com.px.ifp.spc.dto.manager.response.QuerySpcIndicatorDetailRespDTO;
import com.px.ifp.spc.dto.publish.request.QuerySpcIndicatorListReqDTO;
import com.px.ifp.spc.dto.publish.response.SpcAnalysisDTO;
import com.px.ifp.spc.entity.SpcPointMetadataDO;
import com.px.ifp.spc.error.OperationError;
import com.px.ifp.spc.mapper.SpcPointMetadataMapper;
import com.px.ifp.spc.service.indicator.SpcPointMetaDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-08
 */
@Tag(name = "SPC指标管理相关接口")
@RestController
@RequestMapping("/api/v2/spcPointMetaData")
public class SpcPointMetaDataController extends BaseController {
    @Autowired
    protected SpcPointMetaDataService spcPointMetaDataService;

    @Autowired
    protected SpcPointMetadataMapper spcPointMetaDataMapper;

    @Autowired
    protected MeaService meaService;

    @Log(title = "SPC指标", businessType = BusinessType.INSERT)
    @Operation(summary = "新增SPC指标设定")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public BaseResponseData<Boolean> addPointMeta(@RequestBody @Valid AddSpcPointMetaReqDTO reqDTO){
        return BaseResponseData.success(spcPointMetaDataService.add(reqDTO));
    }

    @Log(title = "SPC指标", businessType = BusinessType.UPDATE)
    @Operation(summary = "更新SPC指标设定")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public BaseResponseData<Boolean> updatePointMeta(@RequestBody @Valid UpdateSpcPointMetaReqDTO reqDTO){
        return BaseResponseData.success(spcPointMetaDataService.update(reqDTO));
    }

    @Log(title = "SPC指标", businessType = BusinessType.DELETE)
    @Operation(summary = "删除SPC指标设定")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public BaseResponseData<Boolean> deletePointMeta(@RequestBody @Valid IdReqDTO reqDTO){
        SpcPointMetadataDO spcIndicatorDO = spcPointMetaDataService.getById(reqDTO.getId());
        return BaseResponseData.success(spcPointMetaDataService.delete(reqDTO));
    }

    @Operation(summary = "查询SPC指标详情")
    @RequestMapping(value = "/queryDetail", method = RequestMethod.POST)
    public BaseResponseData<QuerySpcIndicatorDetailRespDTO> queryPointMetaDetail(@RequestBody @Valid IdReqDTO reqDTO){
        return BaseResponseData.success(spcPointMetaDataService.queryDetail(reqDTO));
    }

    @Operation(summary = "查询SPC指标详情")
    @RequestMapping(value = "/commonQueryDetail", method = RequestMethod.POST)
    public BaseResponseData<QuerySpcIndicatorDetailRespDTO> queryDetail(@RequestBody @Valid QuerySpcIndicatorDTO reqDTO){
        return BaseResponseData.success(spcPointMetaDataService.querySpcIndicatorDetail(reqDTO));
    }

    @Operation(summary = "查询SPC指标列表")
    @RequestMapping(value = "/queryList", method = RequestMethod.POST)
    public BaseResponseData<PageResponse<SpcAnalysisDTO>> queryPointMetaList(@RequestBody QuerySpcIndicatorListReqDTO reqDTO){
        Page page = startPage(reqDTO);
        List<SpcAnalysisDTO> list = spcPointMetaDataService.queryList(reqDTO);
        // querySpcCount = true 会去查询 zeone，必须指定时间范围，否则查询数据量太大
        if (reqDTO.getQuerySpcCount() != null && reqDTO.getQuerySpcCount() ) {
            if(Objects.isNull(reqDTO.getStartTime()) || Objects.isNull(reqDTO.getEndTime()))
                throw new BusinessException(OperationError.PARAM_ERROR.getErrorCode(),"未指定 开始时间 和 结束时间");
        }
        return BaseResponseData.success(getData(list, page));
    }

    @Operation(summary = "更新spc指标监控状态（status = 0:禁用| 1:启用）")
    @PostMapping(value = "/updateStatus")
    public BaseResponseData<Boolean> updateStatus(@RequestBody SpcStatusChangeDTO statusChangeDTO){
        return BaseResponseData.success(spcPointMetaDataService.updateStatus(statusChangeDTO));
    }
}
