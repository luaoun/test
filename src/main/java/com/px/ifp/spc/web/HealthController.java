package com.px.ifp.spc.web;

import com.px.ifp.common.bean.common.BaseResponseData;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liaiguo on 2023.10.16
 */
@RestController
public class HealthController {

    @RequestMapping(value = "/health", method = RequestMethod.GET)
    public BaseResponseData<Map<String, String>> health() {
        Map<String, String> result = new HashMap<String, String>();
        result.put("status", "UP");
        return BaseResponseData.success(result);
    }

}
