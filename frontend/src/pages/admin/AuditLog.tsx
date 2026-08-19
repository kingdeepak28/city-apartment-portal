// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import api from "../../api/client";
import { Page } from "../../api/types";
import { formatDateTime, Pagination, Spinner } from "../../components/ui";

interface AuditItem {
  id: string;
  actorName: string;
  actorType: string;
  module: string;
  action: string;
  recordId: string;
  ip: string;
  occurredAt: string;
}

export default function AuditLog() {
  const [data, setData] = useState<Page<AuditItem> | null>(null);
  const [page, setPage] = useState(0);
  const [module, setModule] = useState("");
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    const { data } = await api.get<Page<AuditItem>>("/admin/audit-log", { params: { page, size: 25, module: module || undefined } });
    setData(data);
    setLoading(false);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, module]);

  async function exportExcel() {
    const res = await api.get("/admin/audit-log/export", { params: { module: module || undefined }, responseType: "blob" });
    const url = URL.createObjectURL(res.data);
    const a = document.createElement("a");
    a.href = url;
    a.download = "audit-log.xlsx";
    a.click();
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Audit Log</h1>
        <button className="btn-secondary" onClick={exportExcel}>
          Export Excel
        </button>
      </div>
      <div className="card flex gap-3 p-4">
        <input className="input max-w-xs" placeholder="Filter by module (e.g. REPORT, NOTICE, APPROVAL)" value={module} onChange={(e) => setModule(e.target.value)} />
      </div>
      <div className="card overflow-x-auto">
        {loading ? (
          <Spinner />
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="p-3">Timestamp</th>
                <th className="p-3">Actor</th>
                <th className="p-3">Module</th>
                <th className="p-3">Action</th>
                <th className="p-3">Record</th>
                <th className="p-3">IP</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data?.content.map((a) => (
                <tr key={a.id}>
                  <td className="p-3 text-xs text-slate-500">{formatDateTime(a.occurredAt)}</td>
                  <td className="p-3">
                    {a.actorName || "System"} <span className="text-xs text-slate-400">({a.actorType})</span>
                  </td>
                  <td className="p-3">{a.module}</td>
                  <td className="p-3">{a.action}</td>
                  <td className="p-3 text-xs text-slate-500">{a.recordId}</td>
                  <td className="p-3 text-xs text-slate-500">{a.ip}</td>
                </tr>
              ))}
              {data?.content.length === 0 && (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-slate-400">
                    No audit entries found
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
        {data && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}
      </div>
    </div>
  );
}
