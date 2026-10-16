package com.nodatanolife.bifusion_be.domain.job.ml.dto.response;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * ML 결과의 S3 key 필드명이 확정되지 않아 key 해석을 이곳 한 곳에서만 한다.
 *
 * 1) key 필드가 있으면 사용한다. 단 ML이 bucket을 함께 보냈고 우리 버킷이 아니면 null.
 * 2) key가 없으면 URL에서 추출한다. 우리 버킷 URL일 때만 추출하며, 쿼리스트링(presigned 서명)은 버린다.
 *    - virtual-hosted: https://{bucket}.s3[.{region}].amazonaws.com/{key}
 *    - path-style:     https://s3[.{region}].amazonaws.com/{bucket}/{key}
 * 3) 그 외(다른 버킷, 로컬 경로, 형식 오류)는 null.
 */
final class MlStorageKeys {

    private static final String AWS_HOST_SUFFIX = ".amazonaws.com";

    private MlStorageKeys() {
    }

    static String resolve(String key, String url, String resultBucket, String ourBucket) {
        if (ourBucket == null || ourBucket.isBlank()) {
            return null;
        }
        if (key != null && !key.isBlank()) {
            return resultBucket == null || resultBucket.isBlank() || resultBucket.equals(ourBucket) ? key : null;
        }
        return extractKey(url, ourBucket);
    }

    private static String extractKey(String url, String ourBucket) {
        if (url == null || url.isBlank()) {
            return null;
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return null;
        }
        String host = uri.getHost();
        // getPath()는 쿼리스트링을 제외하고 퍼센트 인코딩을 디코딩한 경로다.
        String path = uri.getPath();
        if (host == null || path == null || !host.endsWith(AWS_HOST_SUFFIX)) {
            return null;
        }

        String extracted = null;
        if (host.startsWith(ourBucket + ".s3.") || host.startsWith(ourBucket + ".s3-")) {
            extracted = path.substring(1);
        } else if ((host.startsWith("s3.") || host.startsWith("s3-")) && path.startsWith("/" + ourBucket + "/")) {
            extracted = path.substring(ourBucket.length() + 2);
        }
        return extracted == null || extracted.isBlank() ? null : extracted;
    }
}
