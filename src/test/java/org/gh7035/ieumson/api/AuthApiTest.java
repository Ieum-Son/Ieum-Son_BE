package org.gh7035.ieumson.api;

import org.gh7035.ieumson.domain.auth.domain.RefreshToken;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends ApiTestSupport {

    @Test
    void signupLoginRefreshLogoutAndDeleteAccount() throws Exception {
        String loginId = uid("user");
        String email = loginId + "@gmail.com";

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        String code = redisStore.get("auth:signup:code:" + email);
        assertThat(code).hasSize(6);

        mockMvc.perform(post("/api/auth/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s"}
                                """.formatted(email, code)))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/api/auth/signup")
                        .file(new MockMultipartFile("image", "profile.png", "image/png", PNG_BYTES))
                        .param("email", email)
                        .param("name", "홍길동")
                        .param("loginId", loginId)
                        .param("password", RAW_PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profileImageUrl").value(org.hamcrest.Matchers.containsString("profiles/" + loginId)));

        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"%s","password":"%s"}
                                """.formatted(loginId, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = com.jayway.jsonpath.JsonPath.read(loginBody, "$.accessToken");
        String refreshToken = com.jayway.jsonpath.JsonPath.read(loginBody, "$.refreshToken");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());

        refreshTokenRepository.save(RefreshToken.builder()
                .loginId(loginId)
                .refreshToken("replaced-token")
                .expireTime(1_000L)
                .build());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다."));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 RefreshToken이 존재 하지 않습니다."));

        String nextAccess = jwtTokenProvider.generateAccessToken(loginId);
        mockMvc.perform(delete("/api/auth/account")
                        .header("Authorization", "Bearer " + nextAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"wrong-password"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));

        mockMvc.perform(delete("/api/auth/account")
                        .header("Authorization", "Bearer " + nextAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"%s"}
                                """.formatted(RAW_PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"%s","password":"%s"}
                                """.formatted(loginId, RAW_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("로그인에 실패했습니다."));
    }

    @Test
    void verifyRejectsDuplicateEmailAndFastResend() throws Exception {
        Member member = saveMember(uid("dup"));

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(member.getEmail())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("해당 이메일이 이미 사용중입니다."));

        String email = uid("new") + "@gmail.com";
        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("인증 메일은 잠시 후 다시 요청해주세요."));
    }

    @Test
    void verifyCodeRejectsMismatchAndExpiredCode() throws Exception {
        String email = uid("code") + "@gmail.com";
        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"000000"}
                                """.formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인증 코드가 올바르지 않습니다."));

        String code = redisStore.get("auth:signup:code:" + email);
        mockMvc.perform(post("/api/auth/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s"}
                                """.formatted(email, code)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s"}
                                """.formatted(email, code)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인증 코드가 만료되었습니다."));
    }

    @Test
    void signupRequiresVerifiedEmailAndValidImage() throws Exception {
        String loginId = uid("signup");
        String email = loginId + "@gmail.com";

        mockMvc.perform(multipart("/api/auth/signup")
                        .file(new MockMultipartFile("image", "profile.png", "image/png", PNG_BYTES))
                        .param("email", email)
                        .param("name", "홍길동")
                        .param("loginId", loginId)
                        .param("password", RAW_PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("이메일 코드 인증이 필요합니다."));

        redisStore.set("auth:signup:verified:" + email, "true", java.time.Duration.ofMinutes(30));
        mockMvc.perform(multipart("/api/auth/signup")
                        .file(new MockMultipartFile("image", "note.txt", "text/plain", "hello".getBytes()))
                        .param("email", email)
                        .param("name", "홍길동")
                        .param("loginId", loginId)
                        .param("password", RAW_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("지원하지 않는 이미지 형식입니다."));
    }

    @Test
    void loginRejectsWrongPasswordUnverifiedMemberAndBlankInput() throws Exception {
        String loginId = uid("login");
        saveMember(loginId);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"%s","password":"wrong-pass"}
                                """.formatted(loginId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("로그인에 실패했습니다."));

        String unverified = uid("unverified");
        saveMember(unverified, unverified + "@gmail.com", RAW_PASSWORD, false);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"%s","password":"%s"}
                                """.formatted(unverified, RAW_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("이메일 인증이 필요합니다."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"","password":"%s"}
                                """.formatted(RAW_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("로그인 ID를 입력해주세요."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"%s","password":""}
                                """.formatted(loginId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비밀번호를 입력해주세요."));
    }

    @Test
    void refreshRejectsUnknownAndAccessToken() throws Exception {
        String loginId = uid("refresh");
        saveMember(loginId);
        String unknownRefresh = jwtTokenProvider.generateRefreshToken(loginId);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(unknownRefresh)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 RefreshToken이 존재 하지 않습니다."));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(jwtTokenProvider.generateAccessToken(loginId))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다."));
    }
}
