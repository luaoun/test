package com.px.ifp.spc.web;

import com.px.ifp.common.bean.common.BaseResponseData;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public BaseResponseData<String> index() {
        return BaseResponseData.success("redirect:doc.html");
    }

}
