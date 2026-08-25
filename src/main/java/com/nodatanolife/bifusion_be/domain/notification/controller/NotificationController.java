package com.nodatanolife.bifusion_be.domain.notification.controller;

import com.nodatanolife.bifusion_be.domain.notification.dto.response.NotificationResponse;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationCategory;
import com.nodatanolife.bifusion_be.domain.notification.service.NotificationService;
import com.nodatanolife.bifusion_be.global.dto.response.PageResponse;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Operation(
            summary = "알림 목록 조회",
            description = """
                    category(전문가/검수, 데이터 증강, 팀/프로젝트, 커뮤니티), isRead, keyword로 필터링해 페이지 조회합니다.
                    소프트 삭제된 알림은 결과에서 제외됩니다.
                    """
    )
    @GetMapping
    public CommonResponse<PageResponse<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomOAuth2User userDetails,
            @RequestParam(required = false) NotificationCategory category,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> result = notificationService.getNotifications(
                userDetails.getUser().getUserId(), category, isRead, keyword, pageable);
        return CommonResponse.success(PageResponse.from(result), "알림 목록 조회 성공!");
    }

    @Operation(
            summary = "알림 전체목록 삭제(hard) - 알림 전체목록",
            description = """
                    notificationId가 있으면 해당 알림 하나만, 없으면 본인 알림 전체를 영구 삭제(hard delete)합니다.
                    개별 삭제 시 존재하지 않으면 404, 본인 알림이 아니면 403이 반환됩니다.
                    """
    )
    @DeleteMapping
    public CommonResponse<Void> hardDeleteNotifications(
            @AuthenticationPrincipal CustomOAuth2User userDetails,
            @RequestParam(required = false) Long notificationId
    ) {
        notificationService.hardDeleteNotifications(userDetails.getUser().getUserId(), notificationId);
        return CommonResponse.success(null, "알림을 삭제했습니다.");
    }

    @Operation(
            summary = "알림 팝업 삭제(soft) - 알림창",
            description = """
                    notificationId가 있으면 해당 알림 하나만, 없으면 본인 알림 전체를 소프트 삭제(isDeleted=true)합니다.
                    실제로 삭제되지 않고 목록 조회/안읽음 카운트에서만 제외됩니다.
                    개별 삭제 시 존재하지 않거나 본인 알림이 아니거나 이미 삭제된 경우 404가 반환됩니다.
                    """
    )
    @DeleteMapping("/popup")
    public CommonResponse<Void> softDeleteNotifications(
            @AuthenticationPrincipal CustomOAuth2User userDetails,
            @RequestParam(required = false) Long notificationId
    ) {
        notificationService.softDeleteNotifications(userDetails.getUser().getUserId(), notificationId);
        return CommonResponse.success(null, "알림을 삭제했습니다.");
    }
}