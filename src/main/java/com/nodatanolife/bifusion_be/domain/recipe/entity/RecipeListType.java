package com.nodatanolife.bifusion_be.domain.recipe.entity;

import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;

public enum RecipeListType {
    MINE,
    FORKED;

    public static RecipeListType from(String value) {
        try {
            return RecipeListType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_RECIPE_LIST_TYPE);
        }
    }
}
