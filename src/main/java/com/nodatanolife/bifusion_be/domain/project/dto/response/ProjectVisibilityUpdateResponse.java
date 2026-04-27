package com.nodatanolife.bifusion_be.domain.project.dto.response;

import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProjectVisibilityUpdateResponse {

    private List<Item> items;

    public static ProjectVisibilityUpdateResponse from(List<ProjectMember> members) {
        return ProjectVisibilityUpdateResponse.builder()
                .items(members.stream().map(Item::from).toList())
                .build();
    }

    @Getter
    @Builder
    public static class Item {
        private Long projectId;
        private Boolean isPublic;

        public static Item from(ProjectMember member) {
            return Item.builder()
                    .projectId(member.getProject().getProjectId())
                    .isPublic(member.getIsPublic())
                    .build();
        }
    }
}
