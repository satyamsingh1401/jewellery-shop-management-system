package com.myhourly.jewellery.security.controller;

import com.myhourly.jewellery.common.response.PageResponse;
import com.myhourly.jewellery.security.dto.CreateSalesStaffRequest;
import com.myhourly.jewellery.security.dto.SalesStaffResponse;
import com.myhourly.jewellery.security.dto.UpdateSalesStaffRequest;
import com.myhourly.jewellery.security.dto.UpdateSalesStaffStatusRequest;
import com.myhourly.jewellery.security.entity.User;
import com.myhourly.jewellery.security.service.SalesStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sales_staff")
@RequiredArgsConstructor
public class SalesStaffController {

    private final SalesStaffService staffService;

    // Create Sales Staff
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SalesStaffResponse> createSalesStaff(
            @Valid @RequestBody CreateSalesStaffRequest request) {

        User staff = staffService.createSalesStaff(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(staff));
    }

    // Get All Sales Staff
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<SalesStaffResponse>> getAllSalesStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        Page<User> staffPage = staffService.getAllSalesStaff(
                page, size, sortBy, direction
        );

        Page<SalesStaffResponse> responsePage =
                staffPage.map(this::toResponse);

        return ResponseEntity.ok(PageResponse.from(responsePage));
    }

    // Get Sales Staff By ID
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<SalesStaffResponse> getSalesStaffById(
            @PathVariable Long id) {

        User staff = staffService.getSalesStaffById(id);

        return ResponseEntity.ok(toResponse(staff));
    }

    // Update Sales Staff
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<SalesStaffResponse> updateSalesStaff(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSalesStaffRequest request) {

        User staff = staffService.updateSalesStaff(id, request);

        return ResponseEntity.ok(toResponse(staff));
    }

    // Activate / Deactivate Sales Staff
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<SalesStaffResponse> updateSalesStaffStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSalesStaffStatusRequest request) {

        User staff = staffService.updateSalesStaffStatus(id, request);

        return ResponseEntity.ok(toResponse(staff));
    }

    // Convert User entity to response DTO
    private SalesStaffResponse toResponse(User staff) {

        return SalesStaffResponse.builder()
                .id(staff.getId())
                .name(staff.getName())
                .email(staff.getEmail())
                .role(staff.getRole())
                .active(staff.isActive())
                .build();
    }
}

