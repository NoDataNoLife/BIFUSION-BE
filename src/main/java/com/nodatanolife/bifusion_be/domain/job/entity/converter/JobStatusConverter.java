package com.nodatanolife.bifusion_be.domain.job.entity.converter;

import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.global.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class JobStatusConverter extends EnumNameConverter<JobStatus> {

    public JobStatusConverter() {
        super(JobStatus.class);
    }
}
