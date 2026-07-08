import api from "./axios";
import {
  ReservationPreview,
  PaymentReady,
  PaymentConfirmResult,
  ReservationListItem,
  ReservationDetail
} from '../types/reservation'

// 예매 정보 확인
export const getReservationPreview = async (
  scheduleId: number,
  seatIds: number[]
): Promise<ReservationPreview> => {
  const response = await api.get('/reservations/preview', {
    params: {
      scheduleId,
      seatIds: seatIds.join(','),
    },
  });
  return response.data.data;
};

// 결제 요청
export const requestPayment = async (
  scheduleId: number,
  seatIds: number[],
  totalPrice: number
): Promise<PaymentReady> => {
  const response = await api.post('/payments/request', {
    scheduleId,
    seatIds,
    totalPrice
  });
  return response.data.data;
};

// 결제 승인
export const confirmPayment = async (
  paymenyKey: string,
  orderId: string,
  amount: number
): Promise<PaymentConfirmResult> => {
  const response = await api.post('/payments/confirm', {
    paymenyKey,
    orderId,
    amount
  });
  return response.data.data;
};

// 예매 내역 조회
export const getMyReservations = async (): Promise<ReservationListItem[]> => {
  const response = await api.get('/my/reservations');
  return response.data.data;
};


// 에매 상세 조회
export const getReservaitonDetail = async (
  reservationId: number
): Promise<ReservationDetail> => {
  const response = await api.get(`/my/reservations/${reservationId}`);
  return response.data.data;
};

// 예매 취소
export const cancelReservation = async (
  reservationId: number
): Promise<{ refundAmount: number; cancelledAt: string }> => {
  const response = await api.delete(`/reservations/${reservationId}`);
  return response.data.data;
};