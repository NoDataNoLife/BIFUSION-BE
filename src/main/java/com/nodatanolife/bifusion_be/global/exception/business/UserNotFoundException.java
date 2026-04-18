package com.nodatanolife.bifusion_be.global.exception.business;

import com.nodatanolife.bifusion_be.global.exception.ErrorCode;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}