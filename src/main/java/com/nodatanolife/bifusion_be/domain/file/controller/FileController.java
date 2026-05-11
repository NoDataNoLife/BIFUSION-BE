package com.nodatanolife.bifusion_be.domain.file.controller;

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
}
