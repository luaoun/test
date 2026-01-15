package com.px.ifp.spc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(basePackages = {"com.px.ifp.spc", "com.px.ifp.common"})
@MapperScan({"com.px.ifp.spc.mapper", "com.px.ifp.common.mapper"})
@SpringBootApplication
@EnableFeignClients(basePackages = {"com.px.ifp.spc.remote", "com.px.ifp.common.remote"})
@Configuration
@EnableCaching
public class Spc {

    public static void main(String[] args) {
        SpringApplication.run(Spc.class, args);
    }

}
