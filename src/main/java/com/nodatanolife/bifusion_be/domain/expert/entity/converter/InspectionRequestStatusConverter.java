package com.nodatanolife.bifusion_be.domain.expert.entity.converter;

import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.global.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class InspectionRequestStatusConverter extends EnumNameConverter<InspectionRequestStatus> {

    public InspectionRequestStatusConverter() {
        super(InspectionRequestStatus.class);
    }
}
