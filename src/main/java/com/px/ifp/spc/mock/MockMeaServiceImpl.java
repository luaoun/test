package com.px.ifp.spc.mock;

import com.px.ifp.common.dto.account.response.MeaDTO;
import com.px.ifp.common.service.measure.MeaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * MeaService 的 Mock 实现
 * 仅在 local profile 下生效，用于本地开发测试
 * 继承 MeaService 并覆盖 getListByMeasureCodes 方法
 *
 * @author mock
 * @since 2026-01-27
 */
@Slf4j
@Service
@Profile("local")
@Primary
public class MockMeaServiceImpl extends MeaService {

    @Override
    public List<MeaDTO> getListByMeasureCodes(List<String> measureCodes) {
        log.info("[MockMeaService] getListByMeasureCodes called with: {}", measureCodes);

        List<MeaDTO> result = new ArrayList<>();
        if (measureCodes == null || measureCodes.isEmpty()) {
            return result;
        }

        for (String measureCode : measureCodes) {
            MeaDTO meaDTO = new MeaDTO();
            meaDTO.setMeasureCode(measureCode);
            meaDTO.setMeasureName("Mock_" + measureCode);
            meaDTO.setMeasureUnit("unit");
            result.add(meaDTO);
            log.debug("[MockMeaService] Created mock MeaDTO for measureCode: {}", measureCode);
        }

        return result;
    }
}
