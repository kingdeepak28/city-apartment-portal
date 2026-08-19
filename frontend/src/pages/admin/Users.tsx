// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import api, { apiErrorMessage } from "../../api/client";
import { Page, UserSummary } from "../../api/types";
import { Badge, formatDate, formatDateTime, Modal, Pagination, Spinner, statusColor } from "../../components/ui";
import { useToast } from "../../context/ToastContext";

export default function Users() {
  const { notify } = useToast();
  const [data, setData] = useState<Page<UserSummary> | null>(null);
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState("");
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [detailUser, setDetailUser] = useState<UserSummary | null>(null);

  async function load() {
    setLoading(true);
    const { data } = await api.get<Page<UserSummary>>("/admin/users", {
      params: { page, size: 15, status: status || undefined, keyword: keyword || undefined },
    });
    setData(data);
    setLoading(false);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, status, keyword]);

  async function updateStatus(id: string, newStatus: string) {
    try {
      await api.patch(`/admin/users/${id}/status`, { status: newStatus });
      notify("Status updated", "success");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function deleteUser(id: string, name: string) {
    if (!window.confirm(`Permanently delete ${name}? This cannot be undone.`)) return;
    try {
      await api.delete(`/admin/users/${id}`);
      notify("User deleted", "success");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function triggerReset(id: string) {
    try {
      await api.post(`/admin/users/${id}/trigger-password-reset`);
      notify("Password reset email sent", "success");
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function exportUsers() {
    const res = await api.get("/admin/users/export", { params: { status: status || undefined }, responseType: "blob" });
    const url = URL.createObjectURL(res.data);
    const a = document.createElement("a");
    a.href = url;
    a.download = "users.xlsx";
    a.click();
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Users</h1>
        <div className="flex gap-2">
          <button className="btn-secondary" onClick={exportUsers}>
            Export Excel
          </button>
          <button className="btn-secondary" onClick={() => setImportOpen(true)}>
            Bulk Import
          </button>
          <button className="btn-primary" onClick={() => setCreateOpen(true)}>
            + Add User
          </button>
        </div>
      </div>

      <div className="card flex flex-wrap gap-3 p-4">
        <input className="input max-w-xs" placeholder="Search..." value={keyword} onChange={(e) => setKeyword(e.target.value)} />
        <select className="input max-w-[180px]" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          <option value="PENDING">Pending</option>
          <option value="ACTIVE">Active</option>
          <option value="REJECTED">Rejected</option>
          <option value="SUSPENDED">Suspended</option>
          <option value="INFO_REQUESTED">Info Requested</option>
        </select>
      </div>

      <div className="card overflow-x-auto">
        {loading ? (
          <Spinner />
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="p-3">Name</th>
                <th className="p-3">Flat / Block</th>
                <th className="p-3">Contact</th>
                <th className="p-3">Status</th>
                <th className="p-3">Last Login</th>
                <th className="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data?.content.map((u) => (
                <tr key={u.id}>
                  <td className="p-3 font-medium">
                    <button className="hover:underline" onClick={() => setDetailUser(u)}>
                      {u.name}
                    </button>
                  </td>
                  <td className="p-3">
                    {u.flatNo} / {u.block}
                  </td>
                  <td className="p-3 text-xs text-slate-500">
                    {u.email}
                    <br />
                    {u.mobile}
                  </td>
                  <td className="p-3">
                    <Badge color={statusColor(u.status)}>{u.status}</Badge>
                  </td>
                  <td className="p-3 text-xs text-slate-500">{formatDateTime(u.lastLogin)}</td>
                  <td className="p-3">
                    <div className="flex justify-end gap-1.5">
                      {u.status === "ACTIVE" && (
                        <button className="btn-secondary btn-sm" onClick={() => updateStatus(u.id, "SUSPENDED")}>
                          Suspend
                        </button>
                      )}
                      {u.status === "SUSPENDED" && (
                        <button className="btn-primary btn-sm" onClick={() => updateStatus(u.id, "ACTIVE")}>
                          Reactivate
                        </button>
                      )}
                      <button className="btn-secondary btn-sm" onClick={() => triggerReset(u.id)}>
                        Reset PW
                      </button>
                      <button className="btn-danger btn-sm" onClick={() => deleteUser(u.id, u.name)}>
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {data?.content.length === 0 && (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-slate-400">
                    No users found
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
        {data && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}
      </div>

      {createOpen && (
        <CreateUserModal
          onClose={() => setCreateOpen(false)}
          onCreated={() => {
            setCreateOpen(false);
            load();
          }}
        />
      )}

      {importOpen && (
        <BulkImportModal
          onClose={() => setImportOpen(false)}
          onDone={() => {
            setImportOpen(false);
            load();
          }}
        />
      )}

      {detailUser && (
        <Modal title={detailUser.name} onClose={() => setDetailUser(null)}>
          <dl className="space-y-2 text-sm">
            <Row label="Flat / Block" value={`${detailUser.flatNo} / ${detailUser.block}`} />
            <Row label="Resident Type" value={detailUser.residentType} />
            <Row label="Email" value={detailUser.email} />
            <Row label="Mobile" value={detailUser.mobile} />
            <Row label="Status" value={detailUser.status} />
            <Row label="Registered" value={formatDate(detailUser.registeredOn)} />
            <Row label="Approved By" value={detailUser.approvedByName || "-"} />
            <Row label="Last Login" value={formatDateTime(detailUser.lastLogin)} />
          </dl>
        </Modal>
      )}
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between border-b border-slate-100 py-1.5">
      <dt className="text-slate-500">{label}</dt>
      <dd className="font-medium">{value}</dd>
    </div>
  );
}

function CreateUserModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const { notify } = useToast();
  const [form, setForm] = useState({ fullName: "", flatNo: "", block: "", residentType: "OWNER", mobile: "", email: "" });
  const [busy, setBusy] = useState(false);

  async function submit() {
    setBusy(true);
    try {
      await api.post("/admin/users", form);
      notify("User created and notified", "success");
      onCreated();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title="Add Pre-Approved User" onClose={onClose}>
      <div className="space-y-3">
        <div>
          <label className="label">Full Name</label>
          <input className="input" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label">Flat No.</label>
            <input className="input" value={form.flatNo} onChange={(e) => setForm({ ...form, flatNo: e.target.value })} />
          </div>
          <div>
            <label className="label">Block</label>
            <input className="input" value={form.block} onChange={(e) => setForm({ ...form, block: e.target.value })} />
          </div>
        </div>
        <div>
          <label className="label">Resident Type</label>
          <select className="input" value={form.residentType} onChange={(e) => setForm({ ...form, residentType: e.target.value })}>
            <option value="OWNER">Owner</option>
            <option value="TENANT">Tenant</option>
          </select>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label">Mobile</label>
            <input className="input" value={form.mobile} onChange={(e) => setForm({ ...form, mobile: e.target.value })} />
          </div>
          <div>
            <label className="label">Email</label>
            <input className="input" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          </div>
        </div>
        <div className="flex justify-end gap-2">
          <button className="btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button className="btn-primary" disabled={busy} onClick={submit}>
            Create
          </button>
        </div>
      </div>
    </Modal>
  );
}

function BulkImportModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const { notify } = useToast();
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<{ successCount: number; errors: string[] } | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit() {
    if (!file) return;
    setBusy(true);
    try {
      const fd = new FormData();
      fd.append("file", file);
      const { data } = await api.post("/admin/users/bulk-import", fd);
      setResult(data);
      if (data.errors.length === 0) notify(`${data.successCount} user(s) imported`, "success");
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title="Bulk Import Users" onClose={onClose}>
      <p className="mb-3 text-xs text-slate-500">
        CSV columns: name, flatNo, block, residentType (OWNER/TENANT), mobile, email
      </p>
      <input type="file" accept=".csv" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
      {result && (
        <div className="mt-3 rounded-md bg-slate-50 p-3 text-xs">
          <p className="font-medium text-emerald-600">{result.successCount} imported successfully</p>
          {result.errors.length > 0 && (
            <ul className="mt-1 list-disc pl-4 text-red-600">
              {result.errors.map((e, i) => (
                <li key={i}>{e}</li>
              ))}
            </ul>
          )}
        </div>
      )}
      <div className="mt-4 flex justify-end gap-2">
        <button className="btn-secondary" onClick={onClose}>
          {result ? "Close" : "Cancel"}
        </button>
        {!result && (
          <button className="btn-primary" disabled={!file || busy} onClick={submit}>
            Import
          </button>
        )}
        {result && (
          <button className="btn-primary" onClick={onDone}>
            Done
          </button>
        )}
      </div>
    </Modal>
  );
}
