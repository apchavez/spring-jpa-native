import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';

import { api, ApiError, setAuthToken } from '../api/client';
import { storage } from '../api/storage';
import type { Role } from '../api/types';

const STORAGE_KEY = 'product-service-auth';

interface StoredAuth {
  token: string;
  username: string;
  roles: Role[];
}

interface AuthContextValue {
  token: string | null;
  username: string | null;
  roles: Role[];
  isAdmin: boolean;
  isLoading: boolean;
  error: string | null;
  login: (username: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [username, setUsername] = useState<string | null>(null);
  const [roles, setRoles] = useState<Role[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const raw = await storage.getItem(STORAGE_KEY);
        if (raw) {
          const parsed: StoredAuth = JSON.parse(raw);
          setAuthToken(parsed.token);
          setToken(parsed.token);
          setUsername(parsed.username);
          setRoles(parsed.roles);
        }
      } catch {
      } finally {
        setIsLoading(false);
      }
    })();
  }, []);

  const login = async (usernameInput: string, password: string): Promise<boolean> => {
    setError(null);
    try {
      const response = await api.login({ username: usernameInput, password });
      setAuthToken(response.token);
      setToken(response.token);
      setUsername(response.username);
      setRoles(response.roles);
      await storage.setItem(
        STORAGE_KEY,
        JSON.stringify({ token: response.token, username: response.username, roles: response.roles }),
      );
      return true;
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        setError('Invalid username or password.');
      } else if (e instanceof ApiError) {
        setError(e.message);
      } else {
        setError('Unexpected error while logging in.');
      }
      return false;
    }
  };

  const logout = async () => {
    setAuthToken(null);
    setToken(null);
    setUsername(null);
    setRoles([]);
    await storage.removeItem(STORAGE_KEY);
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      username,
      roles,
      isAdmin: roles.includes('ADMIN'),
      isLoading,
      error,
      login,
      logout,
    }),
    [token, username, roles, isLoading, error],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
