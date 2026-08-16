import { Navigate, Route, Routes } from "react-router-dom";
import { AdminRoute, MemberRoute, PublicOnlyRoute } from "./routes/guards";
import { useAuth } from "./context/AuthContext";

import Login from "./pages/public/Login";
import Register from "./pages/public/Register";
import ForgotPassword from "./pages/public/ForgotPassword";
import ResetPassword from "./pages/public/ResetPassword";

import AdminLayout from "./layouts/AdminLayout";
import AdminDashboard from "./pages/admin/Dashboard";
import Approvals from "./pages/admin/Approvals";
import Users from "./pages/admin/Users";
import Reports from "./pages/admin/Reports";
import ReportForm from "./pages/admin/ReportForm";
import Notices from "./pages/admin/Notices";
import NoticeForm from "./pages/admin/NoticeForm";
import NoticeReadReport from "./pages/admin/NoticeReadReport";
import Categories from "./pages/admin/Categories";
import Broadcast from "./pages/admin/Broadcast";
import NotificationLog from "./pages/admin/NotificationLog";
import RecycleBin from "./pages/admin/RecycleBin";
import AuditLog from "./pages/admin/AuditLog";
import AdminAccounts from "./pages/admin/AdminAccounts";
import Settings from "./pages/admin/Settings";

import MemberLayout from "./layouts/MemberLayout";
import MemberDashboard from "./pages/member/Dashboard";
import MemberReports from "./pages/member/Reports";
import MemberReportDetail from "./pages/member/ReportDetail";
import MemberNotices from "./pages/member/Notices";
import MemberNoticeDetail from "./pages/member/NoticeDetail";
import MemberNotifications from "./pages/member/Notifications";
import MemberProfile from "./pages/member/Profile";

function Home() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={user.accountType === "ADMIN" ? "/admin" : "/member"} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />

      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
      </Route>

      <Route element={<AdminRoute />}>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<AdminDashboard />} />
          <Route path="approvals" element={<Approvals />} />
          <Route path="users" element={<Users />} />
          <Route path="reports" element={<Reports />} />
          <Route path="reports/new" element={<ReportForm />} />
          <Route path="reports/:id" element={<ReportForm />} />
          <Route path="notices" element={<Notices />} />
          <Route path="notices/new" element={<NoticeForm />} />
          <Route path="notices/:id" element={<NoticeForm />} />
          <Route path="notices/:id/read-report" element={<NoticeReadReport />} />
          <Route path="categories" element={<Categories />} />
          <Route path="broadcast" element={<Broadcast />} />
          <Route path="notification-log" element={<NotificationLog />} />
          <Route path="recycle-bin" element={<RecycleBin />} />
          <Route path="audit-log" element={<AuditLog />} />
          <Route path="admins" element={<AdminAccounts />} />
          <Route path="settings" element={<Settings />} />
        </Route>
      </Route>

      <Route element={<MemberRoute />}>
        <Route path="/member" element={<MemberLayout />}>
          <Route index element={<MemberDashboard />} />
          <Route path="reports" element={<MemberReports />} />
          <Route path="reports/:id" element={<MemberReportDetail />} />
          <Route path="notices" element={<MemberNotices />} />
          <Route path="notices/:id" element={<MemberNoticeDetail />} />
          <Route path="notifications" element={<MemberNotifications />} />
          <Route path="profile" element={<MemberProfile />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
