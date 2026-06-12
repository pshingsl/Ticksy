// axios는 브라우저에서 백엔드 API 요청을 쉽게 보내기 위한 라이브러리
import axios from "axios";

/*
 * axios 공통 인스턴스 생성
 * 매번 서버 주소를 적지 않도록 기본 설정
 */
const api = axios.create({
  // .env 파일에 저장된 백엔드 서버 주소를 가져와 기본 주소를 설정
  baseURL: process.env.REACT_APP_API_BASE_URL,
  /* 세션 쿠키나 인증 정보를 요청에 자동으로 포함하도록 허용하는 옵션
   * - 주로 세션 로그인
   * - Refresh Token 쿠키 저장 
   * 
   * true: 쿠키 포함 허용
   * false: 쿠키 포함 안함
   */
  withCredentials: true
});

/*
 * 프론트엔드에서 백엔드로 요청이 출발하기 "직전"에 실행되는 가로채기 구역
 * -> interceptor 실행 -> Authorization 헤더 추가 -> 서버 요청
 */
api.interceptors.request.use((config) => {

   /* localStorage: 브라우저에 데이터를 저장하는 공간
    * 특징: 브라우저 꺼도 유지, 문자열 형태로 저장됨
    * 로그인 성공 시 저장한 accessToken을 가져온다.
    */
  const token = localStorage.getItem('accessToken');

  /* 토큰이 존재하면 모든 백엔드 헤더에 Bearer 토큰값 형태로 저장
   * 토큰이 없으면 로그인 안 된 상태 -> 그냥 요청 진행
   */
  if (token) {
    // Authorization 헤더 설정 Bearer: JWT 인증 방식 표준 prefix
    config.headers.Authorization = `Bearer ${token}`;
  }

  // interceptor는 반드시 config를 반환해야 한다.
  // 반환된 config가 실제 요청에 사용된다.
  return config;
})

/**
 * 응답 인터섭터(Respone Interceptor)
 * 서버 응답 후 실행되는 영역
 *  
 * 목적: 
 * - 401 Unauthorized 발생 시
 * - AccessToken 자동 재발급
 */
api.interceptors.response.use(
  // 백엔드가 200번대 성공을 응답을 보냈다면 아무 작업 없이 그대로 데이터를 통과
  (response) => response,

  // 백엔드가 에러 응답(400~500)을 보냈다면 이 오류 제어 구역으로 이동
  async (error) => {
    // 에러가 발생한 원래 정보를 기억하기 위한 변수 선언
    const originalRequest = error.config;

    // 에러 상태 코드 401 이거나 응답이 재시도 한적이 없다면 조건문 실행
    if (error.response?.status === 401 && !originalRequest._retry) {
      // 무한 루프에 빠지 않도록 '재시도 했음을 마크'
      originalRequest._retry = true;

      try {

        // 로컬 스토리지에서 새로운 Access Token을 받아올 리프레시 토큰 생성
        const refreshToken = localStorage.getItem('refreshToken');
       
        // 리프레시 토큰이 존재 하지 않다면 예외 처리
        if (!refreshToken) {
          throw new Error('No refresh token');
        }

        // 백엔드에서 리프레시 토큰 재발급할 엔드포인트에 요청
        const response = await axios.post(
         
          // 경로, 요청, 헤더로 이루어짐
          'http://localhost:8080/auth/reissue',
          {},

          // 헤더에 리프레시 토큰을 보낸다.
          { headers: { 'Refresh-Token': refreshToken } }
        );

        // 백엔드에서 새로 발급해 Access 토큰을 가져온다.
        const newToken = response.data.data.accessToken;

        // 로컬스토리지에 기존에 있던 토큰을 새로운 토큰으로 변경
        localStorage.setItem('accessToken', newToken);

        // 방금 만료되어 실패했던 원래 요청의 헤더에 새 토큰으로 변경
        originalRequest.headers.Authorization = `Bearer ${newToken}`;

        // 새 토큰으로 받은 요청을 백엔드에 재전송하여 유저가 에러를 눈치채지 못하게 처리
        return api(originalRequest);
      } catch {
        // 리프레시 토큰마저 만료되었거나 재발급에 실패했다면 보안을 위해 강제 로그아웃
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');

        // 로그인 페이지로 이동
        window.location.href = '/login';
      }
    }

    // 401 에러가 아닌 다른 에레들은 프론트엔드 컴포넌트로 에러를 그대로 반환
    return Promise.reject(error);
  }
);

// 다른 파일에서 임포트 해서 쓸 수 있도록 보낸다.
export default api;