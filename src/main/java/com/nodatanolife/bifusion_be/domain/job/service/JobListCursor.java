package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 프로젝트 Job 목록 커서. 정렬 키 (createdAt, jobId)를 opaque한 문자열로 감싼다.
 * 마이페이지 활동 커서와 같은 형식(Base64URL, "createdAt_id")이다.
 */
public record JobListCursor(LocalDateTime createdAt, Long jobId) {

    private static final String DELIMITER = "_";

    public String encode() {
        String raw = createdAt.toString() + DELIMITER + jobId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @return cursor가 비어 있으면 null (첫 페이지)
     */
    public static JobListCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int idx = raw.lastIndexOf(DELIMITER);
            return new JobListCursor(LocalDateTime.parse(raw.substring(0, idx)), Long.valueOf(raw.substring(idx + 1)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
    }
}
