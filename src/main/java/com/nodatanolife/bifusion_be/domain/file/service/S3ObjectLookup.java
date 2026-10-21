package com.nodatanolife.bifusion_be.domain.file.service;

/**
 * S3 HeadObject 조회 결과. 객체가 없는 경우와 조회 자체에 실패한 경우(권한·네트워크 등)를 구분한다.
 *
 * @param notFound 객체가 없다고 S3가 응답함 (404)
 * @param info     객체 정보. 없거나 조회에 실패하면 null
 */
public record S3ObjectLookup(
        boolean notFound,
        S3ObjectInfo info
) {

    public static S3ObjectLookup found(S3ObjectInfo info) {
        return new S3ObjectLookup(false, info);
    }

    public static S3ObjectLookup missing() {
        return new S3ObjectLookup(true, null);
    }

    public static S3ObjectLookup failed() {
        return new S3ObjectLookup(false, null);
    }
}
