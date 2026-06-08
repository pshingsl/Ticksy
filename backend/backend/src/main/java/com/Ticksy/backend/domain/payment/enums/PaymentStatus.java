package com.Ticksy.backend.domain.payment.enums;

public enum PaymentStatus {
    DONE,               // 결제 완료
    CANCELLED,          // 전액 환불
    PARTIAL_CANCELLED   // 부분 환불 (70%)
}
