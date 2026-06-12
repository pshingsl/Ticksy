import api from './axios';

// 이메일 중복 확인
export const checkEmail = async (email: string) => {
  const response = await api.get(
    `/auth/check-email?email=${email}`
  );
  return response.data.data; // { isDuplicate: boolean }
};

// 이메일 인증번호 발송
export const sendVerificationCode = async (email: string) => {
  const response = await api.post('/auth/email/send', { email });
  return response.data;
};

// 이메일 인증번호 확인
export const verifyEmail = async (email: string, code: string) => {
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