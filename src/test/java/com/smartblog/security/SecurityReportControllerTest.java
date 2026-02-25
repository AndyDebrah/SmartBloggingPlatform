package com.smartblog.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.smartblog.auth.SecurityEventMetricsService;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityReportControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SecurityEventMetricsService securityEvents;

    @Test
    void securityReport_withAdmin_shouldReturnMetrics() throws Exception {
        securityEvents.recordLoginAttempt("alice", "127.0.0.1");
        securityEvents.recordLoginFailure("alice", "127.0.0.1", "BadCredentials");

        mvc.perform(get("/admin/security-report").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginAttempts").isNumber())
                .andExpect(jsonPath("$.data.loginFailures").isNumber())
                .andExpect(jsonPath("$.data.loginFailuresByPrincipal.alice").isNumber());
    }

    @Test
    void securityReport_withAuthor_shouldBeForbidden() throws Exception {
        mvc.perform(get("/admin/security-report").with(user("author").roles("AUTHOR")))
                .andExpect(status().isForbidden());
    }
}
