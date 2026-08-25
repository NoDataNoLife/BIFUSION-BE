package com.nodatanolife.bifusion_be.domain.notification.controller;

import com.nodatanolife.bifusion_be.domain.notification.service.NotificationService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification", description = "알림 API")
public class NotificationController {

    private final NotificationService notificationService;


    @Operation(summary = "알림 단건 읽음 처리", description = "본인 소유 알림만 읽음 처리 가능합니다.")
    @PatchMapping("/{notificationId}/read")
    public CommonResponse<Void> markAsRead(
            @AuthenticationPrincipal CustomOAuth2User userDetails,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(userDetails.getUser().getUserId(), notificationId);
        return CommonResponse.success(null, "알림을 읽음 처리했습니다.");
    }

    @Operation(summary = "알림 전체 읽음 처리", description = "본인의 안읽음 알림을 모두 읽음 처리합니다.")
    @PatchMapping("/read-all")
    public CommonResponse<Void> markAllAsRead(
            @AuthenticationPrincipal CustomOAuth2User userDetails
    ) {
        notificationService.markAllAsRead(userDetails.getUser().getUserId());
        return CommonResponse.success(null, "모든 알림을 읽음 처리했습니다.");
    }
}