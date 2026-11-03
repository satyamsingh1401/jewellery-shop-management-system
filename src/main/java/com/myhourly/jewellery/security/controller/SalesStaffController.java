package com.myhourly.jewellery.security.controller;

import com.myhourly.jewellery.security.dto.CreateSalesStaffRequest;
import com.myhourly.jewellery.security.dto.SalesStaffResponse;
import com.myhourly.jewellery.security.entity.User;
import com.myhourly.jewellery.security.service.SalesStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sales_staff")
@RequiredArgsConstructor
public class SalesStaffController {

    private final SalesStaffService staffService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SalesStaffResponse> createSalesStaff(
            @Valid @RequestBody CreateSalesStaffRequest request) {

        User staff = staffService.createSalesStaff(request);

        SalesStaffResponse response = new SalesStaffResponse(
                staff.getId(),
                staff.getName(),
                staff.getEmail(),
                staff.getRole(),
                staff.isActive()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
