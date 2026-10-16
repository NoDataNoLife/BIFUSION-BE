package com.nodatanolife.bifusion_be.domain.file.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class File extends BaseCreatedEntity {

    // AI 작업 입력 원본
    public static final String TARGET_TYPE_JOB = "JOB";
    // AI 작업 결과물(미리보기·다운로드용으로 ML이 우리 S3에 올린 파일). 입력 파일과 구분한다.
    public static final String TARGET_TYPE_JOB_RESULT = "JOB_RESULT";

    private static final int FILE_TYPE_MAX_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "is_temp")
    private Boolean isTemp;

    @Column(name = "s3_key")
    private String s3Key;

    @Column(name = "file_size")
    private Long fileSize;

    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }

    // 정적 팩토리 메서드
    public static File createTemp(User user, String fileUrl, String s3Key, String fileName, String fileType, Long fileSize) {
        return File.builder()
                .user(user)
                .fileUrl(fileUrl)
                .s3Key(s3Key)
                .fileName(fileName)
                .fileType(fileType)
                .fileSize(fileSize)
                .isTemp(true)
                .build();
    }

    /**
     * AI 작업 결과 파일. 처음부터 영구 파일이며 Job에 연결된다.
     * fileUrl에는 S3 key를 저장한다 (presign 시 key로 처리됨). 크기·형식은 조회에 실패하면 null.
     */
    public static File createJobResult(User owner, Long jobId, String s3Key, String contentType, Long fileSize) {
        return File.builder()
                .user(owner)
                .fileUrl(s3Key)
                .s3Key(s3Key)
                .fileName(s3Key.substring(s3Key.lastIndexOf('/') + 1))
                .fileType(contentType != null && contentType.length() <= FILE_TYPE_MAX_LENGTH ? contentType : null)
                .fileSize(fileSize)
                .isTemp(false)
                .targetType(TARGET_TYPE_JOB_RESULT)
                .targetId(jobId)
                .build();
    }

    public void markAsPermanent() {
        this.isTemp = false;
    }

    /**
     * AI 작업 입력 원본으로 사용된 임시 파일을 영구 보관으로 전환하고 Job에 연결한다.
     */
    public void attachToJob(Long jobId) {
        markAsPermanent();
        this.targetType = TARGET_TYPE_JOB;
        this.targetId = jobId;
    }
}
