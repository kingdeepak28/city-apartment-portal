// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import api, { apiErrorMessage } from "../../api/client";
import { Page } from "../../api/types";
import { AsyncButton, Badge, formatDateTime, Pagination, Spinner, statusColor } from "../../components/ui";
import { useToast } from "../../context/ToastContext";

interface LogItem {
  id: string;
  title: string;
  body: string;
  channel: string;
  deliveryStatus: string;
  sentAt: string;
  recipientName: string;
}

export default function NotificationLog() {
  const { notify } = useToast();
  const [data, setData] = useState<Page<LogItem> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    const { data } = await api.get<Page<LogItem>>("/admin/notifications/log", { params: { page, size: 20 } });
    setData(data);
    setLoading(false);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  async function resend(id: string) {
    try {
      await api.post(`/admin/notifications/log/${id}/resend`);
      notify("Failed notifications re-sent", "success");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Notification Log</h1>
      <div className="card overflow-x-auto">
        {loading ? (
          <Spinner />
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="p-3">Title</th>
                <th className="p-3">Details</th>
                <th className="p-3">Channels</th>
                <th className="p-3">Status</th>
                <th className="p-3">Sent</th>
                <th className="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data?.content.map((n) => (
                <tr key={n.id}>
                  <td className="p-3 font-medium">{n.title}</td>
                  <td className="p-3 text-xs text-slate-500">{n.body}</td>
                  <td className="p-3 text-xs">{n.channel}</td>
                  <td className="p-3">
                    <Badge color={statusColor(n.deliveryStatus)}>{n.deliveryStatus}</Badge>
                  </td>
                  <td className="p-3 text-xs text-slate-500">{formatDateTime(n.sentAt)}</td>
                  <td className="p-3 text-right">
                    <AsyncButton className="btn-secondary btn-sm" onClick={() => resend(n.id)}>
                      Resend Failed
                    </AsyncButton>
                  </td>
                </tr>
              ))}
              {data?.content.length === 0 && (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-slate-400">
                    No notifications sent yet
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
