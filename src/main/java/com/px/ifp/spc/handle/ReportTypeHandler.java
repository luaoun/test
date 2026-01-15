package com.px.ifp.spc.handle;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.px.ifp.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.util.Optional;

@MappedJdbcTypes({JdbcType.VARCHAR})
@MappedTypes({ReportTypeDTO.class})
@Slf4j
public class ReportTypeHandler extends AbstractJsonTypeHandler<ReportTypeDTO> {

    @Override
    protected ReportTypeDTO parse(String json) {
        Optional<ReportTypeDTO> objects = Optional.ofNullable(JsonUtils.fromJson(json, ReportTypeDTO.class));
        return objects.orElse(null);
    }

    @Override
    protected String toJson(ReportTypeDTO obj) {
        return JsonUtils.toJson(obj);
    }
}

