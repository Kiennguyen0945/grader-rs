import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { apiRequest, getToken, setToken } from '../api';
import { isDemo } from '../dev/demo';
import type { LoginResponse } from '../types';

export interface Session {
  token: string;
  userId: number | null;
  username: string;
  roles: string[];
}

interface AuthCtx {
  session: Session | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const USERNAME_KEY = 'lamplight-username';

/** The backend JWT carries `sub` (userId) and `roles`; decoded client-side only. */
function decodeToken(token: string): { userId: number | null; roles: string[] } {
  try {
    const payload = token.split('.')[1];
    const json = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    const userId = Number(json.sub);
    return {
      userId: Number.isFinite(userId) ? userId : null,
      roles: Array.isArray(json.roles) ? json.roles : [],
    };
  } catch {
    return { userId: null, roles: [] };
  }
}

function readSession(): Session | null {
  const token = getToken();
  if (!token) return null;
  let username = '';
  try {
    username = localStorage.getItem(USERNAME_KEY) ?? '';
  } catch {}
  const { userId, roles } = decodeToken(token);
  return { token, userId, username: username || 'student', roles };
}

const Ctx = createContext<AuthCtx>({ session: null, login: async () => {}, logout: () => {} });

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => readSession());

  useEffect(() => {
    const onUnauthorized = () => {
      try {
        localStorage.removeItem(USERNAME_KEY);
      } catch {}
      setSession(null);
    };
    window.addEventListener('auth:unauthorized', onUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', onUnauthorized);
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    if (isDemo()) {
      // Design-review build: no backend required, prototype data only.
      const demoUsername = username.trim() || 'minh.tran';
      setToken('demo.token');
      try {
        localStorage.setItem(USERNAME_KEY, demoUsername);
      } catch {}
      setSession({ token: 'demo.token', userId: 1, username: demoUsername, roles: ['ROLE_STUDENT'] });
      return;
    }

    const res = await apiRequest<LoginResponse>('/auth/login', {
      method: 'POST',
      body: { username, password },
    });
    setToken(res.accessToken);
    try {
      localStorage.setItem(USERNAME_KEY, username);
    } catch {}
    const { userId, roles } = decodeToken(res.accessToken);
    setSession({ token: res.accessToken, userId, username, roles });
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    try {
      localStorage.removeItem(USERNAME_KEY);
    } catch {}
    setSession(null);
    if (window.location.hash && window.location.hash !== '#/dashboard') {
      window.location.hash = '#/dashboard';
    }
  }, []);

  return <Ctx.Provider value={{ session, login, logout }}>{children}</Ctx.Provider>;
}

export const useAuth = () => useContext(Ctx);
