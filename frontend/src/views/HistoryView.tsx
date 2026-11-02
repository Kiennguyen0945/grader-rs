import { useMemo, useState } from 'react';
import { Empty, fmtAgo, GradeCell, ListIcon, Search, SubPill, subStatusLabel, Terminal } from '../components/SharedUI';
import { useOverview } from '../hooks/useOverview';
import { useSetCrumb } from '../state/crumb';
import { navigate } from '../router';
import type { SubmissionStatus } from '../types';

const STATUSES: SubmissionStatus[] = [
  'QUEUED',
  'RUNNING',
  'ACCEPTED',
  'WRONG_ANSWER',
  'COMPILE_ERROR',
  'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED',
];

export function HistoryView() {
  const { loading, error, courses, assignments, submissionsByAssignment, reload } = useOverview();
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('');
  const [courseCode, setCourseCode] = useState('');
  useSetCrumb(['Home'], 'Submissions');

  const rows = useMemo(() => {
    const all = assignments.flatMap((assignment) =>
      (submissionsByAssignment[assignment.id] ?? []).map((sub) => ({ sub, assignment })),
    );
    return all
      .filter(({ sub, assignment }) => {
        const q = query.trim().toLowerCase();
        const matchesQuery = !q || `${assignment.code} ${assignment.title}`.toLowerCase().includes(q);
        const matchesStatus = !status || sub.status === status;
        const matchesCourse = !courseCode || assignment.courseCode === courseCode;
        return matchesQuery && matchesStatus && matchesCourse;
      })
      .sort((a, b) => {
        const at = a.sub.submittedAt ? new Date(a.sub.submittedAt).getTime() : 0;
        const bt = b.sub.submittedAt ? new Date(b.sub.submittedAt).getTime() : 0;
        if (at !== bt) return bt - at;
        return b.sub.submissionId - a.sub.submissionId;
      });
  }, [assignments, submissionsByAssignment, query, status, courseCode]);

  if (loading) {
    return (
      <section className="screen on" id="scr-history">
        <div style={{ padding: '60px', textAlign: 'center' }} className="mut">
          <span className="spin" /> Loading submissions…
        </div>
      </section>
    );
  }

  return (
    <section className="screen on" id="scr-history">
      <span className="kicker">your activity</span>
      <h1 className="pagetitle" style={{ margin: '6px 0 16px' }}>
        Submission history
      </h1>

      <div className="toolbar">
        <div className="searchbox" style={{ width: '240px' }}>
          <Search />
          <input id="hf-q" placeholder="Search assignment…" value={query} onChange={(e) => setQuery(e.target.value)} />
        </div>
        <select className="mini" id="hf-status" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {subStatusLabel(s)}
            </option>
          ))}
        </select>
        <select className="mini" id="hf-course" value={courseCode} onChange={(e) => setCourseCode(e.target.value)}>
          <option value="">All courses</option>
          {courses.map((c) => (
            <option key={c.id} value={c.code}>
              {c.code}
            </option>
          ))}
        </select>
      </div>

      {error && (
        <div className="banner b-err" style={{ marginBottom: '16px' }}>
          <div className="bic">
            <Terminal />
          </div>
          <div>
            <h3>COULD NOT LOAD</h3>
            <p>{error}</p>
          </div>
          <span className="spacer" style={{ marginLeft: 'auto' }} />
          <button className="btn btn-sec btn-sm" onClick={reload}>
            Retry
          </button>
        </div>
      )}

      <div className="card">
        <table className="tbl hoverable">
          <thead>
            <tr>
              <th>Assignment</th>
              <th>Course</th>
              <th className="num">Att.</th>
              <th>Status</th>
              <th className="num">Grade</th>
              <th>Submitted</th>
              <th></th>
            </tr>
          </thead>
          <tbody id="hf-rows">
            {rows.map(({ sub, assignment }) => (
              <tr
                key={sub.submissionId}
                className="clickable"
                onClick={() => navigate(`/assignments/${assignment.id}`)}
              >
                <td>
                  <b>
                    {assignment.code}: {assignment.title}
                  </b>
                </td>
                <td className="mono dim">{assignment.courseCode}</td>
                <td className="num mono dim">#{sub.attemptNumber}</td>
                <td>
                  <SubPill status={sub.status} />
                </td>
                <td className="num">
                  <GradeCell grade={sub.grade} />
                </td>
                <td className="mut small">{fmtAgo(sub.submittedAt)}</td>
                <td className="num">
                  <button
                    className="btn btn-ghost btn-sm"
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate(`/assignments/${assignment.id}`);
                    }}
                  >
                    Open in workspace
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {rows.length === 0 && (
          <Empty icon={<ListIcon />} padding={40}>
            {assignments.length === 0 ? 'You have not submitted anything yet.' : 'No submissions match your filters.'}
          </Empty>
        )}
      </div>
    </section>
  );
}
