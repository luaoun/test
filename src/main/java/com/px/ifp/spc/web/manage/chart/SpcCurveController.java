package com.px.ifp.spc.web.manage.chart;

import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.web.BaseController;
import com.px.ifp.spc.dto.manager.request.QuerySpcAnalysisReqDTO;
import com.px.ifp.spc.dto.publish.response.SpcAnalysisDTO;
import com.px.ifp.spc.service.indicator.SpcPointMetaDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("api/v2/curve")
@Tag(name = "SPC曲线数据", description = "SPC曲线数据与报警标记API")
public class SpcCurveController extends BaseController {
    @Autowired
    private SpcPointMetaDataService spcPointMetaDataService;

    @Operation(summary = "查询SPC曲线")
    @RequestMapping(value = "/data", method = RequestMethod.POST)
    public BaseResponseData<List<SpcAnalysisDTO>> getSpcCurveData(@RequestBody @Valid QuerySpcAnalysisReqDTO reqDTO){
        //设置截止时间为当前时间，确保SPC的数据是当前之前的，不存在未来时间的数据
        if(Objects.nonNull(reqDTO.getEndTime())){
            Date currentTime = new Date();
            if(reqDTO.getEndTime().after(currentTime)){
                reqDTO.setEndTime(currentTime);
            }
        }
        return BaseResponseData.success(spcPointMetaDataService.querySpcAnalysis(reqDTO));
    }

}
