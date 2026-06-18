package org.gh7035.ieumson.global.error.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IeumException extends RuntimeException {
    private final ErrorCode errorCode;
}