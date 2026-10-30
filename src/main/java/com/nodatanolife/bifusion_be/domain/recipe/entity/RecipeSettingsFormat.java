package com.nodatanolife.bifusion_be.domain.recipe.entity;

/**
 * 레시피 설정값 형식.
 * LEGACY: 예전 생성 파라미터(model, steps, sampler, cfgScale, seed, resolution, batchSize)
 * AUGMENTATION: 증강 실행 파라미터와 같은 형식(augmentationSettings)
 */
public enum RecipeSettingsFormat {
    LEGACY,
    AUGMENTATION
}
