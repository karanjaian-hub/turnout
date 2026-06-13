import axios, { AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';

// ─── In-memory token store ────────────────────────────────────────────────────
// The access token NEVER touches localStorage or sessionStorage.
// It lives here — wiped on page refresh, which forces a silent refresh via cookie.
let accessToken: string | null = null;

export const setAccessToken = (token: string | null): void => {
  accessToken = token;
};

export const getAccessToken = (): string | null => accessToken;

// ─── Axios instance ───────────────────────────────────────────────────────────
const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
  // Send cookies (refresh token httpOnly cookie) on every request
  withCredentials: true,
});

// ─── Request interceptor: attach access token ────────────────────────────────
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// ─── Response interceptor: silent token refresh on 401 ───────────────────────
// We use a flag to prevent infinite retry loops if the refresh endpoint itself
// returns a 401 (e.g. refresh token expired — user must log in again).
let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

// When a refresh completes, replay all requests that queued up while waiting
const onRefreshComplete = (token: string): void => {
  refreshSubscribers.forEach(callback => callback(token));
  refreshSubscribers = [];
};

// Queue a failed request to be retried once the refresh resolves
const addRefreshSubscriber = (callback: (token: string) => void): void => {
  refreshSubscribers.push(callback);
};

api.interceptors.response.use(
  // Happy path — just pass the response through
  response => response,

  async error => {
    const originalRequest: AxiosRequestConfig & { _retry?: boolean } = error.config;

    // Only attempt refresh on 401, and only once per request
    const is401 = error.response?.status === 401;
    const alreadyRetried = originalRequest._retry;
    const isRefreshEndpoint = originalRequest.url?.includes('/api/auth/refresh');

    if (!is401 || alreadyRetried || isRefreshEndpoint) {
      return Promise.reject(error);
    }

    // If a refresh is already in flight, queue this request behind it
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

    // We're the first 401 — kick off the refresh
    originalRequest._retry = true;
    isRefreshing = true;

    try {
      // The refresh token is in an httpOnly cookie — withCredentials sends it
      const { data } = await api.post('/api/auth/refresh', {});
      const newToken: string = data.accessToken;

      setAccessToken(newToken);
      onRefreshComplete(newToken);

      // Retry the original request with the new token
      originalRequest.headers = {
        ...originalRequest.headers,
        Authorization: `Bearer ${newToken}`,
      };
      return api(originalRequest);

    } catch (refreshError) {
      // Refresh failed — session is dead, force logout
      setAccessToken(null);
      refreshSubscribers = [];
      window.dispatchEvent(new CustomEvent('auth:logout'));
      return Promise.reject(refreshError);

    } finally {
      isRefreshing = false;
    }
  }
);

export default api;
