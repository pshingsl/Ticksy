import api from './axios';

// 이메일 중복 확인
export const checkEmail = async (email: string) => {
  // get방식으로 쿼리 파라미터로 이메일을 실어서 백엔드에 요청
  const response = await api.get(
    `/auth/check-email?email=${email}`
  );

  // Axios 응답 객체(HTTP 상태코드).Axios 내부의 실제 데이터.백엔드가 실어 보낸 데이터
  // 상태코드와 백엔드의 ApiResponse객체의 ApiResponse구조 내부의 DTO를 반환
  return response.data.data; // { isDuplicate: boolean }
};

// 이메일 인증번호 발송
export const sendVerificationCode = async (email: string) => {
  // 포스트 방식으로 JSON 바디에 {이메일:값}을 담아 보낸다.
  const response = await api.post('/auth/email/send', { email });
  return response.data;
};

// 이메일 인증번호 확인
export const verifyEmail = async (email: string, code: string) => {
  // {}은 백엔드에 정의한 디티오들을 담는다.
  const response = await api.post('/auth/email/verify', {
    email,
    code,
  });
  return response.data.data; // { isVerified: boolean }
};

// 회원가입
export const signup = async (data: {
  email: string;
  password: string;
  name: string;
}) => {
  const response = await api.post('/auth/signup', data);
  return response.data.data;
};

// 로그인
export const login = async (email: string, password: string) => {
  const response = await api.post('/auth/login', { email, password });
  return response.data.data; // { accessToken, tokenType }
};

// 로그아웃
export const logout = async () => {
  const response = await api.post('/auth/logout');
  return response.data;
};

// Access Token 재발급
export const reissue = async (refreshToken: string) => {
  const response = await api.post(
    '/auth/reissue',
    {},
    { headers: { 'Refresh-Token': refreshToken } }
  );
  return response.data.data; // { accessToken }
};

// 비밀번호 변경
export const changePassword = async (
  currentPassword: string,
  newPassword: string
) => {
  const response = await api.patch('/my/password', {
    currentPassword,
    newPassword,
  });
  return response.data;
};

// 회원 탈퇴
export const withdraw = async () => {
  const response = await api.delete('/my/account');
  return response.data;
};