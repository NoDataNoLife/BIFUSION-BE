package com.nodatanolife.bifusion_be.domain.dataset.entity;

import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;

/**
 * 데이터셋 종류 구분.
 * "내 자산 목록 조회" API(AssetDatasetController)에서 type 파라미터로 사용되어
 * 업로드한 원본 데이터인지, 증강(augmentation)으로 파생 생성된 데이터인지를 구분한다.
 */
public enum DatasetType {
    // 사용자가 직접 업로드한 원본 데이터셋
    UPLOADED,
    // 증강 파이프라인을 통해 생성된 파생 데이터셋
    AUGMENTED;

    /**
     * 문자열 파라미터를 DatasetType으로 변환한다.
     * 값이 없거나(null) enum에 없는 값이면 잘못된 요청으로 간주해 예외를 던진다.
     */
    public static DatasetType from(String value) {
        try {
            return DatasetType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_DATASET_LIST_TYPE);
        }
    }
}
