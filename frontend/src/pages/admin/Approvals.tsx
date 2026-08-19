// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import api, { apiErrorMessage } from "../../api/client";
import { Page, UserSummary } from "../../api/types";
import { AsyncButton, Badge, formatDateTime, Modal, Pagination, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";

const REJECTION_REASONS = ["Invalid proof", "Not a resident", "Duplicate account", "Incomplete details", "Other"];

export default function Approvals() {
  const { notify } = useToast();
  const [data, setData] = useState<Page<UserSummary> | null>(null);
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState("");
  const [block, setBlock] = useState("");
  const [residentType, setResidentType] = useState("");
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [proofModal, setProofModal] = useState<UserSummary | null>(null);
  const [proofSrc, setProofSrc] = useState<string | null>(null);
  const [rejectModal, setRejectModal] = useState<UserSummary | { bulk: true } | null>(null);
  const [rejectReason, setRejectReason] = useState(REJECTION_REASONS[0]);
  const [rejectRemarks, setRejectRemarks] = useState("");
  const [infoModal, setInfoModal] = useState<UserSummary | null>(null);
  const [infoNote, setInfoNote] = useState("");
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    const { data } = await api.get<Page<UserSummary>>("/admin/approvals", {
      params: { page, size: 10, keyword: keyword || undefined, block: block || undefined, residentType: residentType || undefined },
    });
    setData(data);
    setLoading(false);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, keyword, block, residentType]);

  async function approve(id: string) {
    try {
      await api.post(`/admin/approvals/${id}/approve`);
      notify("Registration approved", "success");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function bulkApprove() {
    try {
      await api.post("/admin/approvals/bulk-approve", { userIds: [...selected] });
      notify(`${selected.size} registration(s) approved`, "success");
      setSelected(new Set());
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function submitReject() {
    try {
      if (rejectModal && "bulk" in rejectModal) {
        await api.post("/admin/approvals/bulk-reject", { userIds: [...selected], reason: rejectReason, remarks: rejectRemarks });
        setSelected(new Set());
      } else if (rejectModal) {
        await api.post(`/admin/approvals/${rejectModal.id}/reject`, { reason: rejectReason, remarks: rejectRemarks });
      }
      notify("Registration(s) rejected", "success");
      setRejectModal(null);
      setRejectRemarks("");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function submitInfoRequest() {
    if (!infoModal) return;
    try {
      await api.post(`/admin/approvals/${infoModal.id}/request-info`, { note: infoNote });
      notify("Information requested from applicant", "success");
      setInfoModal(null);
      setInfoNote("");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  useEffect(() => {
    // /api/files/registration-proof/{id} requires auth, so a plain <iframe src="..."> gets a 401
    // ("Authentication required or session expired") - the browser doesn't attach the app's
    // Authorization header to a frame/image navigation. Fetch it through the authenticated
    // client instead and hand the iframe a local blob URL, same fix as the member profile photo.
    if (!proofModal?.proofFileUrl) {
      setProofSrc(null);
      return;
    }
    let objectUrl: string | null = null;
    let cancelled = false;
    api
      .get(proofModal.proofFileUrl.replace(/^\/api/, ""), { responseType: "blob" })
      .then((r) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(r.data);
        setProofSrc(objectUrl);
      })
      .catch((err) => {
        if (!cancelled) notify(apiErrorMessage(err), "error");
      });
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [proofModal]);

  function toggle(id: string) {
    setSelected((s) => {
      const next = new Set(s);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Pending Approvals</h1>

      <div className="card flex flex-wrap gap-3 p-4">
        <input className="input max-w-xs" placeholder="Search name, email, flat..." value={keyword} onChange={(e) => setKeyword(e.target.value)} />
        <input className="input max-w-[140px]" placeholder="Block" value={block} onChange={(e) => setBlock(e.target.value)} />
        <select className="input max-w-[160px]" value={residentType} onChange={(e) => setResidentType(e.target.value)}>
          <option value="">All resident types</option>
          <option value="OWNER">Owner</option>
          <option value="TENANT">Tenant</option>
        </select>
        {selected.size > 0 && (
          <div className="ml-auto flex gap-2">
            <AsyncButton className="btn-primary btn-sm" onClick={bulkApprove}>
              Approve {selected.size}
            </AsyncButton>
            <button className="btn-danger btn-sm" onClick={() => setRejectModal({ bulk: true })}>
              Reject {selected.size}
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
                    onChange={(e) =>
                      setSelected(e.target.checked ? new Set(data?.content.map((u) => u.id)) : new Set())
                    }
                  />
                </th>
                <th className="p-3">Name</th>
                <th className="p-3">Flat / Block</th>
                <th className="p-3">Type</th>
                <th className="p-3">Contact</th>
                <th className="p-3">Submitted</th>
                <th className="p-3">Proof</th>
                <th className="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data?.content.map((u) => (
                <tr key={u.id}>
                  <td className="p-3">
                    <input type="checkbox" checked={selected.has(u.id)} onChange={() => toggle(u.id)} />
                  </td>
                  <td className="p-3 font-medium">
                    {u.name} {u.overdue && <Badge color="red">Overdue</Badge>}
                  </td>
                  <td className="p-3">
                    {u.flatNo} / {u.block}
                  </td>
                  <td className="p-3">{u.residentType}</td>
                  <td className="p-3 text-xs text-slate-500">
                    {u.email}
                    <br />
                    {u.mobile}
                  </td>
                  <td className="p-3 text-xs text-slate-500">{formatDateTime(u.registeredOn)}</td>
                  <td className="p-3">
                    <button className="text-xs text-brand-600 hover:underline" onClick={() => setProofModal(u)}>
                      Preview
                    </button>
                  </td>
                  <td className="p-3">
                    <div className="flex justify-end gap-1.5">
                      <AsyncButton className="btn-primary btn-sm" onClick={() => approve(u.id)}>
                        Approve
                      </AsyncButton>
                      <button className="btn-secondary btn-sm" onClick={() => setInfoModal(u)}>
                        Info
                      </button>
                      <button className="btn-danger btn-sm" onClick={() => setRejectModal(u)}>
                        Reject
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {data?.content.length === 0 && (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-slate-400">
                    No pending approvals
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
        {data && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}
      </div>

      {proofModal && (
        <Modal title={`Proof - ${proofModal.name}`} onClose={() => setProofModal(null)} wide>
          {proofModal.proofFileUrl ? (
            proofSrc ? (
              <iframe src={proofSrc} className="h-[70vh] w-full rounded border" title="proof" />
            ) : (
              <Spinner />
            )
          ) : (
            <p className="text-sm text-slate-500">No proof document available</p>
          )}
        </Modal>
      )}

      {rejectModal && (
        <Modal title="Reject Registration" onClose={() => setRejectModal(null)}>
          <div className="space-y-3">
            <div>
              <label className="label">Reason</label>
              <select className="input" value={rejectReason} onChange={(e) => setRejectReason(e.target.value)}>
                {REJECTION_REASONS.map((r) => (
                  <option key={r}>{r}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="label">Remarks (optional)</label>
              <textarea className="input" rows={3} value={rejectRemarks} onChange={(e) => setRejectRemarks(e.target.value)} />
            </div>
            <div className="flex justify-end gap-2">
              <button className="btn-secondary" onClick={() => setRejectModal(null)}>
                Cancel
              </button>
              <AsyncButton className="btn-danger" onClick={submitReject}>
                Reject
              </AsyncButton>
            </div>
          </div>
        </Modal>
      )}

      {infoModal && (
        <Modal title="Request More Information" onClose={() => setInfoModal(null)}>
          <div className="space-y-3">
            <div>
              <label className="label">Note to applicant</label>
              <textarea className="input" rows={3} value={infoNote} onChange={(e) => setInfoNote(e.target.value)} />
            </div>
            <div className="flex justify-end gap-2">
              <button className="btn-secondary" onClick={() => setInfoModal(null)}>
                Cancel
              </button>
              <AsyncButton className="btn-primary" onClick={submitInfoRequest}>
                Send Request
              </AsyncButton>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
