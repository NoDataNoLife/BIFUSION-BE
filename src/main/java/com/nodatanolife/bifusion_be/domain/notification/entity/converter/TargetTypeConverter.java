package com.nodatanolife.bifusion_be.domain.notification.entity.converter;

import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.global.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class TargetTypeConverter extends EnumNameConverter<TargetType> {

    public TargetTypeConverter() {
        super(TargetType.class);
    }
}
