package com.nodatanolife.bifusion_be.domain.recruitment.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.converter.StringListConverter;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recruit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recruit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruit_id")
    private Long recruitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;


    @Column(name = "organization")
    private String organization;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Convert(converter = StringListConverter.class)
    @Column(name = "requirements", columnDefinition = "TEXT")
    private List<String> requirements = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "responsibilities", columnDefinition = "TEXT")
    private List<String> responsibilities = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "benefits", columnDefinition = "TEXT")
    private List<String> benefits = new ArrayList<>();

    @Column(name = "team_size")
    private Integer teamSize;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "is_mypage_visible")
    private Boolean isMyPageVisible;

    @Builder
    private Recruit(User user, String jobTitle, String organization, String description,
                    List<String> requirements, List<String> responsibilities, List<String> benefits,
                    Integer teamSize, LocalDate deadline) {
        this.user = user;
        this.jobTitle = jobTitle;
        this.organization = organization;
        this.description = description;
        this.requirements = requirements != null ? requirements : new ArrayList<>();
        this.responsibilities = responsibilities != null ? responsibilities : new ArrayList<>();
        this.benefits = benefits != null ? benefits : new ArrayList<>();
        this.teamSize = teamSize;
        this.deadline = deadline;
        this.isPublic = true;
        this.isMyPageVisible = true;
    }

    public static Recruit create(User user, String jobTitle, String organization, String description,
                                 List<String> requirements, List<String> responsibilities, List<String> benefits,
                                 Integer teamSize, LocalDate deadline) {
        return Recruit.builder()
                .user(user)
                .jobTitle(jobTitle)
                .organization(organization)
                .description(description)
                .requirements(requirements)
                .responsibilities(responsibilities)
                .benefits(benefits)
                .teamSize(teamSize)
                .deadline(deadline)
                .build();
    }


    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }

    /**
     * 마이페이지 활동 공개 여부 변경
     */
    public void updateMyPageVisibility(boolean visible) {
        this.isMyPageVisible = visible;
    }
}
