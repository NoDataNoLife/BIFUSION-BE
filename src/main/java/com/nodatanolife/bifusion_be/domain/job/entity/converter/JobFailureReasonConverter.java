package com.nodatanolife.bifusion_be.domain.job.entity.converter;

import com.nodatanolife.bifusion_be.domain.job.entity.JobFailureReason;
import com.nodatanolife.bifusion_be.global.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class JobFailureReasonConverter extends EnumNameConverter<JobFailureReason> {

    public JobFailureReasonConverter() {
        super(JobFailureReason.class);
    }
}
