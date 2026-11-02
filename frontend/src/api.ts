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

export interface ApiRequestOptions extends Omit<RequestInit, 'body'> {
  body?: any;
}

export async function apiRequest<T = any>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };

  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  let body = options.body;
  if (body && typeof body === 'object' && !(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(body);
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    body,
  });

  if (res.status === 401) {
    setToken(null);
    window.dispatchEvent(new Event('auth:unauthorized'));
    throw new Error('Session expired. Please sign in again.');
  }

  const contentType = res.headers.get('content-type') || '';
  const data = contentType.includes('application/json') ? await res.json() : await res.text();

  if (!res.ok) {
    const errorMsg = (data && (data.message || data.error)) || `Request failed with status ${res.status}`;
    throw new Error(errorMsg);
  }

  return data as T;
}

