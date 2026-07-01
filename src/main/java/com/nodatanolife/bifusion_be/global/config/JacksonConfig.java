package com.nodatanolife.bifusion_be.global.config;

import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.SpringHandlerInstantiator;

/**
 * 커스텀 Jackson 직렬화기/역직렬화기가 Spring Bean을 주입받을 수 있도록 SpringHandlerInstantiator를 등록한다.
 * {@link com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrlSerializer}가 S3Service를 주입받기 위해 필요.
 * Jackson2ObjectMapperBuilderCustomizer로 적용해 Spring Boot 기본 Jackson 설정(date-format 등)은 그대로 유지한다.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer handlerInstantiatorCustomizer(ApplicationContext applicationContext) {
        AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
        HandlerInstantiator handlerInstantiator = new SpringHandlerInstantiator(beanFactory);
        return builder -> builder.handlerInstantiator(handlerInstantiator);
    }
}
