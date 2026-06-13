// The shape of the user object we keep in memory after login.
// We only store what the UI actually needs — not the full backend User entity.
export interface AuthUser {
  id: string;
  username: string;
  email: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'EVENT_ORGANIZER';
}

// What the backend returns on POST /api/auth/login and POST /api/auth/refresh
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  role: string;
}

// What GET /api/auth/me returns
export interface UserResponse {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: string;
  status: string;
  emailVerified: boolean;
}
