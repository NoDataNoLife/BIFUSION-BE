package com.nodatanolife.bifusion_be.domain.tag.repository;

import com.nodatanolife.bifusion_be.domain.tag.entity.TagMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface TagMappingRepository extends JpaRepository<TagMapping, Long> {
    List<TagMapping> findAllByTargetTypeAndTargetId(String targetType, Long targetId);
    List<TagMapping> findAllByTargetTypeAndTargetIdIn(String targetType, List<Long> targetIds);

}
