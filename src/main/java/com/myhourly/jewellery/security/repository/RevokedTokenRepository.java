package com.myhourly.jewellery.security.repository;

import com.myhourly.jewellery.security.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface RevokedTokenRepository
        extends JpaRepository<RevokedToken, Long> {

    boolean existsByJti(String jti);

    void deleteAllByExpiresAtBefore(Instant expirationTime);
}
