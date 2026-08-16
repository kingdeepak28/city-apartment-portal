import { useEffect, useState } from "react";
import api, { apiErrorMessage } from "../../api/client";
import { Page } from "../../api/types";
import { Badge, formatDateTime, Pagination, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";
import { useAuth } from "../../context/AuthContext";

interface BinItem {
  id: string;
  contentType: string;
  title: string;
  deletedAt: string;
  purgeEligibleAfter: string;
}

export default function RecycleBin() {
  const { notify } = useToast();
  const { user } = useAuth();
  const [data, setData] = useState<Page<BinItem> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    const { data } = await api.get<Page<BinItem>>("/admin/recycle-bin", { params: { page, size: 20 } });
    setData(data);
    setLoading(false);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  async function restore(id: string) {
    try {
      await api.post(`/admin/recycle-bin/${id}/restore`);
      notify("Restored", "success");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function permanentDelete(id: string) {
    if (!window.confirm("Permanently delete this item? This cannot be undone.")) return;
    try {
      await api.delete(`/admin/recycle-bin/${id}`);
      notify("Permanently deleted", "success");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Recycle Bin</h1>
      <p className="text-sm text-slate-500">Deleted items are retained for 30 days before permanent deletion.</p>
      <div className="card overflow-x-auto">
        {loading ? (
          <Spinner />
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="p-3">Type</th>
                <th className="p-3">Title</th>
                <th className="p-3">Deleted On</th>
                <th className="p-3">Purge Eligible After</th>
                <th className="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data?.content.map((d) => (
                <tr key={d.id}>
                  <td className="p-3">
                    <Badge color="blue">{d.contentType}</Badge>
                  </td>
                  <td className="p-3 font-medium">{d.title}</td>
                  <td className="p-3 text-xs text-slate-500">{formatDateTime(d.deletedAt)}</td>
                  <td className="p-3 text-xs text-slate-500">{formatDateTime(d.purgeEligibleAfter)}</td>
                  <td className="p-3">
                    <div className="flex justify-end gap-1.5">
                      <button className="btn-primary btn-sm" onClick={() => restore(d.id)}>
                        Restore
                      </button>
                      {user?.role === "SUPER_ADMIN" && (
                        <button className="btn-danger btn-sm" onClick={() => permanentDelete(d.id)}>
                          Delete Permanently
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {data?.content.length === 0 && (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-slate-400">
                    Recycle bin is empty
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
