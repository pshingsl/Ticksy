package com.Ticksy.backend.domain.concert.DTO.Response;

import com.Ticksy.backend.domain.concert.Entity.EventScheduleEntity;
import com.Ticksy.backend.domain.concert.enums.ScheduleStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class ScheduleResponse {

    private Long scheduleId;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private LocalDateTime bookingOpenAt;
    private ScheduleStatus status;
    private int remainingSeatCount;

    public static ScheduleResponse of(
            EventScheduleEntity schedule,
            int remainingSeatCount
    ) {
        return ScheduleResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .eventDate(schedule.getEventDate())
                .eventTime(schedule.getEventTime())
                .bookingOpenAt(schedule.getBookingOpenAt())
                .status(schedule.getStatus())
                .remainingSeatCount(remainingSeatCount)
                .build();
    }
}
