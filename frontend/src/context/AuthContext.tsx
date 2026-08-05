import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import api, { setAccessToken, setRefreshToken } from '../services/api';
import { AuthUser, AuthResponse, UserResponse } from '../types/auth';

// ─── Context shape ────────────────────────────────────────────────────────────
interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  isSuperAdmin: () => boolean;
  isAdmin: () => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

// ─── Provider ─────────────────────────────────────────────────────────────────
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true); // true while we attempt silent refresh

  // Build an AuthUser from a UserResponse
  const hydrateUser = (profile: UserResponse): AuthUser => ({
    id:       profile.id,
    username: profile.username,
    email:    profile.email,
    role:     profile.role as AuthUser['role'],
  });

  // Fetch /me and store the user — called after any successful token acquisition
  const loadCurrentUser = useCallback(async (): Promise<void> => {
    const { data } = await api.get<UserResponse>('/api/auth/me');
    setUser(hydrateUser(data));
  }, []);

  // ── Silent refresh on mount ──────────────────────────────────────────────────
  // If the browser has a valid refresh cookie, this restores the session
  // transparently — the user never sees the login page.
  useEffect(() => {
    const attemptSilentRefresh = async (): Promise<void> => {
      try {
        const { data } = await api.post<AuthResponse>('/api/auth/refresh', {});
        setAccessToken(data.accessToken);
        setRefreshToken(data.refreshToken);
        await loadCurrentUser();
      } catch {
        // No valid session — user will need to log in manually
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    };

    attemptSilentRefresh();
  }, [loadCurrentUser]);

  // ── Listen for forced logout from the Axios interceptor ─────────────────────
  useEffect(() => {
    const handleForcedLogout = (): void => {
      setUser(null);
      setAccessToken(null);
    };

    window.addEventListener('auth:logout', handleForcedLogout);
    return () => window.removeEventListener('auth:logout', handleForcedLogout);
  }, []);

  // ── Login ────────────────────────────────────────────────────────────────────
  const login = async (username: string, password: string): Promise<void> => {
    const { data } = await api.post<AuthResponse>('/api/auth/login', {
      username,
      password,
    });

    // Block non-admin roles before they get any further
    if (data.role === 'EVENT_ORGANIZER') {
      throw new Error('Admin access only. Please use the organizer app.');
    }

    setAccessToken(data.accessToken);
    setRefreshToken(data.refreshToken);
    await loadCurrentUser();
  };

  // ── Logout ───────────────────────────────────────────────────────────────────
  const logout = async (): Promise<void> => {
    try {
      await api.post('/api/auth/logout');
    } catch {
      // Even if the server call fails, clear client state
    } finally {
      setAccessToken(null);
      setRefreshToken(null);
      setUser(null);
    }
  };

  const isSuperAdmin = (): boolean => user?.role === 'SUPER_ADMIN';
  const isAdmin = (): boolean =>
    user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN';

  return (
    <AuthContext.Provider value={{
      user,
      isAuthenticated: !!user,
      isLoading,
      login,
      logout,
      isSuperAdmin,
      isAdmin,
    }}>
      {children}
    </AuthContext.Provider>
  );
};

// ─── Hook ─────────────────────────────────────────────────────────────────────
// Components call useAuth() — they never import AuthContext directly.
// The null check here means forgetting the Provider causes a clear error,
// not a silent undefined crash.
export const useAuth = (): AuthContextValue => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return context;
};
