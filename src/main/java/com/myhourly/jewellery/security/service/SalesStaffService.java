package com.myhourly.jewellery.security.service;

import com.myhourly.jewellery.audit.enums.AuditAction;
import com.myhourly.jewellery.audit.service.AuditService;
import com.myhourly.jewellery.common.exception.BusinessException;
import com.myhourly.jewellery.common.exception.EmailAlreadyExistsException;
import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import com.myhourly.jewellery.security.dto.CreateSalesStaffRequest;
import com.myhourly.jewellery.security.dto.UpdateSalesStaffRequest;
import com.myhourly.jewellery.security.dto.UpdateSalesStaffStatusRequest;
import com.myhourly.jewellery.security.entity.User;
import com.myhourly.jewellery.security.enums.Role;
import com.myhourly.jewellery.security.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SalesStaffService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;


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
        User savedStaff = userRepository.save(staff);
        auditService.log(
                AuditAction.SALES_STAFF_CREATED,
                "SALES_STAFF",
                savedStaff.getId(),
                "Created Sales Staff account"
        );

        return savedStaff;

    }

    // Get All Sales Staff
    @Transactional
    public Page<User> getAllSalesStaff(
            int page,
            int size,
            String sortBy,
            String direction) {

        // Validate page number
        if (page < 0) {
            throw new BusinessException("Page number cannot be negative");
        }

        // Restrict page size
        if (size < 1 || size > 100) {
            throw new BusinessException("Page size must be between 1 and 100");
        }

        // Allow only valid entity fields for sorting
        Set<String> allowedSortFields = Set.of(
                "id", "name", "email", "createdAt", "updatedAt"
        );

        if (!allowedSortFields.contains(sortBy)) {
            throw new BusinessException("Invalid sorting field: " + sortBy);
        }

        // Validate sorting direction
        Sort.Direction sortDirection;

        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Sort direction must be ASC or DESC");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        return userRepository.findAllByRole(Role.SALES_STAFF, pageable);
    }

    // Get Sales Staff By ID
    @Transactional
    public User getSalesStaffById(Long id) {

        return userRepository.findByIdAndRole(id, Role.SALES_STAFF)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Sales Staff not found with ID: " + id
                        )
                );
    }

    //    update Sales Staff
    @Transactional
    public User updateSalesStaff(Long id, UpdateSalesStaffRequest request) {

        User staff = userRepository.findByIdAndRole(id, Role.SALES_STAFF)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Sales Staff not found with ID: " + id
                        )
                );

        String name = request.name().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists"
            );
        }

        staff.setName(name);
        staff.setEmail(email);

        User updatedStaff = userRepository.save(staff);

        auditService.log(
                AuditAction.SALES_STAFF_UPDATED,
                "SALES_STAFF",
                updatedStaff.getId(),
                "Updated Sales Staff details"
        );

        return updatedStaff;
    }

    // Activate / Deactivate Sales Staff
    @Transactional
    public User updateSalesStaffStatus(
            Long id,
            UpdateSalesStaffStatusRequest request) {

        User staff = userRepository.findByIdAndRole(id, Role.SALES_STAFF)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Sales Staff not found with ID: " + id
                        )
                );

        boolean previousStatus = staff.isActive();
        boolean newStatus = request.active();

        staff.setActive(newStatus);

        User updatedStaff = userRepository.save(staff);

        // Log only when the status actually changes
        if (previousStatus != newStatus) {

            AuditAction action = newStatus
                    ? AuditAction.SALES_STAFF_ACTIVATED
                    : AuditAction.SALES_STAFF_DEACTIVATED;

            auditService.log(
                    action,
                    "SALES_STAFF",
                    updatedStaff.getId(),
                    newStatus
                            ? "Activated Sales Staff account"
                            : "Deactivated Sales Staff account"
            );
        }

        return updatedStaff;
    }
}
