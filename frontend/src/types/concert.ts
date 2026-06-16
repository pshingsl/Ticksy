export interface ConcertListItem {
  concertId: number;
  title: string;
  posterUrl: string | null;
  venueName: string;
  eventDate: string | null;
  minPrice: number;
  maxPrice: number;
  hasAvailableSeat: boolean;
}

export interface ConcertListResponse {
  content: ConcertListItem[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

export interface ScheduleItem {
  scheduleId: number;
  eventDate: string;
  eventTime: string;
  bookingOpenAt: string;
  status: 'UPCOMING' | 'OPEN' | 'SOLD_OUT' | 'CLOSED';
  remainingSeatCount: number;
}

export interface GradeItem {
  grade: 'VIP' | 'R' | 'S' | 'GENERAL';
  price: number;
}

export interface ConcertDetail {
  concertId: number;
  title: string;
  cast: string;
  ageLimit: string;
  runTime: number;
  description: string;
  posterUrl: string | null;
  venueName: string;
  venueAddress: string;
  schedules: ScheduleItem[];
  grades: GradeItem[];
}