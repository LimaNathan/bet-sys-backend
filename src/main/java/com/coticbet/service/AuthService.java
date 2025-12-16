package com.coticbet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.coticbet.domain.entity.User;
import com.coticbet.domain.enums.Role;
import com.coticbet.dto.request.AuthRequest;
import com.coticbet.dto.request.RegisterRequest;
import com.coticbet.dto.response.AuthResponse;
import com.coticbet.exception.BusinessException;
import com.coticbet.repository.UserRepository;
import com.coticbet.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;
        private final AuthenticationManager authenticationManager;

        public AuthResponse register(RegisterRequest request) {
                log.info("[REGISTER] Iniciando registro - email={}", request.getEmail());

                if (userRepository.existsByEmail(request.getEmail())) {
                        log.warn("[REGISTER] Email já cadastrado - email={}", request.getEmail());
                        throw new BusinessException("Email já cadastrado");
                }

                // Use name from request, fallback to email prefix if null
                String displayName = request.getName();
                if (displayName == null || displayName.isBlank()) {
                        displayName = request.getEmail().split("@")[0];
                }

                User user = User.builder()
                                .email(request.getEmail())
                                .name(displayName)
                                .password(passwordEncoder.encode(request.getPassword()))
                                .role(Role.USER)
                                .walletBalance(BigDecimal.ZERO)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                user = userRepository.save(user);
                log.info("[REGISTER] Usuário criado com sucesso - userId={}, email={}, role={}",
                                user.getId(), user.getEmail(), user.getRole());

                String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
                log.debug("[REGISTER] Token JWT gerado - userId={}", user.getId());

                return AuthResponse.builder()
                                .token(token)
                                .userId(user.getId())
                                .email(user.getEmail())
                                .name(user.getName())
                                .role(user.getRole().name())
                                .build();
        }

        public AuthResponse login(AuthRequest request) {
                log.info("[LOGIN] Tentativa de login - email={}", request.getEmail());

                try {
                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(request.getEmail(),
                                                        request.getPassword()));
                        log.debug("[LOGIN] Autenticação bem-sucedida - email={}", request.getEmail());
                } catch (Exception e) {
                        log.warn("[LOGIN] Falha na autenticação - email={}, erro={}",
                                        request.getEmail(), e.getMessage());
                        throw e;
                }

                User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

                String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
                log.info("[LOGIN] Login bem-sucedido - userId={}, email={}, role={}",
                                user.getId(), user.getEmail(), user.getRole());

                return AuthResponse.builder()
                                .token(token)
                                .userId(user.getId())
                                .email(user.getEmail())
                                .name(user.getName())
                                .role(user.getRole().name())
                                .build();
        }
}
