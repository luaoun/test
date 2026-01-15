package com.px.ifp.spc.util;

import com.px.ifp.common.bean.common.BaseResponse;
import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.bean.common.IResult;

public class ResponseDataUtil {
    public static  boolean isSuccess(BaseResponse response) {
        if (response != null && response.isSuccess() && IResult.SUCCESS.getCode().equals(response.getCode())) {
            return true;
        }
        return false;
    }

    /**
     * 包含数据
     * @param response
     */
    public static boolean containsData(BaseResponseData response) {
        return isSuccess(response) && response.getData() != null;
    }

}
