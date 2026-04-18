package com.nodatanolife.bifusion_be.domain.user.repository;

import com.nodatanolife.bifusion_be.domain.user.entity.RefreshToken;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndIsRevokedFalse(String token);

    @Transactional
    void deleteByUser(User user);

    @Transactional
    void deleteAllByUser(User user);

}