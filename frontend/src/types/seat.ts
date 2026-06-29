export type SeatStatus = 'AVAILABLE' | 'HOLDING' | 'RESERVED';
export type SeatGrade = 'VIP' | 'R' | 'S' | 'GENERAL';

export interface SeatItem {
  seatId: number;
  rowNum: number;
  colNum: number;
  status: SeatStatus;
}

export interface SectionWithSeats {
  sectionId: number;
  name: string;
  grade: SeatGrade;
  price: number;
  seats: SeatItem[];
}

export interface SeatLayout {
  scheduleId: number;
  bookingOpenAt: string;
  sections: SectionWithSeats[];
}

export interface SeatHoldResponse {
  heldSeatIds: number[];
  expiredAt: string;
}