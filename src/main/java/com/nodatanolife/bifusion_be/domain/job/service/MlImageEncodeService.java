package com.nodatanolife.bifusion_be.domain.job.service;

import java.util.List;
import java.util.Map;

public interface MlImageEncodeService {

    /**
     * 업로드된 파일(fileId)을 ML 입력용 Base64 문자열로 변환한다.
     * 한 번의 ML 요청에 들어갈 파일을 모두 넘겨야 총 용량 상한이 요청 단위로 적용된다
     * (증강은 정상·비정상 fileId를 합쳐서 호출).
     *
     * @return fileId → Base64. 입력 순서를 유지하며 중복 fileId는 한 번만 담긴다.
     */
    Map<Long, String> encodeToBase64(Long userId, List<Long> fileIds);
}
