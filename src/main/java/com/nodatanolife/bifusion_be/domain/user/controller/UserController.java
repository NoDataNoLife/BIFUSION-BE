package com.nodatanolife.bifusion_be.domain.user.controller;

import com.nodatanolife.bifusion_be.domain.user.dto.request.BioUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.response.BioUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.dto.request.NicknameUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.response.NicknameUpdateResponse;
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
        return CommonResponse.success(response, "닉네임이 수정되었습니다.");
    }

    @PutMapping("/api/v1/profile/introduction")
    public CommonResponse<BioUpdateResponse> updateBio(
            @RequestBody BioUpdateRequest request) {
        BioUpdateResponse response = userService.updateBio(1L, request.getBio());
        return CommonResponse.success(response, "소개글이 수정되었습니다.");
    }
}