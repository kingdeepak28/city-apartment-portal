import { useState } from "react";
import api, { apiErrorMessage } from "../../api/client";
import { useToast } from "../../context/ToastContext";

export default function Broadcast() {
  const { notify } = useToast();
  const [form, setForm] = useState({
    title: "",
    message: "",
    audienceType: "ALL",
    blocks: "",
    residentType: "OWNER",
    sendEmail: true,
    sendSms: false,
  });
  const [busy, setBusy] = useState(false);

  async function send() {
    setBusy(true);
    try {
      await api.post("/admin/notifications/broadcast", {
        title: form.title,
        message: form.message,
        audienceType: form.audienceType,
        blocks: form.audienceType === "BLOCK" ? form.blocks.split(",").map((s) => s.trim()).filter(Boolean) : [],
        residentType: form.audienceType === "RESIDENT_TYPE" ? form.residentType : undefined,
        sendEmail: form.sendEmail,
        sendSms: form.sendSms,
      });
      notify("Broadcast sent", "success");
      setForm({ ...form, title: "", message: "" });
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="max-w-2xl space-y-4">
      <h1 className="text-xl font-semibold">Broadcast Announcement</h1>
      <div className="card space-y-4 p-5">
        <div>
          <label className="label">Title</label>
          <input className="input" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
        </div>
        <div>
          <label className="label">Message</label>
          <textarea className="input" rows={4} value={form.message} onChange={(e) => setForm({ ...form, message: e.target.value })} />
        </div>
        <div>
          <label className="label">Audience</label>
          <select className="input" value={form.audienceType} onChange={(e) => setForm({ ...form, audienceType: e.target.value })}>
            <option value="ALL">All Users</option>
            <option value="BLOCK">Specific Block(s)</option>
            <option value="RESIDENT_TYPE">Owners / Tenants</option>
          </select>
        </div>
        {form.audienceType === "BLOCK" && (
          <div>
            <label className="label">Blocks (comma separated)</label>
            <input className="input" placeholder="A, B, C" value={form.blocks} onChange={(e) => setForm({ ...form, blocks: e.target.value })} />
          </div>
        )}
        {form.audienceType === "RESIDENT_TYPE" && (
          <div>
            <label className="label">Resident Type</label>
            <select className="input" value={form.residentType} onChange={(e) => setForm({ ...form, residentType: e.target.value })}>
              <option value="OWNER">Owners</option>
              <option value="TENANT">Tenants</option>
            </select>
          </div>
        )}
        <div className="flex gap-6">
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={form.sendEmail} onChange={(e) => setForm({ ...form, sendEmail: e.target.checked })} />
            Send Email
          </label>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={form.sendSms} onChange={(e) => setForm({ ...form, sendSms: e.target.checked })} />
            Send SMS
          </label>
        </div>
        <div className="flex justify-end">
          <button className="btn-primary" disabled={busy || !form.title || !form.message} onClick={send}>
            Send Broadcast
          </button>
        </div>
      </div>
    </div>
  );
}
