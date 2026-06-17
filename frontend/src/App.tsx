import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LoginPage from './pages/auth/LoginPage'
import SignupPage from './pages/auth/SignupPage';
import MyPage from './pages/auth/MyPage';
import ConcertListPage from './pages/concert/ConcertListPage';
import ConcertDetailPage from './pages/concert/ConcertDetailPage';
import PrivateRoute from './components/PrivateRoute';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 메인페이지 = 공연 목록 */}
        <Route path="/" element={<ConcertListPage />} />
        <Route path="/concerts" element={<ConcertListPage />} />
        <Route path="/concerts/:concertId" element={<ConcertDetailPage />} />

        {/* 공개 라우트 */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />

        {/* 인증 필요 라우트 */}
        <Route element={<PrivateRoute />}>
          <Route path="/mypage" element={<MyPage />} />
        </Route>
        {/* 기본경로 */}
        <Route path="/" element={<LoginPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
