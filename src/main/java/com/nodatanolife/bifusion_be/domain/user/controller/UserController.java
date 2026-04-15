package com.nodatanolife.bifusion_be.domain.user.controller;

import com.nodatanolife.bifusion_be.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PutMapping("/api/v1/profile/nickname")
    public ResponseEntity<Void> updateNickname(
            @RequestParam String nickname) {
        // userId는 나중에 JWT에서 추출할 거예요
        userService.updateNickname(1L, nickname);
        return ResponseEntity.ok().build();
    }
}