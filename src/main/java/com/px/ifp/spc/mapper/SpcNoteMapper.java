package com.px.ifp.spc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.px.ifp.spc.bo.SpcAlarmNoteBO;
import com.px.ifp.spc.dto.manager.request.QuerySpcNoteListReqDTO;
import com.px.ifp.spc.entity.LastSpcNoteDO;
import com.px.ifp.spc.entity.SpcNoteDO;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-07-10
 */
public interface SpcNoteMapper extends BaseMapper<SpcNoteDO> {
    default List<SpcNoteDO> selectList(QuerySpcNoteListReqDTO reqDTO) {
        LambdaQueryWrapper<SpcNoteDO> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(reqDTO.getBusinessKey())){
            queryWrapper.eq(SpcNoteDO::getBusinessKey, reqDTO.getBusinessKey());
        }
        if (reqDTO.getStartTime() != null){
            queryWrapper.ge(SpcNoteDO::getCreateTime, reqDTO.getStartTime());
        }
        if (reqDTO.getEndTime() != null){
            queryWrapper.le(SpcNoteDO::getCreateTime, reqDTO.getEndTime());
        }
        queryWrapper.orderByDesc(SpcNoteDO::getId);
        return selectList(queryWrapper);
    }

    /**
     * 查询某个 job_id 对应的最新一条 spc_note 记录
     *
     * @param jobIds 作业 ID
     * @return 最新的 SpcNoteBO 对象
     */
    List<LastSpcNoteDO> getLatestNoteByJobIds(@Param("jobIds") List<String> jobIds, @Param("facCode") String facCode);

    List<SpcAlarmNoteBO> selectAnalysisAlarmNote(@Param("jobId") String jobId, @Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("spcAnalysisResultId") Long spcAnalysisResultId, @Param("tag") Integer tag, @Param("facCode") String facCode);


}

