// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/client";
import { AdminDashboardResponse } from "../../api/types";
import { Badge, formatDateTime, Spinner, StatCard, statusColor } from "../../components/ui";

export default function Dashboard() {
  const [data, setData] = useState<AdminDashboardResponse | null>(null);

  useEffect(() => {
    api.get<AdminDashboardResponse>("/admin/dashboard").then((r) => setData(r.data));
  }, []);

  if (!data) return <Spinner />;

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold">Dashboard</h1>

      {data.alerts.length > 0 && (
        <div className="space-y-2">
          {data.alerts.map((a, i) => (
            <div key={i} className="rounded-md border border-amber-200 bg-amber-50 px-4 py-2.5 text-sm text-amber-800">
              <Link to={a.link} className="hover:underline">
                {a.message}
              </Link>
            </div>
          ))}
        </div>
      )}

      <div className="grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-6">
        <StatCard label="Pending Approvals" value={data.pendingApprovals} icon="⏳" />
        <StatCard label="Active Users" value={data.totalActiveUsers} icon="👥" />
        <StatCard label="Docs Published" value={data.totalDocumentsPublished} icon="📄" />
        <StatCard label="Uploads This Month" value={data.uploadsThisMonth} icon="📤" />
        <StatCard label="Notices Live" value={data.noticesLive} icon="📢" />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <div className="card p-4">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="font-semibold">Pending Approvals</h2>
            <Link to="/admin/approvals" className="text-xs text-brand-600 hover:underline">
              View all
            </Link>
          </div>
          {data.recentPendingApprovals.length === 0 ? (
            <p className="text-sm text-slate-400">No pending approvals</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {data.recentPendingApprovals.map((u) => (
                <li key={u.id} className="flex items-center justify-between py-2 text-sm">
                  <div>
                    <div className="font-medium">{u.name}</div>
                    <div className="text-xs text-slate-400">
                      Flat {u.flatNo}, Block {u.block}
                    </div>
                  </div>
                  {u.overdue && <Badge color="red">Overdue</Badge>}
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card p-4">
          <h2 className="mb-3 font-semibold">Recently Uploaded</h2>
          {data.recentlyUploaded.length === 0 ? (
            <p className="text-sm text-slate-400">No uploads yet</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {data.recentlyUploaded.map((d) => (
                <li key={d.id} className="flex items-center justify-between py-2 text-sm">
                  <div>
                    <div className="font-medium">{d.title}</div>
                    <div className="text-xs text-slate-400">
                      {d.uploaderName} - {formatDateTime(d.timestamp)}
                    </div>
                  </div>
                  <div className="flex gap-1">
                    <Badge color="blue">{d.contentType}</Badge>
                    <Badge color={statusColor(d.status)}>{d.status}</Badge>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <div className="card p-4">
          <h2 className="mb-3 font-semibold">Uploads per Month (last 12 months)</h2>
          <div className="space-y-1.5">
            {Object.entries(data.uploadsPerMonthByType).map(([month, byType]) => {
              const total = Object.values(byType).reduce((a, b) => a + b, 0);
              const max = Math.max(1, ...Object.values(data.uploadsPerMonthByType).map((v) => Object.values(v).reduce((a, b) => a + b, 0)));
              return (
                <div key={month} className="flex items-center gap-2 text-xs">
                  <span className="w-16 shrink-0 text-slate-500">{month}</span>
                  <div className="h-3 flex-1 overflow-hidden rounded bg-slate-100">
                    <div className="h-full bg-brand-500" style={{ width: `${(total / max) * 100}%` }} />
                  </div>
                  <span className="w-6 text-right text-slate-500">{total}</span>
                </div>
              );
            })}
          </div>
        </div>

        <div className="card p-4">
          <h2 className="mb-3 font-semibold">Most Viewed Documents</h2>
          {data.mostViewed.length === 0 ? (
            <p className="text-sm text-slate-400">No activity yet</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {data.mostViewed.map((d) => (
                <li key={d.id} className="flex items-center justify-between py-2 text-sm">
                  <span className="truncate">{d.title}</span>
                  <span className="shrink-0 text-xs text-slate-400">
                    {d.viewCount} views - {d.downloadCount} downloads
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
