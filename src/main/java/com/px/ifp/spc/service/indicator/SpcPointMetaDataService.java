package com.px.ifp.spc.service.indicator;

import com.baomidou.mybatisplus.extension.service.IService;
import com.px.ifp.common.dto.data.DataEventDTO;
import com.px.ifp.spc.bo.NoModelExcelBO;
import com.px.ifp.spc.dto.manager.request.*;
import com.px.ifp.spc.dto.manager.response.QuerySpcIndicatorDetailRespDTO;
import com.px.ifp.spc.dto.publish.request.*;
import com.px.ifp.spc.dto.publish.response.*;
import com.px.ifp.spc.dto.manager.response.SpcAnalysisResultDTO;
import com.px.ifp.spc.bo.SpcIndicatorExcelBO;
import com.px.ifp.spc.dto.*;
import com.px.ifp.spc.entity.SpcPointMetadataDO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-05-08
 */
public interface SpcPointMetaDataService extends IService<SpcPointMetadataDO> {
    /**
     * 新增SPC指标
     * @param reqDTO
     * @return
     */
    Boolean add(AddSpcPointMetaReqDTO reqDTO);

    /**
     * 修改SPC指标
     * @param reqDTO
     * @return
     */
    Boolean update(UpdateSpcPointMetaReqDTO reqDTO);

    /**
     * 删除SPC指标
     * @param reqDTO
     * @return
     */
    Boolean delete(IdReqDTO reqDTO);

    QuerySpcIndicatorDetailRespDTO querySpcIndicatorDetail(QuerySpcIndicatorDTO reqDTO);

    List<QuerySpcIndicatorDetailRespDTO> querySpcIndicatorList(QuerySpcIndicatorDTO reqDTO);

    Map<String,String> querySpcJobsBySystemCodeAndPoint(List<String> systemCodeAndPointList);

    /**
     * 查询SPC指标设定列表
     * @param reqDTO
     * @return
     */
    List<SpcAnalysisDTO> queryList(QuerySpcIndicatorListReqDTO reqDTO);

    /**
     * 查询SPC指标详情
     * @param reqDTO
     * @return
     */
    QuerySpcIndicatorDetailRespDTO queryDetail(IdReqDTO reqDTO);

    /**
     * 查询告警数量
     * @param reqDTO
     * @return
     */
    QuerySpcAnalysisResultCountRespDTO queryResultCount(QuerySpcAnalysisResultCountReqDTO reqDTO);

    /**
     * 查询SPC指标分析
     * @param reqDTO
     * @return
     */
    List<SpcAnalysisDTO> querySpcAnalysis(QuerySpcAnalysisReqDTO reqDTO);

    /**
     * 针对上报点位数据进行SPC指标计算
     * @param dataEventDTO
     */
    @Deprecated
    void spcCalculate(DataEventDTO dataEventDTO);

    /**
     * 查询SPC指标分析结果列表
     * @param reqDTO
     * @return
     */
    List<SpcAnalysisResultDTO> querySpcAnalysisResultList(QuerySpcAnalysisResultListReqDTO reqDTO);

    /**
     * 批量导入数据
     * @param dtoList
     */
    void importData(List<SpcIndicatorExcelBO> dtoList);

    /**
     * 刷新Redis缓存
     * @param reqDTO
     */
    void refreshRedisCache(QuerySpcIndicatorListReqDTO reqDTO);

    void refreshAllRedisCache();

    List<SpcAnalysisDTO> queryDetailByJobId(List<String> jobIds, List<String> points);

    SpcAnalysisDTO getDetailByJobId(String jobId);


    List<SpcAnalysisResultNoteDTO> queryspcAnalysisResultNote(QuerySpcAnalysisResultNotesDTO query);

    List<SpcIndicatorDataSummaryDTO> queryAnalysisResultsBySystemAndPoints(QuerySpcIndicatorSummaryDTO querySpcIndicatorSummaryDTO);

    List<NoModelExcelBO> querySpcAnalysisExport(QuerySpcAnalysisReqDTO reqDTO);

    String calculateSpcAlarm(String point, BigDecimal value, Date eventTime);

    List<SpcIndicatorTreeDTO> querySpcIndicatorTree(QuerySpcIndicatorTreeDTO reqDTO);

    List<SpcAlarmRelTimeDTO> querySpcAlarmRealTime(QuerySpcAlarmRealTimeDTO reqDTO);

    void synchronizeSpcIndcatorUnit();

    Boolean updateStatus(SpcStatusChangeDTO statusChangeDTO);

    List<Long> getAllActiveIndicatorIds();

    /**
     * 获取所有活跃的SPC指标（用于XXL Job一致性检查）
     */
    public List<SpcPointMetadataDO> getAllActiveIndicators() ;

    /**
     * 获取所有指标（包括非活跃的）
     */
    public List<SpcPointMetadataDO> getAllIndicators() ;

    /**
     * 根据指标ID列表获取指标信息
     */
    public List<SpcPointMetadataDO> getIndicatorsByIds(List<Long> indicatorIds) ;

    List<SpcPointMetadataDO> getIndicatorsByPoint(String facCode,String point);


}
