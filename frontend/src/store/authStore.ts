import { create } from 'zustand';

interface AuthState {
  isLoggedIn: boolean;
  userName: string | null;
  login: (accessToken: string, name: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  isLoggedIn: !!localStorage.getItem('accessToken'),
  userName: localStorage.getItem('userName'),

  login: (accessToken: string, name: string) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('userName', name);
    set({ isLoggedIn: true, userName: name });
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userName');
    set({ isLoggedIn: false, userName: null });
  }
}));