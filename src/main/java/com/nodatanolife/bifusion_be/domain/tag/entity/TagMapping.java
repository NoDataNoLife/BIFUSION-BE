package com.nodatanolife.bifusion_be.domain.tag.entity;

import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * target_type: "RECIPE" | "DATASET" | "EXPERT_QNA" 등
 * target_id: 해당 엔티티의 PK
 */
@Entity
@Table(name = "tag_mapping")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_mapping_id")
    private Long tagMappingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Builder
    private TagMapping(Tag tag, Long targetId, String targetType) {
        this.tag = tag;
        this.targetId = targetId;
        this.targetType = targetType;
    }

    public static TagMapping create(Tag tag, Long targetId, String targetType) {
        return TagMapping.builder()
                .tag(tag)
                .targetId(targetId)
                .targetType(targetType)
                .build();
    }
}
