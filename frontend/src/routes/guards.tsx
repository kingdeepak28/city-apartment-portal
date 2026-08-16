import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export function AdminRoute() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.accountType !== "ADMIN") return <Navigate to="/member" replace />;
  return <Outlet />;
}

export function MemberRoute() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.accountType !== "USER") return <Navigate to="/admin" replace />;
  return <Outlet />;
}

export function PublicOnlyRoute() {
  const { user } = useAuth();
  if (user) return <Navigate to={user.accountType === "ADMIN" ? "/admin" : "/member"} replace />;
  return <Outlet />;
}
