package com.smartblog.auth;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartblog.application.service.UserService;
import com.smartblog.core.dto.UserDTO;
import com.smartblog.core.dto.request.UserCreateRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@Profile("!test")
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RevocationService revocationService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final SecurityEventMetricsService securityEvents;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, RevocationService revocationService,
                          RefreshTokenService refreshTokenService, UserService userService,
                          SecurityEventMetricsService securityEvents) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.revocationService = revocationService;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.securityEvents = securityEvents;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserCreateRequest request) {
        String role = request.role() == null || request.role().isBlank() ? "READER" : request.role();
        userService.register(request.username(), request.email(), request.password(), role);

        UserDTO created = userService.findByUsername(request.username())
                .orElseThrow(() -> new IllegalStateException("User registration failed"));

        String token = jwtService.generateToken(created.username(), List.of("ROLE_" + created.role()));
        String refresh = refreshTokenService.createToken(created.username());
        return ResponseEntity.status(201).body(new AuthResponse(token, refresh));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        String attemptedPrincipal = request.getUsername();
        String remoteIp = httpRequest.getRemoteAddr();
        securityEvents.recordLoginAttempt(attemptedPrincipal, remoteIp);

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            securityEvents.recordLoginFailure(attemptedPrincipal, remoteIp, ex.getClass().getSimpleName());
            throw ex;
        }

        Object principal = auth.getPrincipal();
        String username;
        List<String> roles;

        if (principal instanceof UserDetails) {
            UserDetails ud = (UserDetails) principal;
            username = ud.getUsername();
            roles = ud.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        } else {
            username = String.valueOf(principal);
            roles = List.of();
        }

        String token = jwtService.generateToken(username, roles);
        String refresh = refreshTokenService.createToken(username);
        securityEvents.recordLoginSuccess(username, remoteIp);
        return ResponseEntity.ok(new AuthResponse(token, refresh));
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revokeToken(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @RequestBody(required = false) AuthRequest maybeToken) {
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }
        if (token == null && maybeToken != null && maybeToken.getPassword() != null) {
            // allow token in password field for simplicity (token passed in body)
            token = maybeToken.getPassword();
        }
        if (token == null) {
            return ResponseEntity.badRequest().body("No token provided");
        }

        if (!jwtService.validateToken(token)) {
            return ResponseEntity.badRequest().body("Invalid token");
        }

        java.time.Instant expiry = jwtService.getExpirationInstant(token);
        if (expiry == null) {
            return ResponseEntity.badRequest().body("Unable to determine token expiry");
        }

        revocationService.revoke(token, expiry);
        securityEvents.recordTokenRevocation(jwtService.getUsernameFromToken(token), "revoke_endpoint");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("No Authorization header provided");
        }
        String token = authorization.substring(7);
        if (!jwtService.validateToken(token)) {
            return ResponseEntity.badRequest().body("Invalid token");
        }
        java.time.Instant expiry = jwtService.getExpirationInstant(token);
        revocationService.revoke(token, expiry != null ? expiry : java.time.Instant.now());
        securityEvents.recordTokenRevocation(jwtService.getUsernameFromToken(token), "logout_endpoint");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody(required = true) AuthRequest body) {
        String refresh = body.getPassword();
        if (refresh == null) {
            securityEvents.recordRefreshFailure("missing_refresh_token");
            return ResponseEntity.badRequest().body("Refresh token required");
        }
        String username = refreshTokenService.validateAndConsume(refresh);
        if (username == null) {
            securityEvents.recordRefreshFailure("invalid_or_expired_refresh");
            return ResponseEntity.status(401).body("Invalid or expired refresh token");
        }

        // Create new access token and rotate refresh token
        String token = jwtService.generateToken(username, java.util.List.of());
        String newRefresh = refreshTokenService.createToken(username);
        securityEvents.recordRefreshSuccess(username);
        return ResponseEntity.ok(new AuthResponse(token, newRefresh));
    }
}
