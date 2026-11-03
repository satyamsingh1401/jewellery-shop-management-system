package com.myhourly.jewellery.security.service;

import com.myhourly.jewellery.security.dto.CreateSalesStaffRequest;
import com.myhourly.jewellery.security.entity.User;
import com.myhourly.jewellery.security.enums.Role;
import com.myhourly.jewellery.security.exception.BusinessException;
import com.myhourly.jewellery.security.exception.EmailAlreadyExistsException;
import com.myhourly.jewellery.security.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SalesStaffService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Transactional
    public User createSalesStaff(CreateSalesStaffRequest request) {

        String name = request.name().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String password = request.password();


        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists"
            );
        }

        int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;

        if (password.length() < 12 || passwordBytes > 72) {
            throw new BusinessException(
                    "Password must contain at least 12 characters and no more than 72 UTF-8 bytes");
        }

        User staff = new User(
                name,
                email,
                passwordEncoder.encode(password),
                Role.SALES_STAFF
        );

        return userRepository.save(staff);
    }
}
