package com.myhourly.jewellery.security.dto;

import com.myhourly.jewellery.security.enums.Role;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String name,
        String email,
        Role role
) {
}