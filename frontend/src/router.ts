import { useEffect, useState } from 'react';

export type Route =
  | { name: 'dashboard' }
  | { name: 'courses' }
  | { name: 'course'; courseId: number }
  | { name: 'workspace'; assignmentId: number }
  | { name: 'history' }
  /* lecturer ("teaching") routes */
  | { name: 'teach' }
  | { name: 'teach-courses' }
  | { name: 'teach-course'; courseId: number }
  | { name: 'teach-assignment-new'; courseId: number }
  | { name: 'teach-assignment'; assignmentId: number }
  | { name: 'teach-tests'; assignmentId: number }
  | { name: 'teach-queue'; assignmentId: number }
  | { name: 'teach-review'; submissionId: number };

export function parseRoute(hash: string): Route {
  const clean = (hash || '')
    .replace(/^#/, '')
    .replace(/\?.*$/, '')
    .replace(/^\/+/, '');
  const parts = clean.split('/').filter(Boolean);
  if (parts.length === 0) return { name: 'dashboard' };

  if (parts[0] === 'teach') {
    if (parts.length === 1) return { name: 'teach' };
    if (parts[1] === 'courses' && parts[2]) {
      const courseId = Number(parts[2]);
      if (Number.isFinite(courseId)) {
        if (parts[3] === 'assignments' && parts[4] === 'new') {
          return { name: 'teach-assignment-new', courseId };
        }
        return { name: 'teach-course', courseId };
      }
    }
    if (parts[1] === 'courses') return { name: 'teach-courses' };
    if (parts[1] === 'assignments' && parts[2]) {
      const id = Number(parts[2]);
      if (Number.isFinite(id)) {
        if (parts[3] === 'edit') return { name: 'teach-assignment', assignmentId: id };
        if (parts[3] === 'tests') return { name: 'teach-tests', assignmentId: id };
        if (parts[3] === 'submissions') return { name: 'teach-queue', assignmentId: id };
      }
    }
    if (parts[1] === 'submissions' && parts[2]) {
      const id = Number(parts[2]);
      if (Number.isFinite(id)) return { name: 'teach-review', submissionId: id };
    }
    return { name: 'teach' };
  }

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

/** Teaching routes only render for lecturers; student routes only for students. */
export const isTeachRoute = (route: Route) => route.name.startsWith('teach');

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

