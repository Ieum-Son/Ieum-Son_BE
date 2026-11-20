package org.gh7035.ieumson.api;

import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberApiTest extends ApiTestSupport {

    @Test
    void profileRenameSettingsAndImage() throws Exception {
        String loginId = uid("member");
        Member member = saveMember(loginId);
        String authorization = bearer(loginId);

        mockMvc.perform(get("/api/members/me").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.email").value(member.getEmail()))
                .andExpect(jsonPath("$.loginId").value(loginId))
                .andExpect(jsonPath("$.gold").value(0))
                .andExpect(jsonPath("$.streakCount").value(0))
                .andExpect(jsonPath("$.longestStreakCount").value(0))
                .andExpect(jsonPath("$.monthStudyCount").value(0));

        mockMvc.perform(patch("/api/members/me/rename")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"김이음"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김이음"))
                .andExpect(jsonPath("$.loginId").value(loginId));

        mockMvc.perform(patch("/api/members/me/rename")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이름을 입력해주세요."));

        mockMvc.perform(get("/api/members/me/settings").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.darkMode").value(false))
                .andExpect(jsonPath("$.alarmEnabled").value(true));

        mockMvc.perform(patch("/api/members/me/settings")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"darkMode":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.darkMode").value(true))
                .andExpect(jsonPath("$.alarmEnabled").value(true));

        mockMvc.perform(multipart("/api/members/me/profile")
                        .file(new MockMultipartFile("image", "profile.png", "image/png", PNG_BYTES))
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value(org.hamcrest.Matchers.containsString("profiles/" + loginId)));

        mockMvc.perform(get("/api/members/me").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김이음"))
                .andExpect(jsonPath("$.profileImageUrl").value(org.hamcrest.Matchers.containsString("profiles/" + loginId)));
    }
}
