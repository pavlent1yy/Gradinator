'use client';

import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import * as api from '../../lib/api';
import type { Me } from '../../lib/api';
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '../../lib/auth';

type AuthContextValue = {
  user: Me | null;
  initializing: boolean;
  authLoading: boolean;
  authError: string | null;
  login: (email: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>;
  clearAuthError: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<Me | null>(null);
  const [initializing, setInitializing] = useState(true);
  const [authLoading, setAuthLoading] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);

  const restoreSession = useCallback(async () => {
    const access = getAccessToken();
    if (!access) return;
    try {
      const me = await api.fetchMe(access);
      setUser(me);
      return;
    } catch {
      /* access token expired — try refresh */
    }
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
      clearTokens();
      return;
    }
    try {
      const tokens = await api.refreshTokens(refreshToken);
      setTokens(tokens.accessToken, tokens.refreshToken);
      const me = await api.fetchMe(tokens.accessToken);
      setUser(me);
    } catch {
      clearTokens();
      setUser(null);
    }
  }, []);

  useEffect(() => {
    (async () => {
      await restoreSession();
      setInitializing(false);
    })();
  }, [restoreSession]);

  const login = useCallback(async (email: string, password: string) => {
    setAuthLoading(true);
    setAuthError(null);
    try {
      const tokens = await api.login({ email, password });
      setTokens(tokens.accessToken, tokens.refreshToken);
      const me = await api.fetchMe(tokens.accessToken);
      setUser(me);
      return true;
    } catch (e: any) {
      setAuthError(e.message ?? 'Не удалось войти');
      return false;
    } finally {
      setAuthLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      await api.logout(refreshToken);
    }
    clearTokens();
    setUser(null);
  }, []);

  const clearAuthError = useCallback(() => setAuthError(null), []);

  return (
    <AuthContext.Provider value={{ user, initializing, authLoading, authError, login, logout, clearAuthError }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuthContext() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuthContext must be used within AuthProvider');
  return ctx;
}
