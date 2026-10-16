package com.nodatanolife.bifusion_be.domain.job.ml;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MlProperties.class)
public class MlClientConfig {

    static final String API_KEY_HEADER = "X-API-Key";

    @Bean
    public RestClient mlRestClient(RestClient.Builder builder, MlProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        RestClient.Builder mlBuilder = builder.clone().requestFactory(requestFactory);
        if (StringUtils.hasText(properties.baseUrl())) {
            mlBuilder.baseUrl(properties.baseUrl());
        }
        if (StringUtils.hasText(properties.apiKey())) {
            mlBuilder.defaultHeader(API_KEY_HEADER, properties.apiKey());
        }
        return mlBuilder.build();
    }
}
