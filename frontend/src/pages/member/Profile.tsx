import { useEffect, useState } from "react";
import api, { apiErrorMessage } from "../../api/client";
import { formatDateTime } from "../../components/ui";
import { useToast } from "../../context/ToastContext";

interface ProfileResponse {
  id: string;
  name: string;
  email: string;
  mobile: string;
  flatNo: string;
  block: string;
  residentType: string;
  approvedOn?: string;
  photoUrl?: string;
}

interface LoginHistoryItem {
  timestamp: string;
  ip: string;
  status: string;
}

export default function Profile() {
  const { notify } = useToast();
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [photoSrc, setPhotoSrc] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [history, setHistory] = useState<LoginHistoryItem[]>([]);
  const [passwords, setPasswords] = useState({ currentPassword: "", newPassword: "", confirm: "" });
  const [correctionMsg, setCorrectionMsg] = useState("");

  function load() {
    api.get<ProfileResponse>("/member/profile").then((r) => {
      setProfile(r.data);
      setName(r.data.name);
    });
    api.get<LoginHistoryItem[]>("/member/profile/login-history").then((r) => setHistory(r.data));
  }

  useEffect(load, []);

  useEffect(() => {
    // The photo endpoint requires auth, so a plain <img src="/api/files/photo/..."> 404s/401s -
    // browsers don't attach the app's Authorization header to image requests. Fetch it through
    // the authenticated client instead and hand the <img> a local blob URL.
    if (!profile?.photoUrl) {
      setPhotoSrc(null);
      return;
    }
    let objectUrl: string | null = null;
    let cancelled = false;
    api.get(profile.photoUrl.replace(/^\/api/, ""), { responseType: "blob" }).then((r) => {
      if (cancelled) return;
      objectUrl = URL.createObjectURL(r.data);
      setPhotoSrc(objectUrl);
    });
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [profile?.photoUrl]);

  async function saveProfile() {
    try {
      await api.put("/member/profile", { name });
      notify("Profile updated", "success");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function uploadPhoto(file: File) {
    const fd = new FormData();
    fd.append("photo", file);
    try {
      await api.post("/member/profile/photo", fd);
      notify("Photo updated", "success");
      load();
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function changePassword() {
    if (passwords.newPassword !== passwords.confirm) {
      notify("New passwords do not match", "error");
      return;
    }
    try {
      await api.post("/auth/change-password", { currentPassword: passwords.currentPassword, newPassword: passwords.newPassword });
      notify("Password changed", "success");
      setPasswords({ currentPassword: "", newPassword: "", confirm: "" });
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  async function sendCorrection() {
    try {
      await api.post("/member/profile/request-correction", { message: correctionMsg });
      notify("Correction request sent to the administrator", "success");
      setCorrectionMsg("");
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    }
  }

  if (!profile) return null;

  return (
    <div className="max-w-3xl space-y-6">
      <h1 className="text-xl font-semibold">My Profile</h1>

      <div className="card space-y-4 p-5">
        <h2 className="font-semibold">Basic Details</h2>
        <div className="flex items-center gap-4">
          {photoSrc ? (
            <img src={photoSrc} className="h-16 w-16 rounded-full object-cover" alt="profile" />
          ) : (
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-slate-200 text-xl">
              {profile.name.charAt(0)}
            </div>
          )}
          <input type="file" accept="image/*" onChange={(e) => e.target.files?.[0] && uploadPhoto(e.target.files[0])} />
        </div>
        <div>
          <label className="label">Name</label>
          <input className="input" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="grid grid-cols-2 gap-3 text-sm">
          <div>
            <div className="text-xs text-slate-400">Email</div>
            <div>{profile.email}</div>
          </div>
          <div>
            <div className="text-xs text-slate-400">Mobile</div>
            <div>{profile.mobile}</div>
          </div>
          <div>
            <div className="text-xs text-slate-400">Flat / Block (read-only)</div>
            <div>
              {profile.flatNo} / {profile.block}
            </div>
          </div>
          <div>
            <div className="text-xs text-slate-400">Resident Type (read-only)</div>
            <div>{profile.residentType}</div>
          </div>
        </div>
        <div className="flex justify-end">
          <button className="btn-primary" onClick={saveProfile}>
            Save
          </button>
        </div>
      </div>

      <div className="card space-y-3 p-5">
        <h2 className="font-semibold">Request Correction to Flat/Resident Type</h2>
        <textarea className="input" rows={2} placeholder="Describe the correction needed..." value={correctionMsg} onChange={(e) => setCorrectionMsg(e.target.value)} />
        <div className="flex justify-end">
          <button className="btn-secondary" disabled={!correctionMsg} onClick={sendCorrection}>
            Send Request
          </button>
        </div>
      </div>

      <div className="card space-y-4 p-5">
        <h2 className="font-semibold">Change Password</h2>
        <div className="grid gap-3 sm:grid-cols-3">
          <input
            className="input"
            type="password"
            placeholder="Current password"
            value={passwords.currentPassword}
            onChange={(e) => setPasswords({ ...passwords, currentPassword: e.target.value })}
          />
          <input
            className="input"
            type="password"
            placeholder="New password"
            value={passwords.newPassword}
            onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })}
          />
          <input
            className="input"
            type="password"
            placeholder="Confirm new password"
            value={passwords.confirm}
            onChange={(e) => setPasswords({ ...passwords, confirm: e.target.value })}
          />
        </div>
        <div className="flex justify-end">
          <button className="btn-primary" onClick={changePassword}>
            Change Password
          </button>
        </div>
      </div>

      <div className="card p-5">
        <h2 className="mb-3 font-semibold">Recent Login History</h2>
        <table className="w-full text-sm">
          <thead className="text-left text-xs uppercase text-slate-400">
            <tr>
              <th className="py-1">Date & Time</th>
              <th className="py-1">IP Address</th>
              <th className="py-1">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {history.map((h, i) => (
              <tr key={i}>
                <td className="py-1.5">{formatDateTime(h.timestamp)}</td>
                <td className="py-1.5 text-slate-500">{h.ip}</td>
                <td className="py-1.5 text-emerald-600">{h.status}</td>
              </tr>
            ))}
            {history.length === 0 && (
              <tr>
                <td colSpan={3} className="py-4 text-center text-slate-400">
                  No login history yet
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
