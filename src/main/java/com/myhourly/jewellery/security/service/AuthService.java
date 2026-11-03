package com.myhourly.jewellery.security.service;

import com.myhourly.jewellery.security.dto.LoginRequest;
import com.myhourly.jewellery.security.dto.LoginResponse;
import com.myhourly.jewellery.security.entity.User;
import com.myhourly.jewellery.security.exception.AccountInactiveException;
import com.myhourly.jewellery.security.jwt.JwtService;
import com.myhourly.jewellery.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;


    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        Authentication authentication;
        try {


            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email().trim(),
                            request.password()
                    )
            );
        } catch (DisabledException ex) {
            throw new AccountInactiveException(
                    "Your account is inactive. Please contact the administrator."
            );
        } catch (AuthenticationException ex) {

            throw new BadCredentialsException(
                    "Invalid email or password."
            );
        }
        String email = authentication.getName();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid email or password"));

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
