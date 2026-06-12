import { Navigate, Outlet } from "react-router-dom";
import { useAuthStore } from "../store/authStore";

export default function PrivateRoute() {
  const { isLoggedIn } = useAuthStore();

  if (!isLoggedIn) {
    alert('로그인 해주세요.');
    return <Navigate to="/login" replace />;
  }

  return <Outlet />
}