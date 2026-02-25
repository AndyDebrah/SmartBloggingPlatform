package com.smartblog.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.smartblog.core.model.User;
import com.smartblog.core.model.UserRole;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class OAuth2RBACTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    public void authorRole_allowsAuthorEndpoint() throws Exception {
        User u = User.builder()
                .username("oauth_author")
                .email("oauth_author@example.com")
                .passwordHash("x")
                .role(UserRole.AUTHOR)
                .build();

        User saved = userRepository.save(u);

        String token = jwtService.generateToken(saved.getUsername(), List.of("ROLE_" + saved.getRole().name()));

        mockMvc.perform(get("/protected/author-test").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
