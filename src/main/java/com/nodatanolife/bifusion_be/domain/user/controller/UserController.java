package com.nodatanolife.bifusion_be.domain.user.controller;

import com.nodatanolife.bifusion_be.domain.user.dto.NicknameUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.NicknameUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.service.UserService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/api/v1/profile/nickname")
    public CommonResponse<NicknameUpdateResponse> updateNickname(
            @RequestBody NicknameUpdateRequest request) {
        NicknameUpdateResponse response = userService.updateNickname(1L, request.getNickname());
        return CommonResponse.success(response);
    }
}