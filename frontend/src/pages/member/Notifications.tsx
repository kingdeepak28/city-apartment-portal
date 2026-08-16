import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api, { apiErrorMessage } from "../../api/client";
import { NotificationItem, Page } from "../../api/types";
import { formatDateTime, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";

const TYPE_ICON: Record<string, string> = {
  REPORT_PUBLISHED: "📄",
  NOTICE_PUBLISHED: "📢",
  NOTICE_URGENT: "🚨",
  NOTICE_REMINDER: "⏰",
  REGISTRATION_APPROVED: "✅",
  REGISTRATION_REJECTED: "❌",
  ACCOUNT_SUSPENDED: "⚠️",
  BROADCAST: "📣",
};

export default function Notifications() {
  const { notify } = useToast();
  const navigate = useNavigate();
  const [data, setData] = useState<Page<NotificationItem> | null>(null);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [loading, setLoading] = useState(true);
  const [prefs, setPrefs] = useState<Record<string, boolean> | null>(null);
  const [showPrefs, setShowPrefs] = useState(false);

  async function load() {
    setLoading(true);
    const { data } = await api.get<Page<NotificationItem>>("/member/notifications", { params: { unreadOnly: unreadOnly || undefined, size: 30 } });
    setData(data);
    setLoading(false);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [unreadOnly]);

  useEffect(() => {
    api.get<Record<string, boolean>>("/member/notifications/preferences").then((r) => setPrefs(r.data));
  }, []);

  async function open(n: NotificationItem) {
    if (!n.read) {
      await api.post(`/member/notifications/${n.id}/read`);
    }
    if (n.link) navigate(n.link);
    else load();
  }

  async function markAllRead() {
    await api.post("/member/notifications/read-all");
    load();
  }

  async function remove(id: string, e: React.MouseEvent) {
    e.stopPropagation();
    await api.delete(`/member/notifications/${id}`);
    load();
  }

  async function savePrefs() {
    if (!prefs) return;
    try {
      await api.put("/member/notifications/preferences", prefs);
      notify("Preferences saved", "success");
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Notifications</h1>
        <div className="flex gap-2">
          <button className="btn-secondary btn-sm" onClick={() => setShowPrefs((s) => !s)}>
            Preferences
          </button>
          <button className="btn-secondary btn-sm" onClick={markAllRead}>
            Mark all as read
          </button>
        </div>
      </div>

      {showPrefs && prefs && (
        <div className="card space-y-2 p-4">
          <p className="text-sm text-slate-500">Enable/disable email and SMS per content type. In-app notifications cannot be disabled.</p>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs uppercase text-slate-400">
                <th className="py-1">Type</th>
                <th className="py-1">Email</th>
                <th className="py-1">SMS</th>
              </tr>
            </thead>
            <tbody>
              {["report", "notice", "minutes", "tender"].map((cat) => (
                <tr key={cat}>
                  <td className="py-1 capitalize">{cat}</td>
                  <td className="py-1">
                    <input
                      type="checkbox"
                      checked={prefs[`email.${cat}`] ?? true}
                      onChange={(e) => setPrefs({ ...prefs, [`email.${cat}`]: e.target.checked })}
                    />
                  </td>
                  <td className="py-1">
                    <input
                      type="checkbox"
                      checked={prefs[`sms.${cat}`] ?? true}
                      onChange={(e) => setPrefs({ ...prefs, [`sms.${cat}`]: e.target.checked })}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="flex justify-end">
            <button className="btn-primary btn-sm" onClick={savePrefs}>
              Save Preferences
            </button>
          </div>
        </div>
      )}

      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" checked={unreadOnly} onChange={(e) => setUnreadOnly(e.target.checked)} />
        Show unread only
      </label>

      {loading ? (
        <Spinner />
      ) : (
        <div className="card divide-y divide-slate-100">
          {data?.content.map((n) => (
            <div
              key={n.id}
              onClick={() => open(n)}
              className={`flex cursor-pointer items-start justify-between gap-3 p-4 hover:bg-slate-50 ${!n.read ? "bg-brand-50/40" : ""}`}
            >
              <div className="flex gap-3">
                <span className="text-lg">{TYPE_ICON[n.type] || "🔔"}</span>
                <div>
                  <div className="font-medium">{n.title}</div>
                  {n.body && <div className="mt-0.5 text-sm text-slate-500 line-clamp-2">{n.body}</div>}
                  <div className="mt-1 text-xs text-slate-400">{formatDateTime(n.createdAt)}</div>
                </div>
              </div>
              <button className="shrink-0 text-xs text-slate-400 hover:text-red-500" onClick={(e) => remove(n.id, e)}>
                Delete
              </button>
            </div>
          ))}
          {data?.content.length === 0 && <p className="p-12 text-center text-slate-400">No notifications</p>}
        </div>
      )}
    </div>
  );
}
