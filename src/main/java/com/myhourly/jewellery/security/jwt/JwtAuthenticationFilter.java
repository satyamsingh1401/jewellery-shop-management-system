package com.myhourly.jewellery.security.jwt;

import com.myhourly.jewellery.security.entity.User;
import com.myhourly.jewellery.security.repository.UserRepository;
import com.myhourly.jewellery.security.service.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_ERROR_MESSAGE = "authErrorMessage";
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader =
                request.getHeader("Authorization");

        // No token: continue the chain.
        // Spring Security will reject requests to protected endpoints.
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        final String token = authorizationHeader.substring(7).trim();

        // Reject an empty Bearer token.
        if (token.isEmpty()) {
            rejectRequest(
                    request,
                    response,
                    "Authentication token is required. Please log in."
            );
            return;
        }

        try {
            String email = jwtService.extractUsername(token);

            if (email == null || email.isBlank()) {
                rejectRequest(
                        request,
                        response,
                        "Invalid authentication token. Please log in again."
                );
                return;
            }

            // Do not overwrite an existing authentication.
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

            User user = userRepository
                    .findByEmailIgnoreCase(email)
                    .orElse(null);

            if (user == null) {
                rejectRequest(
                        request,
                        response,
                        "Invalid authentication token. Please log in again."
                );
                return;
            }

            // Check whether the user account is enabled.
            if (!userDetails.isEnabled()) {
                rejectRequest(
                        request,
                        response,
                        "Your account is inactive. Please contact the administrator."
                );
                return;
            }

            // Validate token signature, expiry, and user association.
            if (!jwtService.isTokenValid(token, user)) {
                rejectRequest(
                        request,
                        response,
                        "Invalid authentication token. Please log in again."
                );
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException exception) {

            SecurityContextHolder.clearContext();

            rejectRequest(
                    request,
                    response,
                    "Your session has expired. Please log in again."
            );

        } catch (JwtException | IllegalArgumentException exception) {

            SecurityContextHolder.clearContext();

            rejectRequest(
                    request,
                    response,
                    "Invalid authentication token. Please log in again."
            );

        } catch (UsernameNotFoundException exception) {

            SecurityContextHolder.clearContext();

            rejectRequest(
                    request,
                    response,
                    "Invalid authentication token. Please log in again."
            );
        }
    }

    private void rejectRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            String message) throws IOException, ServletException {

        SecurityContextHolder.clearContext();

        request.setAttribute(AUTH_ERROR_MESSAGE, message);

        authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException(message)
        );
    }
}