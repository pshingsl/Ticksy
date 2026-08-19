import api from "./axios";
import { SeatLayout, SeatHoldResponse } from "../types/seat";

// 좌석 배치도 조회
export const getSeatLayout = async (
  concertId: number,
  scheduleId: number,
  sectionId?: number
): Promise<SeatLayout> => {
  const params: any = {};
  if (sectionId !== undefined) {
    params.sectionId = sectionId;
  }
  const response = await api.get(
    `/concerts/${concertId}/schedules/${scheduleId}/seats`,
    { params }
  );

  return response.data.data;
}

// 좌석 선점 요청
export const holdSeats = async (
  scheduleId: number,
  seatIds: number[]
): Promise<SeatHoldResponse> => {
  const response = await api.post(
    '/seats/hold', { scheduleId, seatIds, });
  return response.data.data;
}


// 좌석 선점 취소
export const cancelHold = async (
  scheduleId: number,
  seatIds: number[]
): Promise<void> => {
  const response = await api.delete(
    '/seats/hold', { data: { scheduleId, seatIds } });
  return response.data.data;
}
