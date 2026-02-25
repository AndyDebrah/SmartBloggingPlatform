package com.smartblog.security;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartblog.core.model.User;
import com.smartblog.core.model.UserRole;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "security.jwt.expiration-ms=1",
    "security.revocation.store=inmemory",
    "security.refresh.store=inmemory"
})
class JwtTokenValidationIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserJpaRepository userRepo;

    @Autowired
    private BCryptPasswordEncoder bCrypt;

    @BeforeEach
    void ensureAdminUser() {
        if (!userRepo.existsByUsername("devadmin")) {
            User u = User.builder()
                    .username("devadmin")
                    .email("devadmin@example.com")
                    .passwordHash(bCrypt.encode("Password123!"))
                    .role(UserRole.ADMIN)
                    .build();
            userRepo.save(u);
        }
    }

    @Test
    void register_shouldBePublic_andReturnTokens() throws Exception {
        String username = "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String body = mapper.writeValueAsString(Map.of(
                "username", username,
                "email", username + "@example.com",
                "password", "Password123!",
                "role", "READER"
        ));

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void tamperedToken_shouldReturn401() throws Exception {
        String token = loginAndExtractAccessToken();
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        mvc.perform(get("/protected/admin-test")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_shouldReturn401() throws Exception {
        String token = loginAndExtractAccessToken();
        Thread.sleep(10);

        mvc.perform(get("/protected/admin-test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String loginAndExtractAccessToken() throws Exception {
        String loginJson = mapper.writeValueAsString(Map.of(
                "username", "devadmin",
                "password", "Password123!"
        ));

        String loginResp = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = mapper.readTree(loginResp);
        return node.get("token").asText();
    }
}
