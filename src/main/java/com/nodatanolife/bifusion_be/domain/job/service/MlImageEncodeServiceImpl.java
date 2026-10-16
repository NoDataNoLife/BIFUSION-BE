package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.file.service.S3Service;
import com.nodatanolife.bifusion_be.domain.job.ml.MlProperties;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MlImageEncodeServiceImpl implements MlImageEncodeService {

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private final FileRepository fileRepository;
    private final S3Service s3Service;
    private final MlProperties mlProperties;

    /**
     * 1) 파일 존재 확인  2) 요청자 소유 확인  3) 총 용량 상한 확인
     * 4) S3에서 읽어 JPG/PNG 여부를 파일 내용(시그니처)으로 확인  5) Base64 변환
     * file_type은 업로드 시 클라이언트가 보낸 값이라 형식 판정에 사용하지 않는다.
     * S3 다운로드 동안 DB 커넥션을 점유하지 않도록 트랜잭션 밖에서 실행한다(파일 조회는 리포지토리 자체 트랜잭션).
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<Long, String> encodeToBase64(Long userId, List<Long> fileIds) {
        List<Long> distinctIds = fileIds.stream().distinct().toList();
        Map<Long, File> fileById = fileRepository.findAllById(distinctIds).stream()
                .collect(Collectors.toMap(File::getFileId, Function.identity()));
        if (fileById.size() != distinctIds.size()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        long maxBytes = mlProperties.maxInputSize().toBytes();
        long declaredTotal = 0;
        for (File file : fileById.values()) {
            if (!file.getUser().getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
            }
            declaredTotal += file.getFileSize() != null ? file.getFileSize() : 0;
        }
        if (declaredTotal > maxBytes) {
            throw new BusinessException(ErrorCode.FILE_TOTAL_SIZE_EXCEEDED);
        }

        Map<Long, String> encoded = new LinkedHashMap<>();
        long actualTotal = 0;
        for (Long fileId : distinctIds) {
            byte[] bytes = s3Service.downloadFile(fileById.get(fileId).getS3Key());
            // file_size가 비어 있거나 실제와 다른 경우를 대비해 실제 바이트로 한 번 더 확인
            actualTotal += bytes.length;
            if (actualTotal > maxBytes) {
                throw new BusinessException(ErrorCode.FILE_TOTAL_SIZE_EXCEEDED);
            }
            if (!startsWith(bytes, JPEG_SIGNATURE) && !startsWith(bytes, PNG_SIGNATURE)) {
                throw new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_FORMAT);
            }
            encoded.put(fileId, Base64.getEncoder().encodeToString(bytes));
        }
        return encoded;
    }

    private static boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
