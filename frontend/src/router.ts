import { useEffect, useState } from 'react';

export type Route =
  | { name: 'dashboard' }
  | { name: 'courses' }
  | { name: 'course'; courseId: number }
  | { name: 'workspace'; assignmentId: number }
  | { name: 'history' };

export function parseRoute(hash: string): Route {
  const clean = (hash || '')
    .replace(/^#/, '')
    .replace(/\?.*$/, '')
    .replace(/^\/+/, '');
  const parts = clean.split('/').filter(Boolean);
  if (parts.length === 0) return { name: 'dashboard' };
  if (parts[0] === 'courses' && parts[1]) {
    const id = Number(parts[1]);
    if (Number.isFinite(id)) return { name: 'course', courseId: id };
  }
  if (parts[0] === 'courses') return { name: 'courses' };
  if (parts[0] === 'assignments' && parts[1]) {
    const id = Number(parts[1]);
    if (Number.isFinite(id)) return { name: 'workspace', assignmentId: id };
  }
  if (parts[0] === 'history') return { name: 'history' };
  return { name: 'dashboard' };
}

export function useRoute(): Route {
  const [route, setRoute] = useState<Route>(() => parseRoute(window.location.hash));
  useEffect(() => {
    const onChange = () => setRoute(parseRoute(window.location.hash));
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);
  return route;
}

export function navigate(path: string) {
  const next = path.startsWith('#') ? path : `#${path.startsWith('/') ? path : `/${path}`}`;
  if (window.location.hash === next) return;
  window.location.hash = next;
}

export function useHashQuery(): URLSearchParams {
  const [query, setQuery] = useState(() => new URLSearchParams(window.location.hash.split('?')[1] ?? ''));
  useEffect(() => {
    const onChange = () => setQuery(new URLSearchParams(window.location.hash.split('?')[1] ?? ''));
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);
  return query;
}

export const routeHref = (path: string) => `#${path.startsWith('/') ? path : `/${path}`}`;

