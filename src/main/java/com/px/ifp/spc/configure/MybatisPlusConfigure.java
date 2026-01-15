package com.px.ifp.spc.configure;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.px.ifp.common.dto.common.response.FileDTO;
import com.px.ifp.spc.handle.ReportTypeDTO;
import com.px.ifp.spc.handle.FileHandler;
import com.px.ifp.spc.handle.ReportTypeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * mp 分页插件
 *
 * @author LI AI GUO
 * @date 2024.01.02
 */
@Configuration
public class MybatisPlusConfigure {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public ConfigurationCustomizer mybatisConfigurationCustomizer() {
        return configuration -> {
            configuration.getTypeHandlerRegistry().register(FileDTO.class, FileHandler.class);
            configuration.getTypeHandlerRegistry().register(ReportTypeDTO.class, ReportTypeHandler.class);

        };
    }

}