package com.Ticksy.backend.domain.reservation.DTO.Response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// 예매 정보 확인 응답
@Getter
@Builder
public class ReservationPreviewResponse {
    private Long scheduleId;
    private String concertTitle;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String venueName;
    private List<SeatPreviewItem> seats;
    private Integer totalPrice;
    private LocalDateTime holdExpiredAt;

    @Getter
    @Builder
    public  static class SeatPreviewItem {
        private Long seatId;
        private String sectionName;
        private String  grade;
        private Integer rowNum;
        private Integer colNum;
        private Integer price;
    }
}
