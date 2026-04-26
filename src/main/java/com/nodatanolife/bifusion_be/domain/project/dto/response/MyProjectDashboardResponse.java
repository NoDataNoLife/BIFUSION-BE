package com.nodatanolife.bifusion_be.domain.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class MyProjectDashboardResponse {
    private List<ProjectDetailResponse> managingProjects;
    private List<ProjectDetailResponse> participatingProjects;
}
