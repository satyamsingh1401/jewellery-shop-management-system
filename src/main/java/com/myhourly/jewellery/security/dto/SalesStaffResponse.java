package com.myhourly.jewellery.security.dto;

import com.myhourly.jewellery.security.enums.Role;

public record SalesStaffResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean active
) {
}