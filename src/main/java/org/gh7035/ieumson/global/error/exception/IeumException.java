package org.gh7035.ieumson.global.error.exception;

import lombok.Getter;

@Getter
public class IeumException extends RuntimeException {
    private final ErrorCode errorCode;

    public IeumException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public IeumException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }
}