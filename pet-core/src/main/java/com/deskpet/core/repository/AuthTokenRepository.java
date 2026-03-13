package com.deskpet.core.repository;

import com.deskpet.core.model.AuthToken;
import com.deskpet.core.model.AuthTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByTokenHash(String tokenHash);

    List<AuthToken> findAllByUser_IdAndTokenTypeAndUsedAtIsNull(Long userId, AuthTokenType tokenType);
}
