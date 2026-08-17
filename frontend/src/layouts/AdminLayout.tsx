import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import api from "../api/client";
import { Footer } from "../components/ui";

const navItems = [
  { to: "/admin", label: "Dashboard", end: true },
  { to: "/admin/approvals", label: "Pending Approvals" },
  { to: "/admin/users", label: "Users" },
  { to: "/admin/reports", label: "Reports" },
  { to: "/admin/notices", label: "Notices" },
  { to: "/admin/categories", label: "Categories" },
  { to: "/admin/broadcast", label: "Broadcast" },
  { to: "/admin/notification-log", label: "Notification Log" },
  { to: "/admin/recycle-bin", label: "Recycle Bin" },
  { to: "/admin/audit-log", label: "Audit Log" },
  { to: "/admin/admins", label: "Admin Accounts" },
  { to: "/admin/settings", label: "Settings" },
];

export default function AdminLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    api
      .get("/admin/dashboard/unread-notifications")
      .then((r) => setUnread(r.data.unreadCount))
      .catch(() => {});
  }, []);

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-64 shrink-0 border-r border-slate-200 bg-white lg:block">
        <div className="border-b border-slate-200 px-5 py-4">
          <div className="text-lg font-bold text-brand-700">Society Portal</div>
          <div className="text-xs text-slate-400">Admin Console</div>
        </div>
        <nav className="flex flex-col gap-0.5 p-3">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `rounded-md px-3 py-2 text-sm font-medium ${
                  isActive ? "bg-brand-50 text-brand-700" : "text-slate-600 hover:bg-slate-100"
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="flex min-h-screen flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-slate-200 bg-white px-4 py-3 lg:px-6">
          <div className="text-sm text-slate-500 lg:hidden font-semibold text-brand-700">Society Portal</div>
          <div className="ml-auto flex items-center gap-4">
            <span className="relative text-lg" title={`${unread} unread`}>
              🔔
              {unread > 0 && (
                <span className="absolute -right-1.5 -top-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] text-white">
                  {unread}
                </span>
              )}
            </span>
            <div className="text-right text-sm">
              <div className="font-medium text-slate-800">{user?.name}</div>
              <div className="text-xs text-slate-400">{user?.role}</div>
            </div>
            <button
              className="btn-secondary btn-sm"
              onClick={() => {
                logout();
                navigate("/login");
              }}
            >
              Logout
            </button>
          </div>
        </header>
        <main className="flex-1 bg-slate-50 p-4 lg:p-6">
          <Outlet />
        </main>
        <Footer />
      </div>
    </div>
  );
}
