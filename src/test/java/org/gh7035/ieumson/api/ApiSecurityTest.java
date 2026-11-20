package org.gh7035.ieumson.api;

import org.gh7035.ieumson.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiSecurityTest extends ApiTestSupport {

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/members/me",
            "/api/members/me/settings",
            "/api/progress/daily",
            "/api/study/chapters",
            "/api/study/lessons/1",
            "/api/streak",
            "/api/streak/gold",
            "/api/streak/calendar",
            "/api/favorites",
            "/api/ranking"
    })
    void getRequiresAuthentication(String path) throws Exception {
        expectUnauthorized(get(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/auth/logout",
            "/api/progress/complete",
            "/api/progress/sentences/1/answers",
            "/api/streak/recover",
            "/api/favorites/1",
            "/internal/words/1/attempts"
    })
    void postRequiresAuthentication(String path) throws Exception {
        expectUnauthorized(post(path).contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    @Test
    void patchAndDeleteRequireAuthentication() throws Exception {
        expectUnauthorized(patch("/api/members/me/rename")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"김이음"}
                        """));
        expectUnauthorized(patch("/api/members/me/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"darkMode":true}
                        """));
        expectUnauthorized(delete("/api/auth/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"password":"password12!"}
                        """));
        expectUnauthorized(delete("/api/favorites/1"));
    }

    private void expectUnauthorized(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }
}
