package com.nodatanolife.bifusion_be.domain.job.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 프로젝트 Job 목록 검색어 해석.
 * "JOB-015" 형식이거나 숫자면 jobId 일치, 그 외는 실행자 닉네임 부분 일치(대소문자 무시). 빈 값이면 조건 없음.
 *
 * @param jobId           jobId 검색이면 값, 아니면 null
 * @param nicknamePattern 닉네임 검색이면 소문자 LIKE 패턴(와일드카드 이스케이프 포함), 아니면 null
 */
public record JobSearchKeyword(Long jobId, String nicknamePattern) {

    public static final char LIKE_ESCAPE = '\\';
    private static final Pattern JOB_ID_PATTERN = Pattern.compile("^(?:JOB-?)?(\\d{1,18})$", Pattern.CASE_INSENSITIVE);
    private static final JobSearchKeyword NONE = new JobSearchKeyword(null, null);

    public static JobSearchKeyword parse(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return NONE;
        }
        String trimmed = keyword.strip();
        Matcher matcher = JOB_ID_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return new JobSearchKeyword(Long.valueOf(matcher.group(1)), null);
        }
        return new JobSearchKeyword(null, "%" + escapeLike(trimmed.toLowerCase()) + "%");
    }

    public boolean hasJobId() {
        return jobId != null;
    }

    public boolean hasNickname() {
        return nicknamePattern != null;
    }

    private static String escapeLike(String value) {
        return value.replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
    }
}
