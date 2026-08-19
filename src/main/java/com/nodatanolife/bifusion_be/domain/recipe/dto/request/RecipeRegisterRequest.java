package com.nodatanolife.bifusion_be.domain.recipe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 레시피 등록 요청 DTO.
 * POST /api/v1/community/recipes 요청 바디로 사용되며, 일반 등록과 포크 등록을 공동으로 처리한다.
 * (커뮤니티/내 자산 페이지 모두 이 API 하나를 공유하며, isPublic 값으로 공개 범위를 구분한다)
 */
public record RecipeRegisterRequest(
        // 레시피 제목 (필수)
        @NotBlank String title,
        // 목록에 노출될 짧은 설명 (필수)
        @NotBlank String description,
        // 생성에 사용된 모델명 (필수)
        @NotBlank String modelName,
        // 생성 스텝 수 (필수)
        @NotNull Integer steps,
        // 샘플러 종류 (필수)
        @NotBlank String sampler,
        // CFG Scale 값 (필수)
        @NotNull Float cfgScale,
        // 시드값 (필수)
        @NotNull Integer seed,
        // 출력 해상도 (필수)
        @NotBlank String resolution,
        // 배치 사이즈 (필수)
        @NotNull Integer batchSize,
        // 상세 페이지 본문 설명 (선택)
        String overview,
        // 핵심 특징 목록 (선택)
        List<String> features,
        // 사용 사례/추천 용도 목록 (선택)
        List<String> recommendations,
        // 포크 등록 시 원본 레시피 ID. null이면 일반 등록으로 처리된다
        Long forkedFromId,
        // 공개 여부. true(또는 생략) = 커뮤니티 공개, false = 내 자산 전용 비공개
        Boolean isPublic
) {
}
