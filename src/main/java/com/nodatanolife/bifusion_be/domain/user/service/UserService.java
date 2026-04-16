package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.user.dto.NicknameUpdateResponse;

public interface UserService {
    NicknameUpdateResponse updateNickname(Long userId, String nickname);
}