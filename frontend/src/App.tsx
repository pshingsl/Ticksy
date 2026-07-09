import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LoginPage from './pages/auth/LoginPage'
import SignupPage from './pages/auth/SignupPage';
import MyPage from './pages/auth/MyPage';
import ConcertListPage from './pages/concert/ConcertListPage';
import ConcertDetailPage from './pages/concert/ConcertDetailPage';
import PrivateRoute from './components/PrivateRoute';
import SeatPage from './pages/seat/SeatPage';
import ReservationPreviewPage from './pages/reservation/ReservationPreviewPage';
import PaymentPage from './pages/payment/PaymentPage';
import PaymentCompletePage from './pages/payment/PaymentCompletePage';
import PaymentFailPage from './pages/payment/PaymentFailPage';

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
        <Route path="/payment/complete" element={<PaymentCompletePage />} />
        <Route path="/payment/fail" element={<PaymentFailPage />} />

        {/* 인증 필요 라우트 */}
        <Route element={<PrivateRoute />}>
          <Route path="/mypage" element={<MyPage />} />
          <Route path="/seats/:scheduleId" element={<SeatPage />} />
          <Route path="/reservations/preview" element={<ReservationPreviewPage />} />
          <Route path="/payment" element={<PaymentPage />} />
        </Route>

        {/* 기본경로 */}
        <Route path="/" element={<LoginPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
