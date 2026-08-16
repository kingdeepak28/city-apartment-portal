import { useEffect, useState } from "react";
import api, { apiErrorMessage } from "../../api/client";
import { CategoryResponse } from "../../api/types";
import { Badge, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";

const TYPES = ["REPORT", "NOTICE", "PHOTO", "MEETING", "TENDER"];

export default function Categories() {
  const { notify } = useToast();
  const [type, setType] = useState("REPORT");
  const [items, setItems] = useState<CategoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [newName, setNewName] = useState("");
  const [parentId, setParentId] = useState("");

  async function load() {
    setLoading(true);
    const { data } = await api.get<CategoryResponse[]>("/admin/categories", { params: { type } });
    setItems(data);
    setLoading(false);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [type]);

  async function create() {
    if (!newName.trim()) return;
    try {
      await api.post("/admin/categories", { type, name: newName, parentId: parentId || undefined });
      setNewName("");
      setParentId("");
      notify("Category added", "success");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function rename(id: string, name: string) {
    try {
      await api.put(`/admin/categories/${id}`, { type, name });
      notify("Renamed", "success");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function toggleActive(c: CategoryResponse) {
    try {
      await api.patch(`/admin/categories/${c.id}/active`, null, { params: { active: !c.active } });
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function remove(id: string) {
    if (!window.confirm("Delete this category?")) return;
    try {
      await api.delete(`/admin/categories/${id}`);
      notify("Category deleted", "success");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  const topLevel = items.filter((i) => !i.parentId);

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Master Categories</h1>

      <div className="flex gap-1 border-b border-slate-200">
        {TYPES.map((t) => (
          <button
            key={t}
            onClick={() => setType(t)}
            className={`px-4 py-2 text-sm font-medium ${
              type === t ? "border-b-2 border-brand-600 text-brand-700" : "text-slate-500"
            }`}
          >
            {t}
          </button>
        ))}
      </div>

      <div className="card flex flex-wrap items-end gap-3 p-4">
        <div>
          <label className="label">New category name</label>
          <input className="input" value={newName} onChange={(e) => setNewName(e.target.value)} />
        </div>
        <div>
          <label className="label">Parent (optional, one level deep)</label>
          <select className="input" value={parentId} onChange={(e) => setParentId(e.target.value)}>
            <option value="">None (top-level)</option>
            {topLevel.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
        <button className="btn-primary" onClick={create}>
          + Add
        </button>
      </div>

      <div className="card overflow-x-auto">
        {loading ? (
          <Spinner />
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="p-3">Name</th>
                <th className="p-3">Parent</th>
                <th className="p-3">Documents</th>
                <th className="p-3">Status</th>
                <th className="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {items.map((c) => (
                <tr key={c.id}>
                  <td className="p-3">
                    <input
                      className="rounded border border-transparent bg-transparent px-1 py-0.5 font-medium hover:border-slate-300 focus:border-brand-500 focus:outline-none"
                      defaultValue={c.name}
                      onBlur={(e) => e.target.value !== c.name && rename(c.id, e.target.value)}
                    />
                  </td>
                  <td className="p-3 text-slate-500">{c.parentName || "-"}</td>
                  <td className="p-3">{c.documentCount}</td>
                  <td className="p-3">
                    <Badge color={c.active ? "green" : "slate"}>{c.active ? "Active" : "Inactive"}</Badge>
                  </td>
                  <td className="p-3">
                    <div className="flex justify-end gap-1.5">
                      <button className="btn-secondary btn-sm" onClick={() => toggleActive(c)}>
                        {c.active ? "Deactivate" : "Activate"}
                      </button>
                      <button
                        className="btn-danger btn-sm"
                        disabled={c.documentCount > 0}
                        title={c.documentCount > 0 ? "Reassign documents before deleting" : ""}
                        onClick={() => remove(c.id)}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {items.length === 0 && (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-slate-400">
                    No categories yet
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
