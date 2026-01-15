package com.px.ifp.spc.web;

import com.px.ifp.spc.dto.manager.request.MockSensorDataRequest;
import com.px.ifp.spc.dto.TdengineScadaQuery;
import com.px.ifp.spc.service.iot.TdengineScadaQueryServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tdengine/scada")
public class TdengineScadaController {
    @Autowired
    private TdengineScadaQueryServiceImpl tdengineScadaQueryService;

    @PostMapping("/query")
    public List<Map<String, Object>> queryScadaIndicator(@RequestBody TdengineScadaQuery query) {
        return tdengineScadaQueryService.queryScadaIndicator(query);
    }

    @PostMapping("/mock/generate")
    public Map<String, Object> generateMockData(@RequestBody MockSensorDataRequest request) {
        String sensorId = request.getSensorId() != null ? request.getSensorId() : "sensor_001";
        String location = request.getLocation() != null ? request.getLocation() : "Factory-A";
        int count = request.getCount() != null ? request.getCount() : 120;
        int intervalSeconds = request.getIntervalSeconds() != null ? request.getIntervalSeconds() : 1;

        return tdengineScadaQueryService.generateMockSensorData(sensorId, location, count, intervalSeconds);
    }
}
