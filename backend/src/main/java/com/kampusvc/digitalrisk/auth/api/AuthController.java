package com.kampusvc.digitalrisk.auth.api;

import com.kampusvc.digitalrisk.auth.dto.*;
import com.kampusvc.digitalrisk.auth.service.AuthService;
import com.kampusvc.digitalrisk.common.api.ApiResponse;
import com.kampusvc.digitalrisk.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {
        String ip = extractClientIp(servletRequest);
        AuthResponse response = authService.register(request, ip);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account and organization registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        String ip = extractClientIp(servletRequest);
        AuthResponse response = authService.login(request, ip);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        CurrentUserResponse response = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
