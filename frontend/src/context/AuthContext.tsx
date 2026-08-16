import { createContext, useContext, useState, ReactNode } from "react";
import api, { TOKEN_KEY } from "../api/client";
import { LoginResponse } from "../api/types";

interface StoredUser {
  id: string;
  name: string;
  email: string;
  accountType: "USER" | "ADMIN";
  role: string;
}

interface AuthContextValue {
  user: StoredUser | null;
  isAdmin: boolean;
  isMember: boolean;
  login: (identifier: string, password: string) => Promise<StoredUser>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function loadUser(): StoredUser | null {
  const raw = localStorage.getItem("sdp_user");
  return raw ? JSON.parse(raw) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<StoredUser | null>(loadUser());

  async function login(identifier: string, password: string) {
    const { data } = await api.post<LoginResponse>("/auth/login", { identifier, password });
    localStorage.setItem(TOKEN_KEY, data.token);
    const stored: StoredUser = {
      id: data.id,
      name: data.name,
      email: data.email,
      accountType: data.accountType,
      role: data.role,
    };
    localStorage.setItem("sdp_user", JSON.stringify(stored));
    setUser(stored);
    return stored;
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem("sdp_user");
    setUser(null);
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        isAdmin: user?.accountType === "ADMIN",
        isMember: user?.accountType === "USER",
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
