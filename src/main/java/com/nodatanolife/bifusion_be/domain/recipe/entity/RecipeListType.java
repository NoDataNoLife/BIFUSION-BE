package com.nodatanolife.bifusion_be.domain.recipe.entity;

import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;

/**
 * "내 자산 레시피 목록 조회" API(AssetRecipeController)에서 type 파라미터로 사용되어
 * 본인이 직접 작성한 레시피인지, 남의 레시피를 포크해온 것인지를 구분한다.
 */
public enum RecipeListType {
    // forkedFrom이 null인, 본인이 직접 등록한 레시피
    MINE,
    // forkedFrom이 있는, 남의 레시피를 포크해 온 레시피
    FORKED;

    /**
     * 문자열 파라미터를 RecipeListType으로 변환한다.
     * 값이 없거나(null) enum에 없는 값이면 잘못된 요청으로 간주해 예외를 던진다.
     */
    public static RecipeListType from(String value) {
        try {
            return RecipeListType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_RECIPE_LIST_TYPE);
        }
    }
}
