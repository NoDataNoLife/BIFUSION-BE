package com.nodatanolife.bifusion_be.global.exception.business;

import com.nodatanolife.bifusion_be.global.exception.ErrorCode;

public class BioDataNotFoundException extends BusinessException {

    public BioDataNotFoundException() {
        super(ErrorCode.BIODATA_NOT_FOUND);
    }
}

