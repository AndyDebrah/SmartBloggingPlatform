package com.smartblog.auth;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final RevocationService revocationService;
    private final SecurityEventMetricsService securityEvents;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService,
                                   RevocationService revocationService, SecurityEventMetricsService securityEvents) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.revocationService = revocationService;
        this.securityEvents = securityEvents;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // Async controller methods trigger a second ASYNC dispatch.
        // Re-run JWT auth there so SecurityContext is available for completion dispatch.
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtService.validateToken(token)) {
            securityEvents.recordTokenValidationFailure(request.getRequestURI(), "invalid_token");
            filterChain.doFilter(request, response);
            return;
        }
        if (revocationService.isRevoked(token)) {
            securityEvents.recordTokenValidationFailure(request.getRequestURI(), "revoked_token");
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtService.getUsernameFromToken(token);
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException ex) {
            securityEvents.recordTokenValidationFailure(request.getRequestURI(), "subject_not_found");
            filterChain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        securityEvents.recordTokenValidationSuccess(username, request.getRequestURI());

        filterChain.doFilter(request, response);
    }
}
