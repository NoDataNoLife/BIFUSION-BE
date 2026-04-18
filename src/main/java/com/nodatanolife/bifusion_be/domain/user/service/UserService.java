package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.user.dto.response.BioUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.dto.response.NicknameUpdateResponse;

public interface UserService {
    NicknameUpdateResponse updateNickname(Long userId, String nickname);
    BioUpdateResponse updateBio(Long userId, String bio);
}