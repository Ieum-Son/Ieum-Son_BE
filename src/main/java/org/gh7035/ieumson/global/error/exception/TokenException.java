package org.gh7035.ieumson.global.error.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TokenException extends IeumException {

    public TokenException(ErrorCode errorCode) {
        super(errorCode);
    }

    public TokenException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
        log.error("[TokenException] {} | cause: {}", errorCode.getErrorMessage(), cause.getMessage(), cause);
    }
}
