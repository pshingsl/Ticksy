import api from "./axios";
import { ConcertListResponse, ConcertDetail } from "../types/concert";
import { data } from "react-router-dom";

/**
 * 동기
 * 요청과 결과가 동시에 일어난다는 뜻, 즉 앞선 작업이 완전히 끝나야만 다음 작업 시작가능
 * 
 * 비동기
 * 요청과 결과가 동시에 일어나지 않음, 즉, 하나의 작업을 시작해 두고, 그 작업이 끝날때 까지
 * 기다리지 않고 다음 작업을 시작하는 병렬식 구조
 * 
 * 자바에서는 일반적으로 위에서부터 아래로 실행하는 구조를 동기
 * 웹 프론트에서 서버에 데이터를 요청하는 axios.get()이나, 백엔드에서 
 * 대량의 이메일을 발송하는 등의 작업은 시간이 오래걸림 -> 비동기 방식
 * 이때 비동기로 처리하지 않으면 화면이 멈춤(블로킹) 발생
 * 
 * 
 * Promise
 * 네트워크의 요청이 시간이 걸리니 성공하든 실패하든
 * 결과가 나오면 꼭 반환한다 선언하는 객체
 * 
 * async
 * 비동기 함수 선언이며 함수 내부에서는 시간이 걸리는 비동기 작업
 * 존재한다는 것을 컴퓨터에게 알려주는 키워드
 * 이 키워드가 붙여진 메소드는 무조건 Promise를 반환
 * 
 * await
 * async 함수 내부에서만 쓸 수 있으며
 * "네트워크에서 데이터가 완전히 올 때까지 다음 줄로 넘어가지 말고 여기서 잠깐 기다려"라는 의미
 * 
 */

/**
 * 공연 목록에서 params: { date?: string; } 사용 이유
 * 해당 메서드는 함수를 호출할때 매개변수로 하나의 객체르 받겠다는 의미
 * ?는 사용자가 날짜를 지정하지 않고 그냥 서울 지역의 공연만 보고 싶을 수도 있고,
 * 페이지 번호 없이 기본 목록만 요청 같이 선택적 지정을 위해 에러를 발생하지 않고 사용하기 위해서이다.
 * 
 */
// 공연 목록 조회
export const getConcertList = async (params: {
  date?: string;
  region?: string;
  // sort?: string; 구현 보류로 인한 주석 처리
  page?: number;
  size?: number;
}): Promise<ConcertListResponse> => {
  const response = await api.get('/concerts', { params });
  return response.data.data;
}


/**
 * 검색 공연 목록 조회과 달리  async (keyword:string, page=0, size=10); 한 이유
 * keyword 하나로 명확하고 필수적 
 * 매개변수의 개수가 적을 때는 굳이 객체(params: {})로 감싸필요 없음
 * 독립된 매개변수로 나열하는 것이 호출할 때 searchConcerts('아이유', 0, 10) 형태로 
 * 직관적이기 때문에 섞어 쓴 것
 * 매개변수가 3개 이상일때 공연목록 처럼 사용하면 좋음(사람 취향 차이)
 */
// 공연 검색
export const searchConcerts = async (
  keyword: string,
  page = 0,
  size = 10
): Promise<ConcertListResponse> => {
  const response = await api.get('/concerts/search', { params: { keyword, page, size }, });
  return response.data.data;
}


/**
 * ``을 사용한 이유 -> 자바스크립트의 템플릿 리터럴(Template Literal)
 * 반 따옴표('나 ")는 문자열 내부에 변수를 집어넣으려면 '/concerts/' + concertId 처럼 더하기 기호로 지저분하게 연결
 * 따라서 ``을 사용하면 자열 중간에 ${변수명}을 사용해 문자열과 자바스크립트 변수를 자연스럽게 섞어서 조립
 */
// 공연 상세 조회
export const getConcertDetail = async (
  concertId: number
): Promise<ConcertDetail> => {
  const response = await api.get(`/concerts/${concertId}`);
  return response.data.data;
}
