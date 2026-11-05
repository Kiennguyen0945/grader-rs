import { useEffect, useMemo, useState } from 'react';
import {
  Avatar,
  Countdown,
  Empty,
  GradeCell,
  Lamp,
  Search,
  SubPill,
  Terminal,
  displayName,
  fmtAgo,
  isTerminal,
  subStatusLabel,
} from '../components/SharedUI';
import { useSetCrumb } from '../state/crumb';
import { data } from '../data';
import { navigate } from '../router';
import type { AssignmentDetail, SubmissionStatus, SubmissionSummary } from '../types';

const STATUSES: SubmissionStatus[] = [
  'QUEUED',
  'RUNNING',
  'ACCEPTED',
  'WRONG_ANSWER',
  'COMPILE_ERROR',
  'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED',
  'RUNTIME_ERROR',
  'SYSTEM_ERROR',
];

export function ReviewQueueView({ assignmentId }: { assignmentId: number }) {
  const [assignment, setAssignment] = useState<AssignmentDetail | null>(null);
  const [rows, setRows] = useState<SubmissionSummary[]>([]);
  const [statusFilter, setStatusFilter] = useState<'ALL' | SubmissionStatus>('ALL');
  const [latestOnly, setLatestOnly] = useState(true);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useSetCrumb(['Teaching'], assignment ? `${assignment.code} · Submissions` : 'Submissions');

  async function load(showSpinner = true) {
    if (showSpinner) setLoading(true);
    try {
      const [detail, list] = await Promise.all([
        data.assignment(assignmentId),
        data.assignmentSubmissions(assignmentId),
      ]);
      setAssignment(detail);
      setRows(list);
      setError('');
    } catch (err: any) {
      setError(err?.message || 'Could not load the submissions.');
    } finally {
      if (showSpinner) setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [assignmentId]);

  // Live queue: poll while anything is still queued or running.
  const hasActive = rows.some((r) => !isTerminal(r.status));
  useEffect(() => {
    if (!hasActive) return;
    const id = window.setInterval(() => load(false), 3000);
    return () => window.clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasActive, assignmentId]);

  const latestByStudent = useMemo(() => {
    const best = new Map<number, SubmissionSummary>();
    for (const row of rows) {
      const current = best.get(row.userId);
      if (!current || row.attemptNumber > current.attemptNumber) best.set(row.userId, row);
    }
    return [...best.values()];
  }, [rows]);

  const visible = useMemo(() => {
    const base = latestOnly ? latestByStudent : rows;
    const q = query.trim().toLowerCase();
    return base
      .filter((r) => statusFilter === 'ALL' || r.status === statusFilter)
      .filter((r) => `${r.username ?? ''} ${r.userId} ${r.attemptNumber}`.toLowerCase().includes(q))
      .sort((a, b) => new Date(b.submittedAt ?? 0).getTime() - new Date(a.submittedAt ?? 0).getTime());
  }, [latestOnly, latestByStudent, rows, statusFilter, query]);

  const uniqueStudents = new Set(rows.map((r) => r.userId)).size;
  const acceptedLatest = latestByStudent.filter((r) => r.status === 'ACCEPTED').length;
  const averageBest = useMemo(() => {
    const best = new Map<number, number>();
    for (const row of rows) {
      if (typeof row.grade !== 'number') continue;
      const current = best.get(row.userId);
      if (current === undefined || row.grade > current) best.set(row.userId, row.grade);
    }
    const grades = [...best.values()];
    if (grades.length === 0) return null;
    return grades.reduce((a, b) => a + b, 0) / grades.length;
  }, [rows]);

  if (loading) {
    return (
      <section className="screen on" id="scr-sublist">
        <div style={{ padding: '60px', textAlign: 'center' }} className="mut">
          <span className="spin" /> Loading submissions…
        </div>
      </section>
    );
  }

  if (!assignment) {
    return (
      <section className="screen on" id="scr-sublist">
        <div className="card pad">
          <p>{error || 'Assignment not found.'}</p>
          <button className="btn btn-sec btn-sm" style={{ marginTop: '12px' }} onClick={() => navigate('/teach')}>
            Back to dashboard
          </button>
        </div>
      </section>
    );
  }


  return (
    <section className="screen on" id="scr-sublist">
      <span className="kicker">{assignment.code} · submissions</span>
      <h1 className="pagetitle" style={{ margin: '6px 0 2px' }}>
        {assignment.title}
      </h1>
      <p className="mut">
        {assignment.assignmentStatus === 'PUBLISHED' && assignment.closingTime ? (
          <>
            Closes in <Countdown until={assignment.closingTime} fallback="no deadline" /> ·{' '}
            {assignment.submissionLimit ?? 3} attempts allowed
          </>
        ) : (
          <>
            {assignment.assignmentStatus.toLowerCase()} · {assignment.submissionLimit ?? 3} attempts
            allowed
          </>
        )}
      </p>

      <div className="toolbar" style={{ marginTop: '12px' }}>
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => navigate(`/teach/courses/${assignment.courseId}`)}
        >
          ← Course
        </button>
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => navigate(`/teach/assignments/${assignment.id}/tests`)}
        >
          Test cases
        </button>
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => navigate(`/teach/assignments/${assignment.id}/edit`)}
        >
          Settings
        </button>
        <span className="spacer" />
        <div className="searchbox" style={{ width: '230px' }}>
          <Search />
          <input placeholder="Student, attempt…" value={query} onChange={(e) => setQuery(e.target.value)} />
        </div>
        <select
          className="btn btn-sec btn-sm"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as 'ALL' | SubmissionStatus)}
        >
          <option value="ALL">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {subStatusLabel(s)}
            </option>
          ))}
        </select>
        <label className="small" style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
          <input type="checkbox" checked={latestOnly} onChange={(e) => setLatestOnly(e.target.checked)} />
          Latest attempt only
        </label>
      </div>

      {error && (
        <div className="notice red" style={{ marginBottom: '16px' }}>
          <Terminal />
          <span>{error}</span>
        </div>
      )}

      <div className="stats">
        <div className="stat">
          <div className="sv">{rows.length}</div>
          <div className="sl">Total submissions</div>
        </div>
        <div className="stat">
          <div className="sv">{uniqueStudents}</div>
          <div className="sl">Unique students</div>
        </div>
        <div className="stat">
          <div className="sv">{acceptedLatest}</div>
          <div className="sl">Accepted (latest)</div>
        </div>
        <div className="stat">
          <div className="sv">{averageBest === null ? '—' : averageBest.toFixed(1)}</div>
          <div className="sl">Average best grade</div>
        </div>
      </div>

      {hasActive && (
        <div className="notice violet" style={{ marginBottom: '16px' }}>
          <Lamp />
          <span>Grading in progress — this list refreshes every 3 seconds.</span>
        </div>
      )}

      <div className="card">
        <div className="cardhead">
          <h2 className="cardtitle">{latestOnly ? 'Latest attempt per student' : 'Every attempt'}</h2>
          <span className="spacer" />
          <span className="mono dim small">{visible.length}</span>
        </div>
        {visible.length === 0 ? (
          <Empty icon={<Terminal size={22} />}>
            No submissions match this filter yet. Students appear here as soon as they send code.
          </Empty>
        ) : (
          <table className="tbl hoverable">
            <thead>
              <tr>
                <th>Student</th>
                <th className="num">Attempt</th>
                <th>Status</th>
                <th className="num">Grade</th>
                <th>Submitted</th>
                <th className="num" />
              </tr>
            </thead>
            <tbody>
              {visible.map((s) => (
                <tr key={s.submissionId}>
                  <td>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '9px' }}>
                      <Avatar name={displayName(s.username ?? 'student')} size={26} radius={7} />
                      <span>
                        <div>{displayName(s.username ?? 'student')}</div>
                        <div className="mono dim small">@{s.username ?? s.userId}</div>
                      </span>
                    </span>
                  </td>
                  <td className="num mono dim">#{s.attemptNumber}</td>
                  <td>
                    <SubPill status={s.status} />
                  </td>
                  <td className="num">
                    <GradeCell grade={s.grade} />
                  </td>
                  <td className="mut small">{fmtAgo(s.submittedAt)}</td>
                  <td className="num">
                    <button
                      className="btn btn-ghost btn-sm"
                      onClick={() => navigate(`/teach/submissions/${s.submissionId}`)}
                    >
                      Review
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}
