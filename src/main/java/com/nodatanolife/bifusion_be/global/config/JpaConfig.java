package com.nodatanolife.bifusion_be.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
