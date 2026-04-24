import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import * as authModule from '@/auth';

type AuthState = {
  credentialId: string | null;
  loading: boolean;
  login: (user: string, password: string) => Promise<void>;
  register: (user: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  changePassword: (oldPassword: string, newPassword: string) => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [credentialId, setCredentialId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  // Once login/logout has set state, the initial validateToken() is no longer
  // authoritative — it may have been fired before the session cookie existed
  // and would otherwise race-overwrite a freshly-authenticated credentialId.
  const established = useRef(false);

  useEffect(() => {
    let cancelled = false;
    authModule.validateToken().then((valid) => {
      if (cancelled || established.current) return;
      established.current = true;
      setCredentialId(valid ? authModule.getCredentialId() : null);
      setLoading(false);
    });
    return () => { cancelled = true; };
  }, []);

  const value: AuthState = {
    credentialId,
    loading,
    async login(user, password) {
      await authModule.login(user, password);
      established.current = true;
      setCredentialId(authModule.getCredentialId());
      setLoading(false);
    },
    async register(user, password) {
      await authModule.register(user, password);
    },
    async logout() {
      await authModule.logout();
      established.current = true;
      setCredentialId(null);
      setLoading(false);
    },
    async changePassword(oldPassword, newPassword) {
      await authModule.changePassword(oldPassword, newPassword);
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
