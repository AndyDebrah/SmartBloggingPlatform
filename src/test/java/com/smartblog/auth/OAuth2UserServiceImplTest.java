package com.smartblog.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.smartblog.core.model.User;
import com.smartblog.core.model.UserRole;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class OAuth2UserServiceImplTest {

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    private OAuth2UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OAuth2UserServiceImpl(userRepository, passwordEncoder, delegate);
    }

    @Test
    void loadUser_newGoogleUser_persistsAndAssignsReaderRole() {
        Map<String, Object> attrs = Map.of(
                "email", "alex@example.com",
                "name", "Alex Doe"
        );
        OAuth2User providerUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attrs,
                "email"
        );

        when(delegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(providerUser);
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("alex")).thenReturn(false);
        when(passwordEncoder.encode(any(String.class))).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OAuth2User result = service.loadUser(org.mockito.Mockito.mock(OAuth2UserRequest.class));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("alex@example.com", savedUser.getValue().getEmail());
        assertEquals("alex", savedUser.getValue().getUsername());
        assertEquals(UserRole.READER, savedUser.getValue().getRole());

        assertTrue(result.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_READER")));
    }

    @Test
    void loadUser_existingUser_usesExistingRoleWithoutCreatingNewRecord() {
        Map<String, Object> attrs = Map.of(
                "email", "author@example.com",
                "name", "Author User"
        );
        OAuth2User providerUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attrs,
                "email"
        );
        User existing = User.builder()
                .username("author")
                .email("author@example.com")
                .passwordHash("hash")
                .role(UserRole.AUTHOR)
                .build();

        when(delegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(providerUser);
        when(userRepository.findByEmail("author@example.com")).thenReturn(Optional.of(existing));

        OAuth2User result = service.loadUser(org.mockito.Mockito.mock(OAuth2UserRequest.class));

        verify(userRepository, never()).save(any(User.class));
        assertTrue(result.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_AUTHOR")));
    }
}
