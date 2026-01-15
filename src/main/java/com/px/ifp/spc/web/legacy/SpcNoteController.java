package com.px.ifp.spc.web.legacy;

import com.px.ifp.common.annotation.Log;
import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.bean.common.PageResponse;
import com.px.ifp.common.enums.BusinessType;
import com.px.ifp.common.web.BaseController;
import com.px.ifp.spc.dto.IdReqDTO;
import com.px.ifp.spc.dto.manager.request.AddSpcNormalNoteReqDTO;
import com.px.ifp.spc.dto.manager.request.AddSpcNoteReqDTO;
import com.px.ifp.spc.dto.manager.request.QuerySpcNoteListReqDTO;
import com.px.ifp.spc.dto.manager.request.UpdateSpcNoteDTO;
import com.px.ifp.spc.dto.manager.response.AddTagDTO;
import com.px.ifp.spc.dto.manager.response.LastSpcNoteDTO;
import com.px.ifp.spc.dto.manager.response.SpcNoteDTO;
import com.px.ifp.spc.entity.SpcNoteDO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-07-10
 */
@Tag(name = "SPC标注相关接口")
@RestController
@RequestMapping("/api/v1/spcNote")
public class SpcNoteController extends BaseController {

    @Autowired
    private SpcNoteController spcNoteController;

    @Log(title = "SPC批注", businessType = BusinessType.INSERT)
    @Operation(summary = "SPC批注-新增")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public BaseResponseData<Boolean> add(@RequestBody @Valid AddSpcNoteReqDTO reqDTO){
        BaseResponseData<Boolean> resp = spcNoteController.add(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "查询SPC批注列表")
    @RequestMapping(value = "/queryList", method = RequestMethod.POST)
    public BaseResponseData<PageResponse<SpcNoteDTO>> queryList(@RequestBody QuerySpcNoteListReqDTO reqDTO){
        BaseResponseData<PageResponse<SpcNoteDTO>> resp = spcNoteController.queryList(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "查询某个job_id对应的最新一条 spc_note 记录")
    @RequestMapping(value = "/getLatestNoteByJobIds", method = RequestMethod.POST)
    public BaseResponseData<List<LastSpcNoteDTO>> getLatestNoteByJobIds(@RequestBody List<String> jobIds){
        BaseResponseData<List<LastSpcNoteDTO>> resp = spcNoteController.getLatestNoteByJobIds(jobIds);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "SPC批注-修改")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public BaseResponseData<Boolean> update(@RequestBody UpdateSpcNoteDTO updateSpcNoteDTO){
        BaseResponseData<Boolean> resp = spcNoteController.update(updateSpcNoteDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "SPC批注-添加标记")
    @RequestMapping(value = "/tag", method = RequestMethod.POST)
    public BaseResponseData<Boolean> tag(@RequestBody AddTagDTO reqDTO){
        BaseResponseData<Boolean> resp = spcNoteController.tag(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "SPC批注-删除")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public BaseResponseData<Boolean> delete(@RequestBody IdReqDTO reqDTO){
        BaseResponseData<Boolean> resp = spcNoteController.delete(reqDTO);
        return BaseResponseData.success(resp.getData());
    }

    @Operation(summary = "SPC批注-选择SPC曲线上的数据添加Normal类型的批注")
    @RequestMapping(value ="/addNormalNote",method = RequestMethod.POST)
    public BaseResponseData<Boolean> addAlarmNote(@RequestBody AddSpcNormalNoteReqDTO reqDTO){
        BaseResponseData<Boolean> resp = spcNoteController.addAlarmNote(reqDTO);
        return BaseResponseData.success(resp.getData());
    }
}