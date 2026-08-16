import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/client";
import { CategoryResponse, DocumentListItem } from "../../api/types";
import { Badge, formatDate, Spinner } from "../../components/ui";

export default function Notices() {
  const [tab, setTab] = useState<"active" | "archive">("active");
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [categoryId, setCategoryId] = useState("");
  const [priority, setPriority] = useState("");
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [items, setItems] = useState<DocumentListItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<CategoryResponse[]>("/member/notices/categories").then((r) => setCategories(r.data));
  }, []);

  useEffect(() => {
    setLoading(true);
    const url = tab === "active" ? "/member/notices" : "/member/notices/archive";
    api
      .get<DocumentListItem[]>(url, { params: { categoryId: categoryId || undefined, priority: priority || undefined, unreadOnly: tab === "active" ? unreadOnly : undefined } })
      .then((r) => setItems(r.data))
      .finally(() => setLoading(false));
  }, [tab, categoryId, priority, unreadOnly]);

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Notices</h1>

      <div className="flex gap-1 border-b border-slate-200">
        <button
          className={`px-4 py-2 text-sm font-medium ${tab === "active" ? "border-b-2 border-brand-600 text-brand-700" : "text-slate-500"}`}
          onClick={() => setTab("active")}
        >
          Active
        </button>
        <button
          className={`px-4 py-2 text-sm font-medium ${tab === "archive" ? "border-b-2 border-brand-600 text-brand-700" : "text-slate-500"}`}
          onClick={() => setTab("archive")}
        >
          Archive
        </button>
      </div>

      <div className="card flex flex-wrap items-center gap-3 p-4">
        <select className="input max-w-[180px]" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <select className="input max-w-[160px]" value={priority} onChange={(e) => setPriority(e.target.value)}>
          <option value="">All priorities</option>
          <option value="NORMAL">Normal</option>
          <option value="IMPORTANT">Important</option>
          <option value="URGENT">Urgent</option>
        </select>
        {tab === "active" && (
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={unreadOnly} onChange={(e) => setUnreadOnly(e.target.checked)} />
            Unread only
          </label>
        )}
      </div>

      {loading ? (
        <Spinner />
      ) : (
        <div className="card divide-y divide-slate-100">
          {items.map((n) => (
            <Link key={n.id} to={`/member/notices/${n.id}`} className="flex items-center justify-between p-4 hover:bg-slate-50">
              <div className="flex items-center gap-2">
                {n.unread && <span className="h-2 w-2 shrink-0 rounded-full bg-brand-500" />}
                {n.pinned && <span>📌</span>}
                <div>
                  <div className="font-medium">{n.title}</div>
                  <div className="text-xs text-slate-400">
                    {n.categoryName} - {formatDate(n.publishedOn)}
                    {tab === "archive" && ` - expired ${formatDate(n.expiryAt)}`}
                  </div>
                </div>
              </div>
              <Badge color={n.priority === "URGENT" ? "red" : n.priority === "IMPORTANT" ? "amber" : "slate"}>{n.priority}</Badge>
            </Link>
          ))}
          {items.length === 0 && <p className="p-12 text-center text-slate-400">No notices found</p>}
        </div>
      )}
    </div>
  );
}
