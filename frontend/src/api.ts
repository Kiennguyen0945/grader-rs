export const API_BASE = '/api';

export function getToken(): string | null {
  try {
    return localStorage.getItem('lamplight-token');
  } catch {
    return null;
  }
}

export function setToken(token: string | null) {
  try {
    if (token) {
      localStorage.setItem('lamplight-token', token);
    } else {
      localStorage.removeItem('lamplight-token');
    }
  } catch {}
}

/** JWT `exp` in ms, or null when the token is opaque/malformed (demo builds). */
function tokenExpiry(token: string): number | null {
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;
    const json = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    return typeof json.exp === 'number' ? json.exp * 1000 : null;
  } catch {
    return null;
  }
}

function isExpired(token: string): boolean {
  const expiry = tokenExpiry(token);
  return expiry !== null && expiry <= Date.now();
}

/** Exposed so the auth provider can drop an expired token on page load. */
export const isTokenExpired = isExpired;

/** Clears the stored session and lets `AuthProvider` fall back to the login screen. */
function endSession(message: string): never {
  setToken(null);
  try {
    localStorage.removeItem('lamplight-username');
  } catch {}
  window.dispatchEvent(new Event('auth:unauthorized'));
  throw new Error(message);
}

export interface ApiRequestOptions extends Omit<RequestInit, 'body'> {
  body?: any;
}

export async function apiRequest<T = any>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };

  const token = getToken();
  // The backend answers 401 for expired tokens, but stopping here avoids a pointless
  // round-trip and makes the redirect to the login screen instant.
  if (token && isExpired(token)) {
    endSession('Your session expired. Please sign in again.');
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  let body = options.body;
  if (body && typeof body === 'object' && !(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(body);
  }

  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
      body,
    });
  } catch {
    // Browser-level failure: the dev server / backend is down, DNS failed, offline, etc.
    // Without this the login form shows the opaque "NetworkError when attempting to fetch".
    throw new Error(`Cannot reach the API at ${API_BASE} — is the backend running?`);
  }

  if (res.status === 401) {
    endSession('Session expired. Please sign in again.');
  }

  const contentType = res.headers.get('content-type') || '';
  const data = contentType.includes('application/json') ? await res.json() : await res.text();

  if (!res.ok) {
    const errorMsg = (data && (data.message || data.error)) || `Request failed with status ${res.status}`;
    throw new Error(errorMsg);
  }

  return data as T;
}

