import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LoginPage from './pages/auth/LoginPage'
import SignupPage from './pages/auth/SignupPage';
import MyPage from './pages/auth/MyPage';
import PrivateRoute from './components/PrivateRoute';

function App() {
  return (
    <BrowserRouter>
      <Routes>
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
