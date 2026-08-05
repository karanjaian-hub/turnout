import axios, { AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';

// ─── In-memory token store ────────────────────────────────────────────────────
// Access token lives in memory — never localStorage (XSS risk).
// Refresh token also stored in memory since the backend no longer uses httpOnly cookies.
let accessToken:  string | null = null;
let refreshToken: string | null = null;

export const setAccessToken  = (token: string | null): void => { accessToken  = token; };
export const setRefreshToken = (token: string | null): void => { refreshToken = token; };
export const getAccessToken  = (): string | null => accessToken;
export const getRefreshToken = (): string | null => refreshToken;

// ─── Axios instance ───────────────────────────────────────────────────────────
const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
  withCredentials: true,
});

// ─── Request interceptor: attach access token ────────────────────────────────
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// ─── Response interceptor ─────────────────────────────────────────────────────
let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

const onRefreshComplete = (token: string): void => {
  refreshSubscribers.forEach(cb => cb(token));
  refreshSubscribers = [];
};

const addRefreshSubscriber = (callback: (token: string) => void): void => {
  refreshSubscribers.push(callback);
};

// Unwrap the ApiResponse envelope — { status, message, data: T } -> T
const unwrap = (data: any): any => {
  if (data && data.status === 1 && 'data' in data) return data.data;
  return data;
};

api.interceptors.response.use(
  // Happy path — unwrap the ApiResponse envelope globally
  // Before: api.get('/foo') returned { success, message, data: {...} }
  // After:  api.get('/foo') returns {...} directly
  response => {
    response.data = unwrap(response.data);
    return response;
  },

  async error => {
    const originalRequest: AxiosRequestConfig & { _retry?: boolean } = error.config;

    const is401             = error.response?.status === 401;
    const alreadyRetried    = originalRequest._retry;
    const isRefreshEndpoint = originalRequest.url?.includes('/api/auth/refresh');

    if (!is401 || alreadyRetried || isRefreshEndpoint) {
      return Promise.reject(error);
    }

    // Queue requests behind an in-flight refresh
    if (isRefreshing) {
      return new Promise(resolve => {
        addRefreshSubscriber((newToken: string) => {
          originalRequest.headers = {
            ...originalRequest.headers,
            Authorization: `Bearer ${newToken}`,
          };
          resolve(api(originalRequest));
        });
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      // Send refresh token in request body — backend requires { refreshToken: "..." }
      const storedRefreshToken = refreshToken;
      if (!storedRefreshToken) throw new Error('No refresh token available');

      const { data } = await api.post('/api/auth/refresh', {
        refreshToken: storedRefreshToken,
      });

      // Unwrap envelope — new token may be at data.accessToken or data.data.accessToken
      // The response interceptor above already unwrapped it, so just read data.accessToken
      const newAccessToken: string  = data.accessToken;
      const newRefreshToken: string = data.refreshToken ?? storedRefreshToken;

      setAccessToken(newAccessToken);
      setRefreshToken(newRefreshToken);
      onRefreshComplete(newAccessToken);

      originalRequest.headers = {
        ...originalRequest.headers,
        Authorization: `Bearer ${newAccessToken}`,
      };
      return api(originalRequest);

    } catch (refreshError) {
      setAccessToken(null);
      setRefreshToken(null);
      refreshSubscribers = [];
      window.dispatchEvent(new CustomEvent('auth:logout'));
      return Promise.reject(refreshError);

    } finally {
      isRefreshing = false;
    }
  }
);

export default api;
