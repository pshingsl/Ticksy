import { create } from 'zustand';

// 인증 형태의 데이터와 함수를 가질지 타입을 명시해두는 곳
interface AuthState {
  isLoggedIn: boolean;      // 현재 로그인 상태인지 여부
  userName: string | null;  // 로그인한 유저의 정보
  login: (accessToken: string, name: string) => void;
  logout: () => void;
}

/**
 * 전역 상태
 * 외부에서 훅 형태로 쓸 수 있게 해준다.
 */
export const useAuthStore = create<AuthState>((set) => ({

  // 초기 상태로는 access token이 실재하는지 검사를 통해 있으면 참/거짓으로 정리
  isLoggedIn: !!localStorage.getItem('accessToken'),

  // 초기 유저 이름 역시 스토리지에서 먼저 읽어 온다. 없으면 null처리
  userName: localStorage.getItem('userName'),

  // 로그인 액션: 로그인 성공했을 때 호출되어 전역 메모리와 로컬 스토리지 동기화
  login: (accessToken: string, name: string) => {
    // 로컬스토리지에 현재 토큰 저장
    localStorage.setItem('accessToken', accessToken);

    // 로컬스토리지에 현재 리프레시 토큰 저장
    localStorage.setItem('userName', name);

    // 리액트 컴포넌트로 로그인여부, 유저이름 변경을 감지하도록 상태 업데이트
    set({ isLoggedIn: true, userName: name });
  },

  // 로그아웃: 모든 흔적을 지우고 창고를 지운다.
  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userName');
    set({ isLoggedIn: false, userName: null }); // 전역 상태도 비로그인 모드로 리셋
}));