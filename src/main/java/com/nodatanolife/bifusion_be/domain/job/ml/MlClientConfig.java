package com.nodatanolife.bifusion_be.domain.job.ml;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(MlProperties.class)
public class MlClientConfig {

    static final String API_KEY_HEADER = "X-API-Key";

    @Bean
    public RestClient mlRestClient(RestClient.Builder builder, MlProperties properties) {
        // JDK HttpClient는 연결 타임아웃(HttpConnectTimeoutException)과 응답 대기 타임아웃(HttpTimeoutException)을
        // 예외 타입으로 구분해 준다. 실행 요청의 응답 대기 타임아웃은 ML 접수 여부가 불확실하므로 따로 다룬다.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
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
