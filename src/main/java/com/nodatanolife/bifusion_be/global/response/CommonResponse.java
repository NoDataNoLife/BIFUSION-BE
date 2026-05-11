package com.nodatanolife.bifusion_be.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> {

    @Schema(description = "요청 성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "응답 데이터 (실패 시 null)")
    private final T data;

    @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
    private final String message;

    @Schema(description = "에러 코드 (실패 응답에만 포함)", example = "AC003", nullable = true)
    private final String code;


    private CommonResponse(boolean success, T data, String message, String code) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.code = code;
    }

    // 성공
    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<T>(true, data, null, null);
    }

    // 메시지 포함 성공
    public static <T> CommonResponse<T> success(T data, String message) {
        return new CommonResponse<>(true, data, message, null);
    }

    // 실패
    public static <T> CommonResponse<T> fail(String code, String message){
        return new CommonResponse<>(false, null, message, code);
    }
}

