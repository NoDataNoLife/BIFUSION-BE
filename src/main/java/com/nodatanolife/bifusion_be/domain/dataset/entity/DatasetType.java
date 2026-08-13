package com.nodatanolife.bifusion_be.domain.dataset.entity;

import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;

public enum DatasetType {
    UPLOADED,
    AUGMENTED;

    public static DatasetType from(String value) {
        try {
            return DatasetType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_DATASET_LIST_TYPE);
        }
    }
}
