package com.nodatanolife.bifusion_be.domain.file.presign;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.nodatanolife.bifusion_be.domain.file.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * {@link PresignedUrl} 필드를 직렬화할 때 저장된 S3 값을 presigned URL로 변환한다.
 * Spring Bean으로 등록되어 {@link com.nodatanolife.bifusion_be.global.config.JacksonConfig}의
 * SpringHandlerInstantiator를 통해 S3Service가 주입된다.
 */
@Component
@RequiredArgsConstructor
public class PresignedUrlSerializer extends JsonSerializer<String> {

    private final S3Service s3Service;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(s3Service.toPresignedUrl(value));
    }
}
