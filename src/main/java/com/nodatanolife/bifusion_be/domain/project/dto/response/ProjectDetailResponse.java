package com.nodatanolife.bifusion_be.domain.project.dto.response;


import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailResponse {
    private Long projectId;
    private String title;
    private String description;
    private ProjectRole role;
    private String bannerImageUrl;
    private Boolean isFavorited;
    private Integer memberCount;
    private List<ProjectMemberResponse> members;
    private LocalDateTime lastActivityAt;
}