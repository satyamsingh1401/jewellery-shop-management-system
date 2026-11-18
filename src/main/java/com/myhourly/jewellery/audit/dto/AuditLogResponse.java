package com.myhourly.jewellery.audit.dto;
import com.myhourly.jewellery.audit.enums.AuditAction;

import com.myhourly.jewellery.audit.entity.AuditLog;
import com.myhourly.jewellery.audit.enums.AuditAction;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long actorUserId,
        String actorEmail,
        AuditAction action,
        String targetType,
        Long targetId,
        String details,
        LocalDateTime createdAt
) {

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorUserId(),
                auditLog.getActorEmail(),
                auditLog.getAction(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }
}
