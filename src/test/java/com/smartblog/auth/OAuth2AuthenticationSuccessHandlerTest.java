package com.smartblog.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private SecurityEventMetricsService securityEvents;

    @Test
    void onAuthenticationSuccess_returnsJwtAndRefreshTokenJson() throws Exception {
        OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(
                jwtService, refreshTokenService, userRepository, securityEvents);

        OAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_READER")),
                Map.of("email", "oauth@example.com"),
                "email"
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        when(jwtService.generateToken("oauth@example.com", List.of("ROLE_READER"))).thenReturn("jwt-token");
        when(refreshTokenService.createToken("oauth@example.com")).thenReturn("refresh-token");
        when(userRepository.findByUsernameOrEmail("oauth@example.com", "oauth@example.com")).thenReturn(java.util.Optional.empty());
        when(userRepository.existsByUsername("oauth")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(response.getContentAsString());
        assertEquals("jwt-token", json.get("token").asText());
        assertEquals("refresh-token", json.get("refreshToken").asText());
        org.junit.jupiter.api.Assertions.assertTrue(response.getContentType().startsWith("application/json"));
        verify(userRepository).save(any());
    }
}
