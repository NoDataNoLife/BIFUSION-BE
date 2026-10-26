package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.expert.config.InspectionRewardProperties;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 검수 대상별 리워드 허용 범위. 대상이 차지하는 스토리지 크기(MB, 올림)에 비례한다.
 * 신청 검증(InspectionServiceImpl)과 화면 안내(InspectionTargetResponse.min/maxRewardPoints)가 같은 값을 쓴다.
 * 증강 데이터의 크기(결과 데이터셋 파일)는 File 조회가 필요해 호출 측에서 구해 넘긴다.
 */
@Component
@RequiredArgsConstructor
public class InspectionRewardPolicy {

    private static final long BYTES_PER_MB = 1024L * 1024;

    private final InspectionRewardProperties properties;

    public record RewardRange(int min, int max) {
    }

    /**
     * @param sizeBytes 대상의 스토리지 크기. 알 수 없으면 null (레시피와 같은 기본 범위)
     */
    public RewardRange forSize(Long sizeBytes) {
        long mb = sizeBytes == null || sizeBytes <= 0 ? 0 : (sizeBytes + BYTES_PER_MB - 1) / BYTES_PER_MB;
        long max = Math.min(properties.maxPoints(), Math.max(properties.baseMaxPoints(), mb * properties.maxPointsPerMb()));
        // 최소가 최대를 넘지 않게 한다 (최대 금액으로는 항상 신청할 수 있다)
        long min = Math.min(max, Math.max(properties.minPoints(), mb * properties.minPointsPerMb()));
        return new RewardRange((int) min, (int) max);
    }

    // 레시피는 생성 설정값이라 스토리지 크기 기준이 없다
    public RewardRange forRecipe() {
        return forSize(null);
    }

    public RewardRange forDataset(Dataset dataset) {
        return forSize(dataset.getFile() != null ? dataset.getFile().getFileSize() : null);
    }

    /**
     * 범위를 벗어나면 400(INSPECTION_REWARD_OUT_OF_RANGE).
     */
    public void validate(int rewardPoints, RewardRange range) {
        if (rewardPoints < range.min() || rewardPoints > range.max()) {
            throw new BusinessException(ErrorCode.INSPECTION_REWARD_OUT_OF_RANGE);
        }
    }
}
