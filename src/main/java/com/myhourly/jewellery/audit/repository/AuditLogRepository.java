package com.myhourly.jewellery.audit.repository;
import com.myhourly.jewellery.audit.enums.AuditAction;

import com.myhourly.jewellery.audit.entity.AuditLog;
import com.myhourly.jewellery.audit.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a
            FROM AuditLog a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId)
              AND (:targetId IS NULL OR a.targetId = :targetId)
              AND (:fromDate IS NULL OR a.createdAt >= :fromDate)
              AND (:toDate IS NULL OR a.createdAt <= :toDate)
            """)
    Page<AuditLog> searchAuditLogs(
            @Param("action") AuditAction action,
            @Param("actorUserId") Long actorUserId,
            @Param("targetId") Long targetId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
