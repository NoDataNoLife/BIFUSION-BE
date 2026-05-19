package com.nodatanolife.bifusion_be.domain.tag;

import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long tagId;

    @Column(name = "name", length = 50)
    private String name;

    @Builder
    private Tag(String name) {
        this.name = name;
    }

    public static Tag create(String name) {
        return Tag.builder()
                .name(name)
                .build();
    }
}
