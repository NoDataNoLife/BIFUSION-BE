package com.nodatanolife.bifusion_be.domain.notification.entity.converter;

import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.global.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class NotificationTypeConverter extends EnumNameConverter<NotificationType> {

    public NotificationTypeConverter() {
        super(NotificationType.class);
    }
}
