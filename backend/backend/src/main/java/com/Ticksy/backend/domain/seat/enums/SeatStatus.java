package com.Ticksy.backend.domain.seat.enums;

public enum SeatStatus {
    AVAILABLE,  // 선택 가능
    RESERVED    // 예매 완료
    // HOLDING은 Redis로만 관리 (DB에 저장 안 함)
}
