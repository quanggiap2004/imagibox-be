package com.imagibox.service;

import com.imagibox.domain.entity.User;
import com.imagibox.domain.enums.UserRole;
import com.imagibox.dto.request.CreateKidRequest;
import com.imagibox.dto.request.LoginRequest;
import com.imagibox.dto.request.RegisterParentRequest;
import com.imagibox.dto.response.AuthResponse;
import com.imagibox.dto.response.UserDto;
import com.imagibox.exception.UnauthorizedException;
import com.imagibox.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse registerParent(RegisterParentRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User parent = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.PARENT)
                .dailyQuota(10)
                .build();

        userRepository.save(parent);

        UserDetails userDetails = userDetailsService.loadUserByUsername(parent.getUsername());
        String token = jwtService.generateToken(userDetails, parent.getId(), parent.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .userId(parent.getId())
                .username(parent.getUsername())
                .role(parent.getRole().name())
                .message("Parent account created successfully")
                .build();
    }

    @Transactional
    public UserDto createKidAccount(CreateKidRequest request, Long parentId) {
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> new UnauthorizedException("Parent not found"));

        if (parent.getRole() != UserRole.PARENT) {
            throw new UnauthorizedException("Only parents can create kid accounts");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User kid = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.KID)
                .parent(parent)
                .dailyQuota(request.getDailyQuota() != null ? request.getDailyQuota() : 10)
                .build();

        userRepository.save(kid);

        return UserDto.builder()
                .id(kid.getId())
                .username(kid.getUsername())
                .role(kid.getRole().name())
                .dailyQuota(kid.getDailyQuota())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails, user.getId(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .message("Login successful")
                .build();
    }

    public List<UserDto> getKidsByParent(Long parentId) {
        return userRepository.findAllKidsByParent(parentId).stream()
                .map(kid -> UserDto.builder()
                        .id(kid.getId())
                        .username(kid.getUsername())
                        .role(kid.getRole().name())
                        .dailyQuota(kid.getDailyQuota())
                        .build())
                .collect(Collectors.toList());
    }
}
