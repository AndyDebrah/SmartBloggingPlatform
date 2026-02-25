package com.smartblog.auth;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.smartblog.core.model.User;
import com.smartblog.core.model.UserRole;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OAuth2UserServiceImpl implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    @Autowired
    public OAuth2UserServiceImpl(UserJpaRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, new DefaultOAuth2UserService());
    }

    OAuth2UserServiceImpl(UserJpaRepository userRepository, PasswordEncoder passwordEncoder,
                          OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.delegate = delegate;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        Map<String, Object> attrs = oauth2User.getAttributes();
        String email = Optional.ofNullable((String) attrs.get("email")).orElse("unknown@unknown");
        String name = Optional.ofNullable((String) attrs.get("name")).orElse("oauth-user");

        // Find or create local user by email
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            String usernameCandidate = email.split("@")[0];
            String username = usernameCandidate;
            int attempt = 0;
            while (userRepository.existsByUsername(username)) {
                attempt++;
                username = usernameCandidate + "-" + attempt;
            }

            User u = User.builder()
                    .email(email)
                    .username(username)
                    .displayName(name)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(UserRole.READER)
                    .build();
            return userRepository.save(u);
        });

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
        return new DefaultOAuth2User(Collections.singleton(authority), attrs, "email");
    }
}
