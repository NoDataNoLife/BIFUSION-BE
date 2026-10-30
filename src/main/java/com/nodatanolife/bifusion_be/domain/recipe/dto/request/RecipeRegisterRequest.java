package com.nodatanolife.bifusion_be.domain.recipe.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 레시피 등록 요청 DTO.
 * POST /api/v1/community/recipes(등록)와 PUT /api/v1/community/recipes/{recipeId}(수정) 요청 바디로 사용되며, 일반 등록과 포크 등록을 공동으로 처리한다.
 * (커뮤니티/내 자산 페이지 모두 이 API 하나를 공유하며, isPublic 값으로 공개 범위를 구분한다)
 */
public record RecipeRegisterRequest(
        // 레시피 제목 (필수)
        @NotBlank @Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.") String title,
        // 목록에 노출될 짧은 설명 (필수)
        @NotBlank @Size(max = 2000, message = "설명은 2000자 이하로 입력해 주세요.") String description,
        // 증강 설정값 직접 입력. sourceJobId와 둘 중 정확히 하나만 보낸다
        @Valid RecipeAugmentationSettingsRequest augmentationSettings,
        // 설정값을 불러올 내 프로젝트의 증강 작업 번호. 서버가 그 작업에 저장된 실행 값을 그대로 복사한다
        Long sourceJobId,
        // 상세 페이지 본문 설명 (선택)
        @Size(max = 2000, message = "본문 설명은 2000자 이하로 입력해 주세요.") String overview,
        // 핵심 특징 목록 (선택)
        @Size(max = 20, message = "핵심 특징은 20개 이하로 입력해 주세요.")
        List<@Size(max = 200, message = "핵심 특징은 항목당 200자 이하로 입력해 주세요.") String> features,
        // 사용 사례/추천 용도 목록 (선택)
        @Size(max = 20, message = "사용 사례는 20개 이하로 입력해 주세요.")
        List<@Size(max = 200, message = "사용 사례는 항목당 200자 이하로 입력해 주세요.") String> recommendations,
        // 포크 등록 시 원본 레시피 ID. null이면 일반 등록으로 처리된다
        Long forkedFromId,
        // 공개 여부. true(또는 생략) = 커뮤니티 공개, false = 내 자산 전용 비공개
        Boolean isPublic,
        // 대표 이미지 (선택). 임시 업로드(POST /api/v1/files/temp) 응답의 fileId, 본인이 올린 이미지 파일만 가능
        Long thumbnailFileId
) {
}
