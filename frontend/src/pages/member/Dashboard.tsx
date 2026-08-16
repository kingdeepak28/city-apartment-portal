import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/client";
import { MemberDashboardResponse } from "../../api/types";
import { Badge, formatDate, Spinner } from "../../components/ui";

export default function Dashboard() {
  const [data, setData] = useState<MemberDashboardResponse | null>(null);

  useEffect(() => {
    api.get<MemberDashboardResponse>("/member/dashboard").then((r) => setData(r.data));
  }, []);

  if (!data) return <Spinner />;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold">Welcome, {data.memberName}</h1>
        <p className="text-sm text-slate-500">Flat {data.flatNo}</p>
      </div>

      {data.pinnedNotice && (
        <Link
          to={`/member/notices/${data.pinnedNotice.id}`}
          className="block rounded-md border border-amber-300 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-800 hover:bg-amber-100"
        >
          📌 {data.pinnedNotice.title}
        </Link>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        <div className="card p-4">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="font-semibold">Latest Notices</h2>
            <Link to="/member/notices" className="text-xs text-brand-600 hover:underline">
              View all
            </Link>
          </div>
          {data.latestNotices.length === 0 ? (
            <p className="text-sm text-slate-400">No notices yet</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {data.latestNotices.map((n) => (
                <li key={n.id} className="py-2">
                  <Link to={`/member/notices/${n.id}`} className="flex items-center justify-between text-sm hover:text-brand-700">
                    <span className="flex items-center gap-1.5">
                      {n.unread && <span className="h-2 w-2 rounded-full bg-brand-500" />}
                      {n.title}
                    </span>
                    <Badge color={n.priority === "URGENT" ? "red" : n.priority === "IMPORTANT" ? "amber" : "slate"}>
                      {n.priority}
                    </Badge>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card p-4">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="font-semibold">Recent Reports</h2>
            <Link to="/member/reports" className="text-xs text-brand-600 hover:underline">
              View all
            </Link>
          </div>
          {data.recentReports.length === 0 ? (
            <p className="text-sm text-slate-400">No reports yet</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {data.recentReports.map((r) => (
                <li key={r.id} className="py-2">
                  <Link to={`/member/reports/${r.id}`} className="flex items-center justify-between text-sm hover:text-brand-700">
                    <span>{r.title}</span>
                    <span className="text-xs text-slate-400">{formatDate(r.publishedOn)}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
