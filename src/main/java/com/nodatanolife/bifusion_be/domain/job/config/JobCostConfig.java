package com.nodatanolife.bifusion_be.domain.job.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JobCostProperties.class)
public class JobCostConfig {
}
