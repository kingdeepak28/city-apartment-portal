import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import api, { apiErrorMessage } from "../../api/client";
import { Footer } from "../../components/ui";

interface FormData {
  fullName: string;
  flatNo: string;
  block: string;
  residentType: string;
  mobile: string;
  email: string;
  password: string;
  confirmPassword: string;
}

const initialForm: FormData = {
  fullName: "",
  flatNo: "",
  block: "",
  residentType: "OWNER",
  mobile: "",
  email: "",
  password: "",
  confirmPassword: "",
};

export default function Register() {
  const [form, setForm] = useState<FormData>(initialForm);
  const [step, setStep] = useState<"form" | "proof" | "submitted">("form");
  const [proof, setProof] = useState<File | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  function update<K extends keyof FormData>(key: K, value: FormData[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function goToProofStep(e: FormEvent) {
    e.preventDefault();
    setError("");
    if (form.password !== form.confirmPassword) {
      setError("Passwords do not match");
      return;
    }
    if (form.password.length < 8) {
      setError("Password must be at least 8 characters");
      return;
    }
    setBusy(true);
    try {
      await api.post("/auth/register/check-duplicate", {
        email: form.email,
        mobile: form.mobile,
        flatNo: form.flatNo,
        block: form.block,
      });
      setStep("proof");
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function submitRegistration(e: FormEvent) {
    e.preventDefault();
    setError("");
    if (!proof) {
      setError("Please attach an ID/ownership proof document");
      return;
    }
    setBusy(true);
    try {
      const data = new Blob(
        [
          JSON.stringify({
            fullName: form.fullName,
            flatNo: form.flatNo,
            block: form.block,
            residentType: form.residentType,
            mobile: form.mobile,
            email: form.email,
            password: form.password,
          }),
        ],
        { type: "application/json" }
      );
      const fd = new FormData();
      fd.append("data", data);
      fd.append("proof", proof);
      await api.post("/auth/register/submit", fd);
      setStep("submitted");
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  if (step === "submitted") {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-br from-brand-50 to-slate-100 px-4">
        <div className="card w-full max-w-md p-8 text-center">
          <div className="text-4xl">✅</div>
          <h1 className="mt-3 text-xl font-bold text-brand-700">Registration Submitted</h1>
          <p className="mt-2 text-sm text-slate-600">
            Your registration is awaiting approval by the society administrator. You'll receive an email/SMS once
            it's approved.
          </p>
          <Link to="/login" className="btn-primary mt-6 inline-flex">
            Back to Login
          </Link>
        </div>
        <Footer />
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-br from-brand-50 to-slate-100 px-4 py-10">
      <div className="card w-full max-w-lg p-8">
        <h1 className="text-xl font-bold text-brand-700">Resident Registration</h1>
        <p className="mt-1 text-sm text-slate-500">
          Step {step === "form" ? "1" : "2"} of 2 - {step === "form" ? "Your details" : "Attach proof & submit"}
        </p>

        {error && <div className="mt-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}

        {step === "form" && (
          <form className="mt-6 space-y-4" onSubmit={goToProofStep}>
            <div>
              <label className="label">Full Name</label>
              <input className="input" value={form.fullName} onChange={(e) => update("fullName", e.target.value)} required />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="label">Flat / Unit No.</label>
                <input className="input" value={form.flatNo} onChange={(e) => update("flatNo", e.target.value)} required />
              </div>
              <div>
                <label className="label">Block / Tower</label>
                <input className="input" value={form.block} onChange={(e) => update("block", e.target.value)} required />
              </div>
            </div>
            <div>
              <label className="label">Resident Type</label>
              <select className="input" value={form.residentType} onChange={(e) => update("residentType", e.target.value)}>
                <option value="OWNER">Owner</option>
                <option value="TENANT">Tenant</option>
              </select>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="label">Mobile Number</label>
                <input className="input" value={form.mobile} onChange={(e) => update("mobile", e.target.value)} required />
              </div>
              <div>
                <label className="label">Email</label>
                <input
                  className="input"
                  type="email"
                  value={form.email}
                  onChange={(e) => update("email", e.target.value)}
                  required
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="label">Password</label>
                <input
                  className="input"
                  type="password"
                  minLength={8}
                  value={form.password}
                  onChange={(e) => update("password", e.target.value)}
                  required
                />
              </div>
              <div>
                <label className="label">Confirm Password</label>
                <input
                  className="input"
                  type="password"
                  value={form.confirmPassword}
                  onChange={(e) => update("confirmPassword", e.target.value)}
                  required
                />
              </div>
            </div>
            <button className="btn-primary w-full" disabled={busy}>
              Continue
            </button>
          </form>
        )}

        {step === "proof" && (
          <form className="mt-6 space-y-5" onSubmit={submitRegistration}>
            <div>
              <label className="label">ID / Ownership Proof Document</label>
              <input
                className="input"
                type="file"
                accept=".pdf,.jpg,.jpeg,.png"
                onChange={(e) => setProof(e.target.files?.[0] ?? null)}
                required
              />
            </div>

            <div className="flex gap-2">
              <button type="button" className="btn-secondary" onClick={() => setStep("form")}>
                Back
              </button>
              <button className="btn-primary flex-1" disabled={busy}>
                {busy ? "Submitting..." : "Submit Registration"}
              </button>
            </div>
          </form>
        )}

        <p className="mt-6 text-center text-sm text-slate-500">
          Already registered?{" "}
          <Link to="/login" className="font-medium text-brand-600 hover:underline">
            Sign in
          </Link>
        </p>
      </div>
      <Footer />
    </div>
  );
}
