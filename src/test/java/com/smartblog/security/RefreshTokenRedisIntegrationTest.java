package com.smartblog.security;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartblog.core.model.User;
import com.smartblog.core.model.UserRole;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "security.revocation.store=inmemory",
    "security.refresh.store=inmemory"
})
public class RefreshTokenRedisIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserJpaRepository userRepo;

    @Autowired
    private BCryptPasswordEncoder bCrypt;

    @Test
    void refresh_rotates_and_invalidates_old() throws Exception {
        // ensure test user exists (in-memory DB for tests)
        if (userRepo.findByUsername("devadmin").isEmpty()) {
            User u = User.builder()
                    .username("devadmin")
                    .email("devadmin@example.com")
                    .passwordHash(bCrypt.encode("Password123!"))
                    .role(UserRole.ADMIN)
                    .build();
            userRepo.save(u);
        }

        String loginJson = mapper.writeValueAsString(Map.of("username", "devadmin", "password", "Password123!"));

    String loginResp = mvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginJson))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    JsonNode loginNode = mapper.readTree(loginResp);
    String refresh = loginNode.get("refreshToken").asText();
    assertNotNull(refresh);

    String refreshReq = mapper.writeValueAsString(Map.of("password", refresh));
    String refreshResp = mvc.perform(post("/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(refreshReq))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    JsonNode refreshed = mapper.readTree(refreshResp);
    String newRefresh = refreshed.get("refreshToken").asText();
    assertNotNull(newRefresh);
    assertNotEquals(refresh, newRefresh);

    // old refresh should be invalid now
    mvc.perform(post("/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(refreshReq))
        .andExpect(status().isUnauthorized());
    }
}
