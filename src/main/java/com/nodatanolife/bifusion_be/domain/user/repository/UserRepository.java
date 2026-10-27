package com.nodatanolife.bifusion_be.domain.user.repository;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // 일반 회원(USER)만 올린다. 이미 ADMIN이거나 탈퇴 계정(DELETED)은 건드리지 않는다.
    @Modifying
    @Query("UPDATE User u SET u.role = 'ADMIN' WHERE u.email IN :emails AND u.role = 'USER'")
    int promoteToAdmin(@Param("emails") Collection<String> emails);
}
