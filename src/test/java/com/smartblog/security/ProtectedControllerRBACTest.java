package com.smartblog.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProtectedControllerRBACTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void adminEndpoint_withAdmin_shouldAllow() throws Exception {
        mvc.perform(get("/protected/admin-test").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string("admin-access"));
    }

    @Test
    void adminEndpoint_withAuthor_shouldForbidden() throws Exception {
        mvc.perform(get("/protected/admin-test").with(user("author").roles("AUTHOR")))
            .andExpect(status().isForbidden());
    }

    @Test
    void authorEndpoint_withAuthor_shouldAllow() throws Exception {
        mvc.perform(get("/protected/author-test").with(user("author").roles("AUTHOR")))
            .andExpect(status().isOk())
            .andExpect(content().string("author-access"));
    }

    @Test
    void readerEndpoint_withReader_shouldAllow() throws Exception {
        mvc.perform(get("/protected/reader-test").with(user("reader").roles("READER")))
            .andExpect(status().isOk())
            .andExpect(content().string("reader-access"));
    }

    @Test
    void adminEndpoint_withoutAuth_shouldUnauthorized() throws Exception {
        mvc.perform(get("/protected/admin-test")).andExpect(status().isUnauthorized());
    }
}
