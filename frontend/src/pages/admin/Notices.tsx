// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { apiErrorMessage } from "../../api/client";
import { CategoryResponse, DocumentListItem, Page } from "../../api/types";
import { Badge, formatDate, Pagination, Spinner, statusColor } from "../../components/ui";
import { useToast } from "../../context/ToastContext";

export default function Notices() {
  const { notify } = useToast();
  const [data, setData] = useState<Page<DocumentListItem> | null>(null);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [keyword, setKeyword] = useState("");
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    const { data } = await api.get<Page<DocumentListItem>>("/admin/notices", {
      params: { page, size: 15, status: status || undefined, categoryId: categoryId || undefined, keyword: keyword || undefined },
    });
    setData(data);
    setLoading(false);
  }

  useEffect(() => {
    api.get<CategoryResponse[]>("/admin/categories", { params: { type: "NOTICE" } }).then((r) => setCategories(r.data));
  }, []);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, status, categoryId, keyword]);

  function toggle(id: string) {
    setSelected((s) => {
      const next = new Set(s);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  async function bulk(action: "publish" | "archive" | "delete") {
    try {
      await api.post(`/admin/notices/bulk/${action}`, { documentIds: [...selected] });
      notify(`${selected.size} notice(s) ${action}d`, "success");
      setSelected(new Set());
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function quickAction(id: string, action: "publish" | "archive") {
    try {
      await api.post(`/admin/notices/${id}/${action}`);
      notify(`Notice ${action}ed`, "success");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function remove(id: string) {
    if (!window.confirm("Move this notice to Recycle Bin?")) return;
    try {
      await api.delete(`/admin/notices/${id}`);
      notify("Moved to recycle bin", "success");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Notices</h1>
        <Link to="/admin/notices/new" className="btn-primary">
          + Post Notice
        </Link>
      </div>

      <div className="card flex flex-wrap gap-3 p-4">
        <input className="input max-w-xs" placeholder="Search title, body..." value={keyword} onChange={(e) => setKeyword(e.target.value)} />
        <select className="input max-w-[200px]" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <select className="input max-w-[160px]" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          <option value="DRAFT">Draft</option>
          <option value="PUBLISHED">Published</option>
          <option value="ARCHIVED">Archived</option>
        </select>
        {selected.size > 0 && (
          <div className="ml-auto flex gap-2">
            <button className="btn-primary btn-sm" onClick={() => bulk("publish")}>
              Publish {selected.size}
            </button>
            <button className="btn-secondary btn-sm" onClick={() => bulk("archive")}>
              Archive
            </button>
            <button className="btn-danger btn-sm" onClick={() => bulk("delete")}>
              Delete
            </button>
          </div>
        )}
      </div>

      <div className="card overflow-x-auto">
        {loading ? (
          <Spinner />
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="p-3">
                  <input
                    type="checkbox"
                    checked={!!data?.content.length && selected.size === data.content.length}
                    onChange={(e) => setSelected(e.target.checked ? new Set(data?.content.map((d) => d.id)) : new Set())}
                  />
                </th>
                <th className="p-3">Notice #</th>
                <th className="p-3">Title</th>
                <th className="p-3">Priority</th>
                <th className="p-3">Status</th>
                <th className="p-3">Published</th>
                <th className="p-3">Expiry</th>
                <th className="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data?.content.map((d) => (
                <tr key={d.id}>
                  <td className="p-3">
                    <input type="checkbox" checked={selected.has(d.id)} onChange={() => toggle(d.id)} />
                  </td>
                  <td className="p-3 text-xs text-slate-500">{d.noticeNumber}</td>
                  <td className="p-3 font-medium">
                    <Link to={`/admin/notices/${d.id}`} className="hover:underline">
                      {d.pinned && "📌 "}
                      {d.title}
                    </Link>
                  </td>
                  <td className="p-3">
                    <Badge color={d.priority === "URGENT" ? "red" : d.priority === "IMPORTANT" ? "amber" : "slate"}>
                      {d.priority}
                    </Badge>
                  </td>
                  <td className="p-3">
                    <Badge color={statusColor(d.status)}>{d.status}</Badge>
                  </td>
                  <td className="p-3 text-xs text-slate-500">{formatDate(d.publishedOn)}</td>
                  <td className="p-3 text-xs text-slate-500">{formatDate(d.expiryAt)}</td>
                  <td className="p-3">
                    <div className="flex flex-wrap justify-end gap-1.5">
                      {d.status !== "PUBLISHED" && (
                        <button className="btn-primary btn-sm" onClick={() => quickAction(d.id, "publish")}>
                          Publish
                        </button>
                      )}
                      {d.status === "PUBLISHED" && (
                        <>
                          <Link to={`/admin/notices/${d.id}/read-report`} className="btn-secondary btn-sm">
                            Read Report
                          </Link>
                          <button className="btn-secondary btn-sm" onClick={() => quickAction(d.id, "archive")}>
                            Archive
                          </button>
                        </>
                      )}
                      <button className="btn-danger btn-sm" onClick={() => remove(d.id)}>
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {data?.content.length === 0 && (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-slate-400">
                    No notices found
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
