package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 마이페이지 "내 활동" 커서 인코딩/디코딩.
 * (createdAt, activityId)를 opaque한 문자열로 감싸 클라이언트에 노출한다.
 * activityId는 도메인마다 별개의 PK 시퀀스이지만, 동일 createdAt으로 정렬이 갈릴 때의
 * 결정론적 tie-break 용도로만 쓰이므로 타입이 달라도 그대로 재사용해도 무방하다.
 */
final class ActivityCursor {

    private static final String DELIMITER = "_";

    private ActivityCursor() {
    }

    record Value(LocalDateTime createdAt, Long id) {
    }

    static String encode(LocalDateTime createdAt, Long id) {
        String raw = createdAt.toString() + DELIMITER + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static Value decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int idx = raw.lastIndexOf(DELIMITER);
            LocalDateTime createdAt = LocalDateTime.parse(raw.substring(0, idx));
            Long id = Long.valueOf(raw.substring(idx + 1));
            return new Value(createdAt, id);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ACTIVITY_INVALID_CURSOR);
        }
    }
}
