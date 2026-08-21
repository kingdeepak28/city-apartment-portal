// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import api, { apiErrorMessage } from "../../api/client";
import { AdminSummary } from "../../api/types";
import { AsyncButton, Badge, ButtonSpinner, formatDateTime, Modal, Spinner, statusColor } from "../../components/ui";
import { useToast } from "../../context/ToastContext";

export default function AdminAccounts() {
  const { notify } = useToast();
  const [items, setItems] = useState<AdminSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [demoteAdmin, setDemoteAdmin] = useState<AdminSummary | null>(null);

  async function load() {
    setLoading(true);
    const { data } = await api.get<AdminSummary[]>("/admin/admins");
    setItems(data);
    setLoading(false);
  }

  useEffect(() => {
    load();
  }, []);

  async function toggleStatus(a: AdminSummary) {
    try {
      await api.patch(`/admin/admins/${a.id}/status`, null, { params: { status: a.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE" } });
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function unlock(id: string) {
    try {
      await api.post(`/admin/admins/${id}/unlock`);
      notify("Account unlocked", "success");
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Admin Accounts</h1>
        <button className="btn-primary" onClick={() => setCreateOpen(true)}>
          + Add Admin
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
                <th className="p-3">Email</th>
                <th className="p-3">Role</th>
                <th className="p-3">Status</th>
                <th className="p-3">Last Login</th>
                <th className="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {items.map((a) => (
                <tr key={a.id}>
                  <td className="p-3 font-medium">{a.name}</td>
                  <td className="p-3 text-xs text-slate-500">{a.email}</td>
                  <td className="p-3">
                    <Badge color="purple">{a.role}</Badge>
                  </td>
                  <td className="p-3">
                    <Badge color={statusColor(a.status)}>{a.status}</Badge>
                  </td>
                  <td className="p-3 text-xs text-slate-500">{formatDateTime(a.lastLogin)}</td>
                  <td className="p-3">
                    <div className="flex justify-end gap-1.5">
                      <AsyncButton className="btn-secondary btn-sm" onClick={() => toggleStatus(a)}>
                        {a.status === "ACTIVE" ? "Suspend" : "Reactivate"}
                      </AsyncButton>
                      <AsyncButton className="btn-secondary btn-sm" onClick={() => unlock(a.id)}>
                        Unlock
                      </AsyncButton>
                      <button className="btn-secondary btn-sm" onClick={() => setDemoteAdmin(a)}>
                        Make Member
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {createOpen && (
        <CreateAdminModal
          onClose={() => setCreateOpen(false)}
          onCreated={() => {
            setCreateOpen(false);
            load();
          }}
        />
      )}

      {demoteAdmin && (
        <DemoteModal
          admin={demoteAdmin}
          onClose={() => setDemoteAdmin(null)}
          onDone={() => {
            setDemoteAdmin(null);
            load();
          }}
        />
      )}
    </div>
  );
}

function CreateAdminModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const { notify } = useToast();
  const [form, setForm] = useState({ name: "", email: "", mobile: "", role: "ADMIN", password: "" });
  const [busy, setBusy] = useState(false);

  async function submit() {
    const password = form.password.trim();
    if (password && password.length < 8) {
      notify("Password must be at least 8 characters", "error");
      return;
    }
    setBusy(true);
    try {
      // Omitting the key entirely (rather than sending "") when left blank matches what the
      // backend treats as "generate one for me" - an explicit empty string would instead fail
      // its own length check.
      await api.post("/admin/admins", { ...form, password: password || undefined });
      notify(password ? "Admin account created" : "Admin account created and a temporary password was emailed", "success");
      onCreated();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title="Add Admin Account" onClose={onClose}>
      <div className="space-y-3">
        <div>
          <label className="label">Name</label>
          <input className="input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        </div>
        <div>
          <label className="label">Email</label>
          <input className="input" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </div>
        <div>
          <label className="label">Mobile</label>
          <input className="input" value={form.mobile} onChange={(e) => setForm({ ...form, mobile: e.target.value })} />
        </div>
        <div>
          <label className="label">Role</label>
          <select className="input" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
            <option value="SUPER_ADMIN">Super Admin</option>
            <option value="ADMIN">Admin (Committee)</option>
            <option value="UPLOADER">Content Uploader</option>
          </select>
        </div>
        <div>
          <label className="label">Password (optional)</label>
          <input
            className="input"
            type="password"
            minLength={8}
            placeholder="Leave blank to auto-generate one"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
          />
          <p className="mt-1 text-xs text-slate-400">
            {form.password
              ? "Sent to no one - share it with them yourself."
              : "A temporary password will be generated and emailed to them."}
          </p>
        </div>
        <div className="flex justify-end gap-2">
          <button className="btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button className="btn-primary" disabled={busy} onClick={submit}>
            {busy && <ButtonSpinner />}
            Create
          </button>
        </div>
      </div>
    </Modal>
  );
}

function DemoteModal({ admin, onClose, onDone }: { admin: AdminSummary; onClose: () => void; onDone: () => void }) {
  const { notify } = useToast();
  // Admin accounts have no flat/block/resident-type at all, and mobile is optional there (unlike
  // members, where it's required) - so unlike promoting a member to admin, this can't be a single
  // click. Pre-fill mobile if this admin already has one; leave it editable either way.
  const [form, setForm] = useState({ flatNo: "", block: "", residentType: "OWNER", mobile: admin.mobile || "" });
  const [busy, setBusy] = useState(false);

  async function submit() {
    setBusy(true);
    try {
      await api.post(`/admin/admins/${admin.id}/demote-to-user`, form);
      notify(`${admin.name} converted to a member account`, "success");
      onDone();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title={`Make ${admin.name} a Member`} onClose={onClose}>
      <div className="space-y-3">
        <p className="text-sm text-slate-600">
          This moves {admin.name} from Admin Accounts to Users as a regular member, using their existing
          password. Their admin access ({admin.role}) will be removed - an identity can't be both an admin
          and a member at the same time.
        </p>
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
        <div>
          <label className="label">Mobile</label>
          <input className="input" value={form.mobile} onChange={(e) => setForm({ ...form, mobile: e.target.value })} />
          {!admin.mobile && <p className="mt-1 text-xs text-slate-400">This admin has no mobile on file - one is required for a member account.</p>}
        </div>
        <div className="flex justify-end gap-2">
          <button className="btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button className="btn-primary" disabled={busy || !form.flatNo || !form.block || !form.mobile} onClick={submit}>
            {busy && <ButtonSpinner />}
            Confirm
          </button>
        </div>
      </div>
    </Modal>
  );
}
