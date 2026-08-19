// Author: deepak.maheshwari

import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import api from "../api/client";
import { Footer } from "../components/ui";

const navItems = [
  { to: "/member", label: "Dashboard", end: true },
  { to: "/member/reports", label: "Reports" },
  { to: "/member/notices", label: "Notices" },
  { to: "/member/notifications", label: "Notifications" },
  { to: "/member/profile", label: "Profile" },
];

export default function MemberLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    api
      .get("/member/notifications/unread-count")
      .then((r) => setUnread(r.data.unreadCount))
      .catch(() => {});
  }, []);

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <div className="text-lg font-bold text-brand-700">City Apartments Portal</div>
          <nav className="hidden gap-1 md:flex">
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
          <div className="flex items-center gap-4">
            <NavLink to="/member/notifications" className="relative text-lg">
              🔔
              {unread > 0 && (
                <span className="absolute -right-1.5 -top-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] text-white">
                  {unread}
                </span>
              )}
            </NavLink>
            <div className="hidden text-right text-sm sm:block">
              <div className="font-medium text-slate-800">{user?.name}</div>
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
        </div>
        <nav className="flex gap-1 overflow-x-auto border-t border-slate-100 px-4 py-1.5 md:hidden">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `whitespace-nowrap rounded-md px-3 py-1.5 text-xs font-medium ${
                  isActive ? "bg-brand-50 text-brand-700" : "text-slate-600"
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="mx-auto max-w-6xl p-4 lg:p-6">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
