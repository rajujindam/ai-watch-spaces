package com.aiwatchspaces.ai_watch_spaces.controller;

import com.aiwatchspaces.ai_watch_spaces.dto.LoginRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.LoginResponse;
import com.aiwatchspaces.ai_watch_spaces.dto.RegisterRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.UserResponse;
import com.aiwatchspaces.ai_watch_spaces.service.AuthService;
import com.aiwatchspaces.ai_watch_spaces.service.UserService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        UserResponse response = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        System.out.println("In login");
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

}