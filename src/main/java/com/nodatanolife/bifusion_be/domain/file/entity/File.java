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

    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }

    // 정적 팩토리 메서드
    public static File createTemp(User user, String fileUrl, String s3Key, String fileName, String fileType) {
        return File.builder()
                .user(user)
                .fileUrl(fileUrl)
                .s3Key(s3Key)
                .fileName(fileName)
                .fileType(fileType)
                .isTemp(true)
                .build();
    }

    public void markAsPermanent() {
        this.isTemp = false;
    }
}
