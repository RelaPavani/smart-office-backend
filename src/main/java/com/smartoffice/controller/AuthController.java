package com.smartoffice.controller;

import com.smartoffice.dto.auth.LoginRequest;
import com.smartoffice.dto.auth.LoginResponse;
import com.smartoffice.dto.response.ApiResponse;
import com.smartoffice.security.JwtService;
import com.smartoffice.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername().trim(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtService.generateToken(authentication);

        String username;
        String role;
        if (authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            username = userPrincipal.getUsername();
            role = userPrincipal.getRole().name();
        } else {
            username = authentication.getName();
            role = jwtService.extractRole(token);
        }

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .username(username)
                .role(role)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.ok(ApiResponse.success("User details", Map.of("message", "Authenticated")));
        }

        Map<String, Object> data = Map.of(
                "id", userPrincipal.getId(),
                "username", userPrincipal.getUsername(),
                "email", userPrincipal.getEmail(),
                "role", userPrincipal.getRole().name(),
                "enabled", userPrincipal.isEnabled()
        );

        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved successfully", data));
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminOnlyEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Authorized", "Admin access granted"));
    }

    @GetMapping("/manager-only")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<String>> managerOnlyEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Authorized", "Manager access granted"));
    }

    @GetMapping("/employee-only")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<String>> employeeOnlyEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Authorized", "Employee access granted"));
    }
}
