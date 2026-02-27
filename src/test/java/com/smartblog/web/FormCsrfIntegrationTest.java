package com.smartblog.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import jakarta.servlet.http.Cookie;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FormCsrfIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getFormThenPost_withCsrf_shouldSucceed() throws Exception {
        MvcResult get = mockMvc.perform(get("/form/csrf")).andExpect(status().isOk()).andReturn();
        String contentStr = get.getResponse().getContentAsString();
        Cookie csrfCookie = get.getResponse().getCookie("XSRF-TOKEN");
        if (csrfCookie == null) {
            throw new AssertionError("Expected XSRF-TOKEN cookie in response");
        }

        Pattern p = Pattern.compile("name=\"([^\"]+)\" value=\"([^\"]+)\"");
        Matcher m = p.matcher(contentStr);
        if (!m.find()) {
            throw new AssertionError("CSRF token input not found in form html");
        }

        String param = m.group(1);
        String token = m.group(2);

        mockMvc.perform(post("/form/submit")
                .cookie(csrfCookie)
                .param(param, token)
                .param("message", "hello"))
                .andExpect(status().isCreated())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("received:hello")));
    }

    @Test
    public void postWithoutCsrf_shouldFail() throws Exception {
        mockMvc.perform(post("/form/submit").param("message", "x")).andExpect(status().isForbidden());
    }

}
