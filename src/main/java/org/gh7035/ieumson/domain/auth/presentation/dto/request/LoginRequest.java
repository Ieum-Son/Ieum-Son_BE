package org.gh7035.ieumson.domain.auth.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "로그인 ID를 입력해주세요.")
        @Size(max = 255, message = "로그인 ID는 255자 이하여야 합니다.")
        @JsonAlias("email")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(max = 72, message = "비밀번호는 72자 이하여야 합니다.")
        String password
) {
}
