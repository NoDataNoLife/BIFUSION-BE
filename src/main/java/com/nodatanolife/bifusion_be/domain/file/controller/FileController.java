package com.nodatanolife.bifusion_be.domain.file.controller;

import com.nodatanolife.bifusion_be.domain.file.dto.response.FileDownloadResponse;
import com.nodatanolife.bifusion_be.domain.file.dto.response.TempFileUploadResponse;
import com.nodatanolife.bifusion_be.domain.file.service.FileService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "File", description = "파일 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/")
public class FileController {

    private final FileService fileService;

    @Operation(
            summary = "임시 파일 다건 업로드",
            description = "게시글 작성 시 임시로 파일을 업로드하고 fileId를 반환합니다."
    )
    @PostMapping("files/temp")
    public CommonResponse<List<TempFileUploadResponse>> uploadTempFiles(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestPart List<MultipartFile> files) {
        List<TempFileUploadResponse> response = fileService.uploadTempFiles(
                principal.getUser().getUserId(), files);
        return CommonResponse.success(response, "파일 업로드 성공!");
    }

    @Operation(
            summary = "파일 다운로드 URL 발급",
            description = """
                    S3 파일을 직접 노출하지 않고, 만료 시간이 있는 presigned URL을 발급합니다.
                    받을 권한이 없거나 없는 파일이면 404입니다.
                    - 데이터셋 첨부 파일: 공개 데이터셋이면 누구나, 비공개·삭제된 데이터셋은 작성자와 그 데이터셋 검수를 맡은 전문가
                    - AI 작업 입력·결과 파일: 그 작업이 속한 프로젝트 멤버
                    - 그 외(임시 업로드 등): 업로드한 본인
                    """
    )
    @PostMapping("files/{fileId}/download")
    public CommonResponse<FileDownloadResponse> downloadFile(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long fileId) {
        FileDownloadResponse response = fileService.getDownloadUrl(principal.getUser().getUserId(), fileId);
        return CommonResponse.success(response, "다운로드 URL 발급 성공!");
    }
}
