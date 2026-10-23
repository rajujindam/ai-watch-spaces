package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.dto.LoginRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.LoginResponse;
import com.aiwatchspaces.ai_watch_spaces.entity.User;
import com.aiwatchspaces.ai_watch_spaces.exception.InvalidCredentialsException;
import com.aiwatchspaces.ai_watch_spaces.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long expirationMs;
    private final long refreshExpirationMs;
    private final ReactiveJwtDecoder jwtDecoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs,
            ReactiveJwtDecoder jwtDecoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.jwtDecoder = jwtDecoder;
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )) {
            throw new InvalidCredentialsException(
                    "Invalid username or password"
            );
        }

        String token = generateToken(
                user,
                expirationMs,
                "access"
        );

        String refreshToken = generateToken(
                user,
                refreshExpirationMs,
                "refresh"
        );

        return new LoginResponse(token, refreshToken);

    }
    private String generateToken(
            User user,
            long expirationMs,
            String tokenType) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ai-watch-spaces")
                .issuedAt(now)
                .expiresAt(now.plus(expirationMs, ChronoUnit.MILLIS))
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("token_type", tokenType)
                .build();

        JwsHeader header =
                JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public Mono<String> refreshAccessToken(String refreshToken) {
        return jwtDecoder.decode(refreshToken)
                .flatMap(jwt -> {
                    if (!"refresh".equals(
                            jwt.getClaimAsString("token_type"))) {
                        return Mono.error(
                                new InvalidCredentialsException(
                                        "Invalid refresh token"
                                )
                        );
                    }

                    Long userId;
                    try {
                        userId = Long.parseLong(jwt.getSubject());
                    } catch (NumberFormatException ex) {
                        return Mono.error(
                                new InvalidCredentialsException(
                                        "Invalid refresh token"
                                )
                        );
                    }

                    return Mono.fromCallable(() ->
                                    userRepository.findById(userId)
                                            .orElseThrow(() ->
                                                    new InvalidCredentialsException(
                                                            "Invalid refresh token"
                                                    )
                                            )
                            )
                            .map(user -> generateToken(
                                    user,
                                    expirationMs,
                                    "access"
                            ));
                });
    }
}
