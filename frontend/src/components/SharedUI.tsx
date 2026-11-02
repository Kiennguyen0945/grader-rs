import React, { useEffect, useState } from 'react';
import type { AssignmentStatus, SubmissionStatus } from '../types';

/* ---------------------------------------------------------------------------
 * Icons — copied from the prototype markup (same paths, same stroke widths)
 * ------------------------------------------------------------------------- */

export interface IconProps {
  size?: number;
  className?: string;
}

const svgProps = (size: number) => ({
  width: size,
  height: size,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
} as const);

export const Book = ({ size = 17 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.8">
    <path d="M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2V5Z" />
    <path d="M4 19a2 2 0 0 1 2-2h13" />
  </svg>
);

export const Clock = ({ size = 14 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.8">
    <circle cx="12" cy="12" r="9" />
    <path d="M12 7v5l3.5 2" />
  </svg>
);

export const Send = ({ size = 15 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
    <path d="M22 2 11 13M22 2l-7 20-4-9-9-4 20-7Z" />
  </svg>
);

export const Lamp = ({ size = 15 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.8" strokeLinecap="round">
    <path d="M9 3h6l1.5 5h-9L9 3Z" />
    <path d="M12 8v3" />
    <circle cx="12" cy="13.5" r="2.4" />
    <path d="M7.5 18.5c1-1.6 8-1.6 9 0" opacity=".6" />
  </svg>
);

export const ArrowRight = ({ size = 15 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="2" strokeLinecap="round">
    <path d="M4 12h16m0 0-6-6m6 6-6 6" />
  </svg>
);

export const ChevronDown = ({ size = 14 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="2" strokeLinecap="round">
    <path d="m6 9 6 6 6-6" />
  </svg>
);

export const ChevronRight = ({ size = 14, className }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="2" strokeLinecap="round" className={className}>
    <path d="m9 5 7 7-7 7" />
  </svg>
);

export const Search = ({ size = 14 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.9">
    <circle cx="11" cy="11" r="7" />
    <path d="m20 20-3.5-3.5" />
  </svg>
);

export const Copy = ({ size = 14 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.8">
    <rect x="9" y="9" width="12" height="12" rx="2" />
    <path d="M5 15V5a2 2 0 0 1 2-2h10" />
  </svg>
);

export const Reset = ({ size = 14 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.9" strokeLinecap="round">
    <path d="M20 12a8 8 0 1 1-2.3-5.6M20 4v5h-5" />
  </svg>
);

export const Focus = ({ size = 15 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.9" strokeLinecap="round">
    <path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5" />
  </svg>
);

export const Trophy = ({ size = 15 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.8">
    <path d="M8 4h8v5a4 4 0 0 1-8 0V4Z" />
    <path d="M8 5H4.5a3.5 3.5 0 0 0 3.6 3.9M16 5h3.5a3.5 3.5 0 0 1-3.6 3.9" />
    <path d="M12 13v4m-4 3h8m-6.5-3h5" />
  </svg>
);

export const ListIcon = ({ size = 17 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.8">
    <path d="M9 6h12M9 12h12M9 18h12" />
    <circle cx="4" cy="6" r="1.3" fill="currentColor" />
    <circle cx="4" cy="12" r="1.3" fill="currentColor" />
    <circle cx="4" cy="18" r="1.3" fill="currentColor" />
  </svg>
);

export const Bell = ({ size = 18 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.8">
    <path d="M6 9a6 6 0 1 1 12 0c0 5 2 6 2 6H4s2-1 2-6" />
    <path d="M10 19a2.2 2.2 0 0 0 4 0" />
  </svg>
);

export const Sun = ({ size = 17, className }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.9" strokeLinecap="round" className={className}>
    <circle cx="12" cy="12" r="4" />
    <path d="M12 2v2m0 16v2M4.9 4.9l1.4 1.4m11.4 11.4 1.4 1.4M2 12h2m16 0h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
  </svg>
);

export const Moon = ({ size = 17, className }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.9" strokeLinecap="round" className={className}>
    <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z" />
  </svg>
);

export const Eye = ({ size = 16 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.8">
    <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z" />
    <circle cx="12" cy="12" r="2.6" />
  </svg>
);

export const Check = ({ size = 15 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="2.4" strokeLinecap="round">
    <path d="m4.5 12.5 5 5 10-11" />
  </svg>
);

export const SignOut = ({ size = 18 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.8">
    <path d="M15 4h4v16h-4" />
    <path d="M3 12h11m0 0-4-4m4 4-4 4" />
  </svg>
);

export const Terminal = ({ size = 15 }: IconProps) => (
  <svg {...svgProps(size)} strokeWidth="1.9" strokeLinecap="round">
    <path d="m8 7-5 5 5 5m8-10 5 5-5 5" />
  </svg>
);

/* ---------------------------------------------------------------------------
 * Status maps (prototype pills / banners)
 * ------------------------------------------------------------------------- */

const SUB_STATUS_CLASS: Record<SubmissionStatus, string> = {
  QUEUED: 'st-queued',
  RUNNING: 'st-run',
  ACCEPTED: 'st-ok',
  WRONG_ANSWER: 'st-err',
  COMPILE_ERROR: 'st-ce',
  TIME_LIMIT_EXCEEDED: 'st-tle',
  MEMORY_LIMIT_EXCEEDED: 'st-mle',
  RUNTIME_ERROR: 'st-rte',
  SYSTEM_ERROR: 'st-sys',
  NOT_RUN: 'st-queued',
  OUTPUT_LIMIT_EXCEEDED: 'st-err',
};

const SUB_STATUS_LABEL: Record<SubmissionStatus, string> = {
  QUEUED: 'queued',
  RUNNING: 'running',
  ACCEPTED: 'accepted',
  WRONG_ANSWER: 'wrong answer',
  COMPILE_ERROR: 'compile error',
  TIME_LIMIT_EXCEEDED: 'time limit exceeded',
  MEMORY_LIMIT_EXCEEDED: 'memory limit exceeded',
  RUNTIME_ERROR: 'runtime error',
  SYSTEM_ERROR: 'system error',
  NOT_RUN: 'not run',
  OUTPUT_LIMIT_EXCEEDED: 'output limit exceeded',
};

const ASG_STATUS_CLASS: Record<AssignmentStatus, string> = {
  DRAFT: 'st-draft',
  PUBLISHED: 'st-pub',
  CLOSED: 'st-closed',
};

const ASG_STATUS_LABEL: Record<AssignmentStatus, string> = {
  DRAFT: 'draft',
  PUBLISHED: 'published',
  CLOSED: 'closed',
};

export const subStatusClass = (s: SubmissionStatus) => SUB_STATUS_CLASS[s] ?? 'st-queued';
export const subStatusLabel = (s: SubmissionStatus) => SUB_STATUS_LABEL[s] ?? String(s).toLowerCase();
export const asgStatusClass = (s: AssignmentStatus) => ASG_STATUS_CLASS[s] ?? 'st-draft';
export const asgStatusLabel = (s: AssignmentStatus) => ASG_STATUS_LABEL[s] ?? String(s).toLowerCase();

export function AsgPill({ status }: { status: AssignmentStatus }) {
  return (
    <span className={`pill ${asgStatusClass(status)}`}>
      <span className="dot" />
      {asgStatusLabel(status)}
    </span>
  );
}

export function SubPill({ status }: { status: SubmissionStatus }) {
  return (
    <span className={`pill ${subStatusClass(status)}`}>
      <span className="dot" />
      {subStatusLabel(status)}
    </span>
  );
}

export const isTerminal = (status: SubmissionStatus) => status !== 'QUEUED' && status !== 'RUNNING';

export interface BannerInfo {
  cls: string;
  title: string;
  text: string;
  icon: 'clock' | 'check' | 'eye';
}

export function bannerFor(
  status: SubmissionStatus,
  limits?: { timeLimitMs?: number; memoryLimitKib?: number },
): BannerInfo {
  const time = limits?.timeLimitMs ? `${(limits.timeLimitMs / 1000).toFixed(1)}s` : 'the';
  const memory = limits?.memoryLimitKib ? `${Math.round(limits.memoryLimitKib / 1024)}MB` : 'the';
  switch (status) {
    case 'QUEUED':
      return { cls: 'b-q', title: 'QUEUED', text: 'Waiting for a free runner — your submission is safe in the queue.', icon: 'clock' };
    case 'RUNNING':
      return { cls: 'b-run', title: 'RUNNING', text: 'Sandbox compiling and executing your tests…', icon: 'clock' };
    case 'ACCEPTED':
      return { cls: 'b-ok', title: 'ACCEPTED', text: 'Every test matched the expected output.', icon: 'check' };
    case 'COMPILE_ERROR':
      return { cls: 'b-ce', title: 'COMPILE ERROR', text: 'Your code did not compile. Check the first error below.', icon: 'eye' };
    case 'TIME_LIMIT_EXCEEDED':
      return { cls: 'b-tle', title: 'TIME LIMIT EXCEEDED', text: `Execution exceeded the ${time} time limit.`, icon: 'clock' };
    case 'MEMORY_LIMIT_EXCEEDED':
      return { cls: 'b-mle', title: 'MEMORY LIMIT EXCEEDED', text: `Execution exceeded the ${memory} memory limit.`, icon: 'clock' };
    case 'RUNTIME_ERROR':
      return { cls: 'b-err', title: 'RUNTIME ERROR', text: 'Your program exited with a non-zero status.', icon: 'eye' };
    case 'SYSTEM_ERROR':
      return { cls: 'b-err', title: 'SYSTEM ERROR', text: 'The judge could not run this submission. Try resubmitting.', icon: 'eye' };
    case 'WRONG_ANSWER':
      return { cls: 'b-err', title: 'WRONG ANSWER', text: 'Output differed from the expected output.', icon: 'eye' };
    default:
      return { cls: 'b-q', title: 'NOT RUN', text: 'This submission was not judged.', icon: 'clock' };
  }
}

/* ---------------------------------------------------------------------------
 * Small presentational pieces
 * ------------------------------------------------------------------------- */

export function GradeCell({ grade }: { grade?: number | null }) {
  if (grade === null || grade === undefined) return <span className="dim">—</span>;
  const cls = grade >= 8 ? 'gradegood' : grade >= 5 ? 'grademid' : 'gradebad';
  return <span className={`${cls} mono`}>{grade.toFixed(1)}</span>;
}

export function initialsOf(name: string) {
  const cleaned = (name || '').trim();
  if (!cleaned) return '??';
  const parts = cleaned.split(/[\s._-]+/).filter(Boolean);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

export function displayName(username: string) {
  return username
    .split(/[._-]+/)
    .filter(Boolean)
    .map((p) => p.charAt(0).toUpperCase() + p.slice(1))
    .join(' ');
}

export function Avatar({
  name,
  size = 32,
  radius = 10,
  role = 'student',
}: {
  name: string;
  size?: number;
  radius?: number;
  role?: 'student' | 'lecturer' | 'admin';
}) {
  return (
    <span
      className={`avatar av-${role}`}
      style={{ width: size, height: size, fontSize: Math.round(size * 0.36), borderRadius: radius }}
    >
      {initialsOf(name)}
    </span>
  );
}

export function Empty({
  icon,
  children,
  padding,
}: {
  icon: React.ReactNode;
  children: React.ReactNode;
  padding?: number;
}) {
  return (
    <div className="empty" style={padding ? { padding } : undefined}>
      <div className="ei">{icon}</div>
      <p>{children}</p>
    </div>
  );
}

export const Spinner = ({ className }: { className?: string }) => (
  <span className={className ? `spin ${className}` : 'spin'} />
);

/* ---------------------------------------------------------------------------
 * Date / countdown helpers
 * ------------------------------------------------------------------------- */

const asDate = (value?: string | null): Date | null => {
  if (!value) return null;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d;
};

/** "23 Oct · 22:55" */
export function fmtShort(value?: string | null): string {
  const d = asDate(value);
  if (!d) return '—';
  return `${d.getDate()} ${d.toLocaleString('en-GB', { month: 'short' })} · ${d
    .toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}`;
}

/** "Fri 23 Oct, 22:55" */
export function fmtLong(value?: string | null): string {
  const d = asDate(value);
  if (!d) return '—';
  return `${d.toLocaleString('en-GB', { weekday: 'short' })} ${d.getDate()} ${d.toLocaleString('en-GB', {
    month: 'short',
  })}, ${d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}`;
}

/** "5h ago" / "16d ago" (used where the API provides a timestamp) */
export function fmtAgo(value?: string | null): string {
  const d = asDate(value);
  if (!d) return '—';
  const diff = Date.now() - d.getTime();
  if (diff < 60_000) return 'just now';
  const mins = Math.round(diff / 60_000);
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.round(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  return `${days}d ago`;
}

/** "2d 09h 41m" */
export function fmtCountdown(ms: number): string {
  if (ms <= 0) return 'closed';
  const totalMins = Math.floor(ms / 60_000);
  const days = Math.floor(totalMins / (60 * 24));
  const hours = Math.floor((totalMins % (60 * 24)) / 60);
  const mins = totalMins % 60;
  if (days > 0) return `${days}d ${String(hours).padStart(2, '0')}h ${String(mins).padStart(2, '0')}m`;
  if (hours > 0) return `${hours}h ${String(mins).padStart(2, '0')}m`;
  const secs = Math.max(0, Math.floor((ms % 60_000) / 1000));
  return `${mins}m ${String(secs).padStart(2, '0')}s`;
}

export function useCountdown(until?: string | null): number | null {
  const target = asDate(until)?.getTime() ?? null;
  const [remaining, setRemaining] = useState<number | null>(target ? target - Date.now() : null);

  useEffect(() => {
    if (!target) {
      setRemaining(null);
      return;
    }
    setRemaining(target - Date.now());
    const id = window.setInterval(() => setRemaining(target - Date.now()), 1000);
    return () => window.clearInterval(id);
  }, [target]);

  return remaining;
}

/** Live `.cd` span, exactly like the prototype's `data-until` placeholders. */
export function Countdown({
  until,
  urgent,
  fallback = '—',
}: {
  until?: string | null;
  urgent?: boolean;
  fallback?: string;
}) {
  const remaining = useCountdown(until);
  if (remaining === null) return <span className={urgent ? 'cd urgent' : 'cd'}>{fallback}</span>;
  const isUrgent = urgent ?? remaining < 24 * 60 * 60 * 1000;
  return <span className={isUrgent ? 'cd urgent' : 'cd'}>{fmtCountdown(remaining)}</span>;
}

/* ---------------------------------------------------------------------------
 * Tiny storage hook (drafts, theme, remembered username)
 * ------------------------------------------------------------------------- */

export function useLocalStorage(key: string, initial: string) {
  const [value, setValue] = useState<string>(() => {
    try {
      return localStorage.getItem(key) ?? initial;
    } catch {
      return initial;
    }
  });

  useEffect(() => {
    try {
      localStorage.setItem(key, value);
    } catch {}
  }, [key, value]);

  return [value, setValue] as const;
}



