import { FormEvent, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import api, { apiErrorMessage } from "../../api/client";
import { Footer } from "../../components/ui";

export default function ResetPassword() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    try {
      await api.post("/auth/reset-password", { token: params.get("token"), newPassword: password });
      setDone(true);
      setTimeout(() => navigate("/login"), 2000);
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-br from-brand-50 to-slate-100 px-4">
      <div className="card w-full max-w-md p-8">
        <h1 className="text-xl font-bold text-brand-700">Reset Password</h1>
        {done ? (
          <p className="mt-4 text-sm text-emerald-600">Password reset! Redirecting to login...</p>
        ) : (
          <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
            {error && <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
            <div>
              <label className="label">New Password</label>
              <input
                className="input"
                type="password"
                minLength={8}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <button className="btn-primary w-full">Reset Password</button>
          </form>
        )}
        <p className="mt-6 text-center text-sm text-slate-500">
          <Link to="/login" className="text-brand-600 hover:underline">
            Back to login
          </Link>
        </p>
      </div>
      <Footer />
    </div>
  );
}
