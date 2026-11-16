package com.myhourly.jewellery.security.repository;

import com.myhourly.jewellery.security.entity.User;
import com.myhourly.jewellery.security.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);

    // Get all users by role
    List<User> findAllByRole(Role role);

    // Get a specific user by ID and role
    Optional<User> findByIdAndRole(Long id, Role role);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Page<User> findAllByRole(Role role, Pageable pageable);
}
