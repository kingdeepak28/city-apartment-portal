// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import api, { apiErrorMessage } from "../../api/client";
import { Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";
import { useAuth } from "../../context/AuthContext";

const FIELDS: { key: string; label: string }[] = [
  { key: "society.name", label: "Society Name" },
  { key: "society.regNo", label: "Registration Number" },
  { key: "society.address", label: "Address" },
  { key: "society.contactEmail", label: "Contact Email" },
  { key: "society.contactPhone", label: "Contact Phone" },
  { key: "file.allowedExtensions", label: "Allowed File Extensions" },
  { key: "file.maxSizeMb", label: "Max File Size (MB)" },
  { key: "notice.numberFormat", label: "Notice Number Format" },
  { key: "tender.numberFormat", label: "Tender Number Format" },
  { key: "approval.slaDays", label: "Approval SLA (days)" },
];

export default function Settings() {
  const { notify } = useToast();
  const { user } = useAuth();
  const [values, setValues] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.get<Record<string, string>>("/admin/settings").then((r) => {
      setValues(r.data);
      setLoading(false);
    });
  }, []);

  async function save() {
    setBusy(true);
    try {
      await api.put("/admin/settings", values);
      notify("Settings updated", "success");
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <Spinner />;

  const readOnly = user?.role !== "SUPER_ADMIN";

  return (
    <div className="max-w-2xl space-y-4">
      <h1 className="text-xl font-semibold">Society Profile & Settings</h1>
      {readOnly && <p className="text-sm text-amber-600">Only the Super Admin can modify settings.</p>}
      <div className="card space-y-4 p-5">
        {FIELDS.map((f) => (
          <div key={f.key}>
            <label className="label">{f.label}</label>
            <input
              className="input"
              disabled={readOnly}
              value={values[f.key] || ""}
              onChange={(e) => setValues({ ...values, [f.key]: e.target.value })}
            />
          </div>
        ))}
        {!readOnly && (
          <div className="flex justify-end border-t border-slate-100 pt-4">
            <button className="btn-primary" disabled={busy} onClick={save}>
              Save Settings
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
