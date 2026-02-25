package com.smartblog.auth;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartblog.core.model.User;
import com.smartblog.core.model.UserRole;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserJpaRepository userRepository;
    private final SecurityEventMetricsService securityEvents;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder fallbackPasswordEncoder = new BCryptPasswordEncoder();

    public OAuth2AuthenticationSuccessHandler(JwtService jwtService,
                                              RefreshTokenService refreshTokenService,
                                              UserJpaRepository userRepository,
                                              SecurityEventMetricsService securityEvents) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.securityEvents = securityEvents;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        String username = null;
        List<String> roles = List.of();
        if (principal instanceof OAuth2User) {
            OAuth2User u = (OAuth2User) principal;
            username = (String) u.getAttributes().getOrDefault("email", u.getName());
            roles = u.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        }

        if (username == null) username = "unknown";
        ensureLocalUser(username, principal);
        securityEvents.recordOauth2Success(username, request.getRemoteAddr());

        String token = jwtService.generateToken(username, roles);
        String refresh = refreshTokenService.createToken(username);

        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        objectMapper.writeValue(response.getWriter(), new AuthResponse(token, refresh));
    }

    private void ensureLocalUser(String usernameOrEmail, Object principal) {
        if (userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail).isPresent()) {
            return;
        }

        String email = usernameOrEmail.contains("@") ? usernameOrEmail : usernameOrEmail + "@oauth.local";
        String displayName = usernameOrEmail;

        if (principal instanceof OAuth2User oauth2User) {
            Object maybeEmail = oauth2User.getAttributes().get("email");
            if (maybeEmail instanceof String e && !e.isBlank()) {
                email = e;
            }
            Object maybeName = oauth2User.getAttributes().get("name");
            if (maybeName instanceof String n && !n.isBlank()) {
                displayName = n;
            }
        }

        String usernameCandidate = email.split("@")[0];
        String uniqueUsername = usernameCandidate;
        int attempt = 0;
        while (userRepository.existsByUsername(uniqueUsername)) {
            attempt++;
            uniqueUsername = usernameCandidate + "-" + attempt;
        }

        User user = User.builder()
                .email(email)
                .username(uniqueUsername)
                .displayName(displayName)
                .passwordHash(fallbackPasswordEncoder.encode(UUID.randomUUID().toString()))
                .role(UserRole.READER)
                .build();

        userRepository.save(user);
    }
}
