package com.nodatanolife.bifusion_be.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> {
    private final boolean success;
    private final T data;
    private final String message;
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

    // 실패
    public static <T> CommonResponse<T> fail(String code, String message){
        return new CommonResponse<>(false, null, message, code);
    }
}

