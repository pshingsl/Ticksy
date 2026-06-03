package com.Ticksy.backend.global.exception;

import com.Ticksy.backend.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // CustomException
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.warn("CustomException: code={}, message={}",
                e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.fail(
                        e.getErrorCode().getCode(),
                        e.getErrorCode().getMessage()
                ));
    }

    // @Valid 유효성 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidException(
            MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().get(0);
        String message = fieldError.getDefaultMessage();
        log.warn("ValidationException: field={}, message={}",
                fieldError.getField(), message);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(
                        ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        message
                ));
    }

    // 예상치 못한 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("UnexpectedException: {}", e.getMessage(), e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
                ));
    }
}
