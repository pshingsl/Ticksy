export interface SeatPreviewItem {
  seatId: number;
  sectionName: string;
  grade: string;
  rowNum: number;
  colNum: number;
  price: number;
}

export interface ReservationPreview {
  scheduleId: number;
  concertTitle: string;
  eventDate: string;
  eventTime: string;
  venueName: string;
  seats: SeatPreviewItem[];
  totalPrice: number;
  holdExpiredAt: string;
}

export interface PaymentReady {
  orderId: string;
  orderName: string;
  amount: number;
  customerName: string;
  customerEmail: string;
}

export interface PaymentConfirmResult {
  reservationId: number;
  reservationCode: string;
  paidAt: string;
}

export interface ReservationListItem {
  reservationId: number;
  reservationCode: string;
  concertTitle: string;
  eventDate: string;
  eventTime: string;
  venueName: string;
  totalPrice: number;
  status: 'CONFIRMED' | 'CANCELLED';
  createdAt: string;
}

export interface ReservationDetail {
  reservationId: number;
  reservationCode: string;
  concertTitle: string;
  eventDate: string;
  eventTime: string;
  venueName: string;
  seats: SeatPreviewItem[];
  totalPrice: number;
  status: string;
  paidAt: string | null;
}