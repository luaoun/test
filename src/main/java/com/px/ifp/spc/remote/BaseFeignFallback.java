package com.px.ifp.spc.remote;

import com.px.ifp.common.bean.common.BaseResponseData;
import com.px.ifp.common.dto.digitaltwins.request.QuerySystemCategoryByCodesDTO;
import com.px.ifp.common.dto.digitaltwins.response.SystemCategoryDTO;
import com.px.ifp.common.dto.digitaltwins.response.SystemCategoryTreeDTO;
import com.px.ifp.spc.remote.dto.QuerySystemCategoryDTO;

import java.util.List;

public class BaseFeignFallback implements BaseFeign{
    @Override
    public BaseResponseData<List<SystemCategoryTreeDTO>> querySystemModelCondition(QuerySystemCategoryDTO dto) {
        return BaseResponseData.fail("001", "查询失败", null);
    }

    @Override
    public BaseResponseData<List<SystemCategoryDTO>> queryByCodeList(QuerySystemCategoryByCodesDTO dto) {
        return BaseResponseData.fail("001", "查询子系统信息出错", null);
    }

    @Override
    public BaseResponseData<List<SystemCategoryDTO>> queryByAllList() {
        return BaseResponseData.fail("001", "查询子系统信息出错", null);
    }
}
