package com.nodatanolife.bifusion_be.domain.job.ml.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * ML이 S3에 올린 결과 파일 위치 (result.artifacts.download / preview). 모든 필드 nullable.
 * ML은 presigned URL을 만들지 않으므로 BE는 key만 저장하고 응답 시점에 presign한다.
 *
 * @param bucket    ML이 업로드한 버킷. 비어 있으면 우리 버킷으로 본다.
 * @param sizeBytes 업로드한 파일 크기
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlArtifact(
        String bucket,
        String key,
        Long sizeBytes
) {

    /**
     * ML 결과 파일이 올라가는 경로. 이 밖의 key(다른 사용자의 업로드 파일 등)는 presign 대상으로 연결하지 않는다.
     */
    public static final String RESULT_KEY_PREFIX = "ml-results/";

    public boolean hasKey() {
        return key != null && !key.isBlank();
    }

    /**
     * key가 있을 때 우리 결과 파일로 연결할 수 없는 사유. 연결할 수 있으면 null.
     *
     * @param ourBucket 우리 S3 버킷명 (spring.cloud.aws.s3.bucket). BE는 이 버킷만 presign한다.
     */
    public String rejectReason(String ourBucket) {
        if (bucket != null && !bucket.isBlank() && !bucket.equals(ourBucket)) {
            return "설정된 버킷이 아님";
        }
        if (!key.startsWith(RESULT_KEY_PREFIX) || key.contains("..")) {
            return "결과 파일 경로(" + RESULT_KEY_PREFIX + ")가 아님";
        }
        return null;
    }
}
