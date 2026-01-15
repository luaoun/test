package com.px.ifp.spc.configure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

@Configuration
public class ValidationConfig {
    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.setConstraintValidatorFactory(new SpringConstraintValidatorFactory(applicationContext.getAutowireCapableBeanFactory()));
        return validatorFactory;
    }
}