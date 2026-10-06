import api from "./axios";
import { ConcertListResponse, ConcertDetail } from "../types/concert";
import { data } from "react-router-dom";

// 공연 목록 조회
export const getConcertList = async (params: {
  date?: string;
  region?: string;
  page?: number;
  size?: number;
}): Promise<ConcertListResponse> => {
  const response = await api.get('/concerts', { params });
  return response.data.data;
}

// 공연 검색
export const searchConcerts = async (
  keyword: string,
  page = 0,
  size = 10
): Promise<ConcertListResponse> => {
  const response = await api.get('/concerts/search', { params: { keyword, page, size }, });
  return response.data.data;
}

// 공연 상세 조회
export const getConcertDetail = async (
  concertId: number
): Promise<ConcertDetail> => {
  const response = await api.get(`/concerts/${concertId}`);
  return response.data.data;
}
