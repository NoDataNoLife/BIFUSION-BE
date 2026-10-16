package com.nodatanolife.bifusion_be.domain.job.entity.converter;

import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import com.nodatanolife.bifusion_be.global.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class JobTypeConverter extends EnumNameConverter<JobType> {

    public JobTypeConverter() {
        super(JobType.class);
    }
}
