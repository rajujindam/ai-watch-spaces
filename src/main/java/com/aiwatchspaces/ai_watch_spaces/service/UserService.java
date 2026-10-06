package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.dto.RegisterRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.UserResponse;
import com.aiwatchspaces.ai_watch_spaces.entity.User;
import com.aiwatchspaces.ai_watch_spaces.exception.UserAlreadyExistsException;
import com.aiwatchspaces.ai_watch_spaces.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {

        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException(
                    "Username is already taken"
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(
                    "Email is already registered"
            );
        }

        String hashedPassword =
                passwordEncoder.encode(request.password());

        User user = new User(
                username,
                email,
                hashedPassword
        );

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getCreatedAt()
        );
    }
}