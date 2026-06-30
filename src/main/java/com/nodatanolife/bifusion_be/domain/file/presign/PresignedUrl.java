package com.nodatanolife.bifusion_be.domain.file.presign;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 응답 DTO에서 S3 객체를 가리키는 String 필드에 붙인다.
 * 직렬화 시점에 저장된 값(S3 URL 또는 key)을 만료 시간이 있는 presigned URL로 변환해 내려준다.
 * 버킷은 private 상태이므로 이 어노테이션이 붙은 필드만 클라이언트가 접근 가능한 URL로 노출된다.
 *
 * record 컴포넌트에 붙일 때는 접근자(메서드)에 적용되도록 ElementType.METHOD 포함.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = PresignedUrlSerializer.class)
public @interface PresignedUrl {
}
