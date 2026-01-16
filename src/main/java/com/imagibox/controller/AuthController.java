package com.imagibox.controller;

import com.imagibox.dto.request.CreateKidRequest;
import com.imagibox.dto.request.LoginRequest;
import com.imagibox.dto.request.RegisterParentRequest;
import com.imagibox.dto.response.AuthResponse;
import com.imagibox.dto.response.UserDto;
import com.imagibox.service.AuthService;
import com.imagibox.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/register")
    @Operation(summary = "Register a new parent account")
    public ResponseEntity<AuthResponse> registerParent(@Valid @RequestBody RegisterParentRequest request) {
        return ResponseEntity.ok(authService.registerParent(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login for both parent and kid accounts")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/kids")
    @Operation(summary = "Create a kid account (Parent only)")
    public ResponseEntity<UserDto> createKidAccount(
            @Valid @RequestBody CreateKidRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long parentId = jwtService.extractUserId(token);
        return ResponseEntity.ok(authService.createKidAccount(request, parentId));
    }

    @GetMapping("/kids")
    @Operation(summary = "Get all kids for the logged-in parent")
    public ResponseEntity<List<UserDto>> getMyKids(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long parentId = jwtService.extractUserId(token);
        return ResponseEntity.ok(authService.getKidsByParent(parentId));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public ResponseEntity<UserDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return ResponseEntity.ok(UserDto.builder()
                .username(username)
                .build());
    }
}
