import { Navigate, Outlet } from "react-router-dom";
import { useAuthStore } from "../store/authStore";

export default function PrivateRoute() {
  // 위에서 만든 zustand 창고에서 로그인 상태 표지판(isLoggedIn)을 실시간으로 가져온다.
  const { isLoggedIn } = useAuthStore();

  // 만약 비로그인을 했다면 브라우저에 경고창 띄우고 로그인페이지로 이동
  if (!isLoggedIn) {
    alert('로그인 해주세요.');
    
    // replace 옵션은 브라우저 히스토리에 현재 페이즈를 남기지 않아 뒤로가기 방지
    return <Navigate to="/login" replace />;
  }

  // 로그인된 정상 유저라면 <Outlet />을 반환하여 내부에 배치된 실제 하위 페이지 컴포넌트들을 화면에 정상 렌더링
  return <Outlet />
}