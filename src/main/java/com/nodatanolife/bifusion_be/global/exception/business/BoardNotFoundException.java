package com.nodatanolife.bifusion_be.global.exception.business;

import com.nodatanolife.bifusion_be.global.exception.ErrorCode;

public class BoardNotFoundException extends BusinessException {

    public BoardNotFoundException() {
        super(ErrorCode.BOARD_NOT_FOUND);
    }
}
