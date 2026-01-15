package com.px.ifp.spc.handle;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.px.ifp.common.dto.common.response.FileDTO;
import com.px.ifp.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.util.Optional;

@MappedJdbcTypes({JdbcType.VARCHAR})
@MappedTypes({FileDTO.class})
@Slf4j
public class FileHandler extends AbstractJsonTypeHandler<FileDTO> {

    @Override
    protected FileDTO parse(String json) {
        Optional<FileDTO> objects = Optional.ofNullable(JsonUtils.fromJson(json, FileDTO.class));
        return objects.orElse(null);
    }

    @Override
    protected String toJson(FileDTO obj) {
        return JsonUtils.toJson(obj);
    }
}

