package com.nodatanolife.bifusion_be.domain.job.entity.converter;

import com.nodatanolife.bifusion_be.domain.job.entity.PredictedClass;
import com.nodatanolife.bifusion_be.global.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class PredictedClassConverter extends EnumNameConverter<PredictedClass> {

    public PredictedClassConverter() {
        super(PredictedClass.class);
    }
}
