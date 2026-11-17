package com.myhourly.jewellery.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "revoked_tokens",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_revoked_tokens_jti",
                        columnNames = "jti"
                )
        },
        indexes = {
                @Index(
                        name = "idx_revoked_tokens_expires_at",
                        columnList = "expires_at"
                )
        }
)
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jti", nullable = false, length = 36)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
