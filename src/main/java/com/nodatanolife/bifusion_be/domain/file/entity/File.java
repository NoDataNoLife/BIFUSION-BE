package com.nodatanolife.bifusion_be.domain.file.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class File extends BaseCreatedEntity {

    public static final String TARGET_TYPE_JOB = "JOB";

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

    // 임시 보관 시작 시각. 임시 파일 정리는 이 값(없으면 created_at) 기준 1시간이다.
    // Job 실행 실패로 임시 상태로 되돌린 파일이 곧바로 정리되지 않도록 되돌린 시각으로 다시 설정한다.
    @Column(name = "temp_since")
    private LocalDateTime tempSince;

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
                .tempSince(LocalDateTime.now())
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

    /**
     * Job 실행이 시작되지 못했을 때 입력 파일을 다시 임시 상태로 되돌린다.
     * 사용자는 같은 fileId로 재시도할 수 있고, 재시도하지 않으면 임시 파일 정리 대상이 된다.
     */
    public void detachFromJob() {
        this.isTemp = true;
        this.targetType = null;
        this.targetId = null;
        this.tempSince = LocalDateTime.now();
    }
}
