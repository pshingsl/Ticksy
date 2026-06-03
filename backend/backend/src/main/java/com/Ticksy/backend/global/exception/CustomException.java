package com.Ticksy.backend.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException{

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessasge());
        this.errorCode = errorCode;
    }

}
