package com.nodatanolife.bifusion_be.domain.tag.repository;

import com.nodatanolife.bifusion_be.domain.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
}
