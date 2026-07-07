package org.gh7035.ieumson.global.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // MEMBER
    MEMBER_NOT_FOUND(404, "해당 유저가 존재하지 않습니다."),
    EMAIL_ALREADY_EXISTS(409, "해당 이메일이 이미 사용중입니다."),
    PASSWORD_MISMATCH(401, "비밀번호가 일치하지 않습니다."),
    EMAIL_NOT_VERIFIED(403, "이메일 인증이 필요합니다."),

    // TOKEN
    TOKEN_EXPIRED(401, "토큰이 만료되었습니다."),
    INVALID_TOKEN(401, "유효 하지 않은 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(404, "해당 RefreshToken이 존재 하지 않습니다."),
    INVALID_ROLE(401,"유효 하지 않은 역할입니다."),

    // MAIL
    AUTH_MAIL_SEND_FAILED(502, "인증 메일 전송에 실패했습니다."),

    // VERIFY
    VERIFY_CODE_EXPIRED(400, "인증 코드가 만료되었습니다."),
    VERIFY_CODE_MISMATCH(400, "인증 코드가 올바르지 않습니다."),
    EMAIL_VERIFICATION_REQUIRED(403, "이메일 코드 인증이 필요합니다.");


    private final int statusCode;
    private final String ErrorMessage;

}
