package com.myhourly.jewellery.security.service;

import com.myhourly.jewellery.security.entity.RevokedToken;
import com.myhourly.jewellery.security.jwt.JwtService;
import com.myhourly.jewellery.security.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RevokedTokenService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtService jwtService;

    @Transactional
    public void revokeToken(String token) {

        String jti = jwtService.extractTokenId(token);

        // Prevent duplicate revocation records.
        if (revokedTokenRepository.existsByJti(jti)) {
            return;
        }

        Instant expiresAt = jwtService.extractExpiration(token);

        RevokedToken revokedToken = RevokedToken.builder()
                .jti(jti)
                .expiresAt(expiresAt)
                .build();

        revokedTokenRepository.save(revokedToken);
    }

    @Transactional(readOnly = true)
    public boolean isTokenRevoked(String jti) {
        return revokedTokenRepository.existsByJti(jti);
    }
}
