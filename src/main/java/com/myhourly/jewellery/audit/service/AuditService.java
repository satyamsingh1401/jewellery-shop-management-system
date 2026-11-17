package com.myhourly.jewellery.audit.service;

import com.myhourly.jewellery.audit.entity.AuditLog;
import com.myhourly.jewellery.audit.enums.AuditAction;
import com.myhourly.jewellery.audit.repository.AuditLogRepository;
import com.myhourly.jewellery.common.exception.BusinessException;
import com.myhourly.jewellery.security.entity.User;
import com.myhourly.jewellery.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void log(
            AuditAction action,
            String targetType,
            Long targetId,
            String details) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException(
                    "Authenticated user is required for audit logging"
            );
        }

        String email = authentication.getName();

        User actor = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + email
                ));

        AuditLog auditLog = AuditLog.builder()
                .actorUserId(actor.getId())
                .actorEmail(actor.getEmail())
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> searchAuditLogs(
            AuditAction action,
            Long actorUserId,
            Long targetId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            int size,
            String sortBy,
            String direction) {

        if (page < 0) {
            throw new BusinessException(
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new BusinessException(
                    "Page size must be between 1 and 100"
            );
        }

        if (actorUserId != null && actorUserId <= 0) {
            throw new BusinessException(
                    "Actor user ID must be positive"
            );
        }

        if (targetId != null && targetId <= 0) {
            throw new BusinessException(
                    "Target ID must be positive"
            );
        }

        if (fromDate != null && toDate != null
                && fromDate.isAfter(toDate)) {
            throw new BusinessException(
                    "From date cannot be after To date"
            );
        }

        Set<String> allowedSortFields = Set.of(
                "id",
                "action",
                "createdAt",
                "actorUserId",
                "targetId"
        );

        if (!allowedSortFields.contains(sortBy)) {
            throw new BusinessException(
                    "Invalid sorting field: " + sortBy
            );
        }

        Sort.Direction sortDirection;

        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    "Sort direction must be ASC or DESC"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        return auditLogRepository.searchAuditLogs(
                action,
                actorUserId,
                targetId,
                fromDate,
                toDate,
                pageable
        );
    }

}
