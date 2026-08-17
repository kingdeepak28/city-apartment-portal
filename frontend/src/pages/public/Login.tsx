import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { apiErrorMessage } from "../../api/client";
import { Footer } from "../../components/ui";

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const user = await login(identifier, password);
      navigate(user.accountType === "ADMIN" ? "/admin" : "/member");
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-br from-brand-50 to-slate-100 px-4">
      <div className="card w-full max-w-md p-8">
        <h1 className="text-xl font-bold text-brand-700">Society Document Portal</h1>
        <p className="mt-1 text-sm text-slate-500">Sign in to continue</p>

        {error && <div className="mt-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}

        <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
          <div>
            <label className="label">Email or Mobile Number</label>
            <input className="input" value={identifier} onChange={(e) => setIdentifier(e.target.value)} required />
          </div>
          <div>
            <label className="label">Password</label>
            <input
              className="input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <div className="flex justify-end">
            <Link to="/forgot-password" className="text-xs text-brand-600 hover:underline">
              Forgot password?
            </Link>
          </div>
          <button className="btn-primary w-full" disabled={loading}>
            {loading ? "Signing in..." : "Sign In"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-500">
          New resident?{" "}
          <Link to="/register" className="font-medium text-brand-600 hover:underline">
            Register here
          </Link>
        </p>

        <div className="mt-6 rounded-md bg-slate-50 p-3 text-xs text-slate-400">
          Demo admin: super.admin@societyportal.local / Admin@123
          <br />
          Demo member: demo.member@societyportal.local / Member@123
        </div>
      </div>
      <Footer />
    </div>
  );
}
