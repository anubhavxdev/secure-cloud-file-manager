package com.cloudvault.backend.service;

import com.cloudvault.backend.dto.AuthRequest;
import com.cloudvault.backend.dto.AuthResponse;
import com.cloudvault.backend.dto.RegisterRequest;
import com.cloudvault.backend.dto.UserInfoResponse;
import com.cloudvault.backend.exception.UserAlreadyExistsException;
import com.cloudvault.backend.model.Role;
import com.cloudvault.backend.model.User;
import com.cloudvault.backend.repository.UserRepository;
import com.cloudvault.backend.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling user registration, login, and current-user retrieval.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register a new user.
     * Hashes the password, persists the user, and returns a JWT.
     *
     * @throws UserAlreadyExistsException if the email is already taken
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(request.email());
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                Role.USER
        );

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getName(), user.getRole().name());
    }

    /**
     * Authenticate a user with email and password.
     * Delegates credential validation to Spring Security's AuthenticationManager.
     *
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are invalid
     */
    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        // Authentication succeeded — load the full user for the response
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + request.email()));

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getName(), user.getRole().name());
    }

    /**
     * Retrieve the currently authenticated user's info from the SecurityContext.
     */
    public UserInfoResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + email));

        return new UserInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getCreatedAt().toString()
        );
    }

    /**
     * Resolve the currently authenticated user entity from the SecurityContext.
     * Useful for other services that need the full User object.
     */
    public User resolveCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + email));
    }
}
