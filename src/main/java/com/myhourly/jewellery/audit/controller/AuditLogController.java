package com.myhourly.jewellery.audit.controller;

import com.myhourly.jewellery.audit.dto.AuditLogResponse;
import com.myhourly.jewellery.audit.entity.AuditLog;
import com.myhourly.jewellery.audit.enums.AuditAction;
import com.myhourly.jewellery.audit.service.AuditService;
import com.myhourly.jewellery.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService auditService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogs(

            @RequestParam(required = false)
            AuditAction action,

            @RequestParam(required = false)
            Long actorUserId,

            @RequestParam(required = false)
            Long targetId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "DESC")
            String direction) {

        Page<AuditLog> auditPage = auditService.searchAuditLogs(
                action,
                actorUserId,
                targetId,
                fromDate,
                toDate,
                page,
                size,
                sortBy,
                direction
        );

        Page<AuditLogResponse> responsePage =
                auditPage.map(AuditLogResponse::from);

        return ResponseEntity.ok(
                PageResponse.from(responsePage)
        );
    }
}
