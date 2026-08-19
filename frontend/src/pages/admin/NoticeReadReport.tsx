// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import api, { apiErrorMessage } from "../../api/client";
import { AsyncButton, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";

interface ReportData {
  audienceSize: number;
  readCount: number;
  unreadCount: number;
  readUsers: { name: string; flatNo: string; block: string; email: string }[];
  unreadUsers: { name: string; flatNo: string; block: string; email: string }[];
}

export default function NoticeReadReport() {
  const { id } = useParams();
  const { notify } = useToast();
  const [data, setData] = useState<ReportData | null>(null);
  const [tab, setTab] = useState<"read" | "unread">("unread");

  useEffect(() => {
    api.get<ReportData>(`/admin/notices/${id}/read-report`).then((r) => setData(r.data));
  }, [id]);

  async function exportExcel() {
    const res = await api.get(`/admin/notices/${id}/read-report/export`, { responseType: "blob" });
    const url = URL.createObjectURL(res.data);
    const a = document.createElement("a");
    a.href = url;
    a.download = "notice-read-report.xlsx";
    a.click();
  }

  async function sendReminder() {
    try {
      await api.post(`/admin/notices/${id}/send-reminder`);
      notify("Reminder sent to users who have not read this notice", "success");
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  if (!data) return <Spinner />;

  const list = tab === "read" ? data.readUsers : data.unreadUsers;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Notice Read Report</h1>
        <div className="flex gap-2">
          <AsyncButton className="btn-secondary" onClick={exportExcel}>
            Export Excel
          </AsyncButton>
          <AsyncButton className="btn-primary" onClick={sendReminder}>
            Send Reminder to Unread
          </AsyncButton>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4">
        <div className="card p-4 text-center">
          <div className="text-2xl font-semibold">{data.audienceSize}</div>
          <div className="text-xs text-slate-500">Total Audience</div>
        </div>
        <div className="card p-4 text-center">
          <div className="text-2xl font-semibold text-emerald-600">{data.readCount}</div>
          <div className="text-xs text-slate-500">Read</div>
        </div>
        <div className="card p-4 text-center">
          <div className="text-2xl font-semibold text-amber-600">{data.unreadCount}</div>
          <div className="text-xs text-slate-500">Unread</div>
        </div>
      </div>

      <div className="card">
        <div className="flex gap-1 border-b border-slate-200 p-2">
          <button
            className={`rounded-md px-3 py-1.5 text-sm ${tab === "unread" ? "bg-brand-50 text-brand-700" : "text-slate-500"}`}
            onClick={() => setTab("unread")}
          >
            Unread ({data.unreadCount})
          </button>
          <button
            className={`rounded-md px-3 py-1.5 text-sm ${tab === "read" ? "bg-brand-50 text-brand-700" : "text-slate-500"}`}
            onClick={() => setTab("read")}
          >
            Read ({data.readCount})
          </button>
        </div>
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="p-3">Name</th>
              <th className="p-3">Flat / Block</th>
              <th className="p-3">Email</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {list.map((u, i) => (
              <tr key={i}>
                <td className="p-3">{u.name}</td>
                <td className="p-3">
                  {u.flatNo} / {u.block}
                </td>
                <td className="p-3 text-xs text-slate-500">{u.email}</td>
              </tr>
            ))}
            {list.length === 0 && (
              <tr>
                <td colSpan={3} className="p-8 text-center text-slate-400">
                  No users in this list
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
