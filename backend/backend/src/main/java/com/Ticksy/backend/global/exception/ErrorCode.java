package com.Ticksy.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력값이 올바르지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),

    // 회원
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),
    ALREADY_REGISTERED_EMAIL(HttpStatus.BAD_REQUEST, "ALREADY_REGISTERED_EMAIL", "이미 가입된 이메일입니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "NOT_FOUND_USER", "존재하지 않는 회원입니다."),
    DELETED_USER(HttpStatus.UNAUTHORIZED, "DELETED_USER", "탈퇴한 회원입니다."),
    HAS_ACTIVE_RESERVATION(HttpStatus.BAD_REQUEST, "HAS_ACTIVE_RESERVATION", "확정된 예매 내역이 있어 탈퇴가 불가능합니다. 예매를 먼저 취소해주세요."),
    
    // TODO: 소셜 로그인 구현 시 에러코드 주석 해제
    // SOCIAL_LOGIN_USER(HttpStatus.BAD_REQUEST, "SOCIAL_LOGIN_USER", "소셜 로그인 계정은 해당 기능을 사용할 수 없습니다."),

    // 인증/인가
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "비밀번호 형식이 올바르지 않습니다. 8자 이상, 특수문자를 포함해야 합니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "현재 비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."),

    // JWT
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_ACCESS_TOKEN", "Access Token이 만료되었습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_TOKEN", "유효하지 않은 Access Token입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_REFRESH_TOKEN", "Refresh Token이 만료되었습니다. 다시 로그인해주세요."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다."),

    // 이메일 인증
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE", "인증번호가 일치하지 않습니다."),
    EXPIRED_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "EXPIRED_VERIFICATION_CODE", "인증번호가 만료되었습니다. 다시 요청해주세요."),
    INVALID_VERIFICATION(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION", "이메일 인증이 완료되지 않았습니다."),
    NOT_FOUND_EMAIL(HttpStatus.BAD_REQUEST, "NOT_FOUND_EMAIL", "가입되지 않은 이메일입니다."),

    // TODO: OAuth2 구현 시 에러코드 주석 해제 현재 기능이 많아서 소셜 로그인 구현 보류
    // ALREADY_REGISTERED_EMAIL_SOCIAL(HttpStatus.BAD_REQUEST, "ALREADY_REGISTERED_EMAIL", "이미 일반 가입된 이메일입니다. 이메일 로그인을 이용해주세요."),
    // SOCIAL_LOGIN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SOCIAL_LOGIN_FAILED", "소셜 로그인 처리 중 오류가 발생했습니다."),

    // 공연장
    NOT_FOUND_VENUE(HttpStatus.NOT_FOUND, "NOT_FOUND_VENUE", "존재하지 않는 공연장입니다."),

    // 공연
    NOT_FOUND_CONCERT(HttpStatus.NOT_FOUND, "NOT_FOUND_CONCERT", "존재하지 않는 공연입니다."),
    HAS_RESERVATION(HttpStatus.BAD_REQUEST, "HAS_RESERVATION", "예매 내역이 존재하여 해당 작업을 수행할 수 없습니다."),

    // TODO: 회차 에러코드 주석 처리 에러코드 기능 구현이 많아 여유시 확장 처리
    OT_FOUND_SCHEDULE(HttpStatus.NOT_FOUND, "NOT_FOUND_SCHEDULE", "존재하지 않는 회차입니다."),
    // DUPLICATE_SCHEDULE(HttpStatus.CONFLICT, "DUPLICATE_SCHEDULE", "동일 공연장에 동일 날짜/시간의 회차가 이미 존재합니다."),
    BOOKING_NOT_OPEN_YET(HttpStatus.BAD_REQUEST, "BOOKING_NOT_OPEN_YET", "아직 예매 오픈 전입니다."),

    // 좌석
    NOT_FOUND_SEAT(HttpStatus.NOT_FOUND, "NOT_FOUND_SEAT", "존재하지 않는 좌석입니다."),
    ALREADY_HELD_SEAT(HttpStatus.BAD_REQUEST, "ALREADY_HELD_SEAT", "이미 선점된 좌석입니다."),
    ALREADY_RESERVED_SEAT(HttpStatus.BAD_REQUEST, "ALREADY_RESERVED_SEAT", "이미 예매 완료된 좌석입니다."),
    EXCEED_MAX_SEAT_COUNT(HttpStatus.BAD_REQUEST, "EXCEED_MAX_SEAT_COUNT", "1인당 최대 4석까지 예매 가능합니다."),
    SEAT_NOT_HELD(HttpStatus.BAD_REQUEST, "SEAT_NOT_HELD", "선점되지 않은 좌석입니다. 좌석을 다시 선택해주세요."),
    SEAT_HOLD_EXPIRED(HttpStatus.BAD_REQUEST, "SEAT_HOLD_EXPIRED", "좌석 선점이 만료되었습니다. 좌석을 다시 선택해주세요."),

    // 예매
    NOT_FOUND_RESERVATION(HttpStatus.NOT_FOUND, "NOT_FOUND_RESERVATION", "존재하지 않는 예매입니다."),
    ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "ALREADY_CANCELLED", "이미 취소된 예매입니다."),
    CANCEL_PERIOD_EXPIRED(HttpStatus.BAD_REQUEST, "CANCEL_PERIOD_EXPIRED", "취소 가능 기간이 지났습니다."),
    RESERVATION_USER_MISMATCH(HttpStatus.FORBIDDEN, "FORBIDDEN", "본인의 예매만 조회할 수 있습니다."),

    // 결제
    PRICE_MISMATCH(HttpStatus.BAD_REQUEST, "PRICE_MISMATCH", "결제 금액이 일치하지 않습니다."),
    PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_CONFIRM_FAILED", "결제 승인에 실패했습니다. 다시 시도해주세요."),
    PAYMENT_CANCEL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_CANCEL_FAILED", "환불 처리 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
