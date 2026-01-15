package com.px.ifp.spc.service.note;

import com.baomidou.mybatisplus.extension.service.IService;
import com.px.ifp.spc.dto.manager.request.AddSpcNormalNoteReqDTO;
import com.px.ifp.spc.dto.manager.request.AddSpcNoteReqDTO;
import com.px.ifp.spc.dto.manager.request.QuerySpcNoteListReqDTO;
import com.px.ifp.spc.dto.manager.response.AddTagDTO;
import com.px.ifp.spc.dto.manager.response.LastSpcNoteDTO;
import com.px.ifp.spc.dto.manager.response.SpcNoteDTO;
import com.px.ifp.spc.entity.SpcNoteDO;

import java.util.List;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-07-10
 */
public interface SpcNoteService extends IService<SpcNoteDO> {
    /**
     * SPC批注-新增
     * @param reqDTO
     * @return
     */
    Boolean add(AddSpcNoteReqDTO reqDTO);

    boolean tag(AddTagDTO reqDTO);

    /**
     * 查询SPC批注列表
     * @param reqDTO
     * @return
     */
    List<SpcNoteDTO> queryList(QuerySpcNoteListReqDTO reqDTO);

    List<LastSpcNoteDTO> getLatestNoteByJobIds(List<String> jobIds);

    boolean addSpcNormalAnalysisRecord(AddSpcNormalNoteReqDTO dto);
}
