import { useEffect, useMemo, useState } from 'react';
import {
  Avatar,
  ChevronRight,
  Clock,
  Empty,
  GradeCell,
  Lamp,
  SubPill,
  Terminal,
  TestPill,
  displayName,
  fmtAgo,
  isTerminal,
} from '../components/SharedUI';
import { useSetCrumb } from '../state/crumb';
import { data } from '../data';
import { navigate } from '../router';
import type { AssignmentDetail, SubmissionDetail, SubmissionSummary } from '../types';

export function SubmissionReviewView({ submissionId }: { submissionId: number }) {
  const [detail, setDetail] = useState<SubmissionDetail | null>(null);
  const [assignment, setAssignment] = useState<AssignmentDetail | null>(null);
  const [siblings, setSiblings] = useState<SubmissionSummary[]>([]);
  const [expanded, setExpanded] = useState<number[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useSetCrumb(
    ['Teaching'],
    detail ? `${displayName(detail.username ?? 'student')} · review` : 'Submission review',
  );

  async function load(showSpinner = true) {
    if (showSpinner) setLoading(true);
    try {
      const d = await data.submission(submissionId);
      const [a, list] = await Promise.all([
        data.assignment(d.assignmentId),
        data.assignmentSubmissions(d.assignmentId).catch(() => []),
      ]);
      setDetail(d);
      setAssignment(a);
      setSiblings(list);
      setError('');
    } catch (err: any) {
      setError(err?.message || 'Could not load this submission.');
    } finally {
      if (showSpinner) setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [submissionId]);

  const active = detail ? !isTerminal(detail.status) : false;
  useEffect(() => {
    if (!active) return;
    const id = window.setInterval(() => load(false), 2000);
    return () => window.clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active, submissionId]);

  const ordered = useMemo(
    () => [...siblings].sort((a, b) => b.submissionId - a.submissionId),
    [siblings],
  );
  const index = ordered.findIndex((s) => s.submissionId === submissionId);
  const prev = index > 0 ? ordered[index - 1] : null;
  const next = index >= 0 && index < ordered.length - 1 ? ordered[index + 1] : null;

  const tests = detail ? (detail.testResults ?? detail.sampleTestResults) : [];
  const failed = tests.filter((t) => t.status !== 'PASSED');
  const sourceLines = detail ? detail.sourceCode.split('\n') : [];

  if (loading) {
    return (
      <section className="screen on" id="scr-review">
        <div style={{ padding: '60px', textAlign: 'center' }} className="mut">
          <span className="spin" /> Loading submission…
        </div>
      </section>
    );
  }

  if (!detail || !assignment) {
    return (
      <section className="screen on" id="scr-review">
        <div className="card pad">
          <p>{error || 'Submission not found.'}</p>
          <button className="btn btn-sec btn-sm" style={{ marginTop: '12px' }} onClick={() => navigate('/teach')}>
            Back to dashboard
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="screen on" id="scr-review">
      <span className="kicker">
        {assignment.code} · {assignment.title}
      </span>
      <h1 className="pagetitle" style={{ margin: '6px 0 8px' }}>
        {displayName(detail.username ?? 'student')}
      </h1>

      <div className="toolbar">
        <Avatar name={displayName(detail.username ?? 'student')} size={30} radius={9} />
        <span className="mono dim small">@{detail.username ?? detail.userId}</span>
        <SubPill status={detail.status} />
        <span className="mono dim small">attempt #{detail.attemptNumber}</span>
        <span className="dim small">submitted {fmtAgo(detail.submittedAt)}</span>
        <span className="spacer" />
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => navigate(`/teach/assignments/${assignment.id}/submissions`)}
        >
          All submissions
        </button>
        <button
          className="btn btn-ghost btn-sm"
          disabled={!prev}
          onClick={() => prev && navigate(`/teach/submissions/${prev.submissionId}`)}
        >
          Prev
        </button>
        <button
          className="btn btn-ghost btn-sm"
          disabled={!next}
          onClick={() => next && navigate(`/teach/submissions/${next.submissionId}`)}
        >
          Next
        </button>
        <button className="btn btn-sec btn-sm" onClick={() => load(false)}>
          Refresh
        </button>
      </div>

      {error && (
        <div className="notice red" style={{ marginBottom: '16px' }}>
          <Terminal />
          <span>{error}</span>
        </div>
      )}
      {active && (
        <div className="notice violet" style={{ marginBottom: '16px' }}>
          <Clock size={15} />
          <span>Grading in progress — this page refreshes every 2 seconds.</span>
        </div>
      )}

      <div className="grid2">
        <div className="card">
          <div className="cardhead">
            <h2 className="cardtitle">Source code</h2>
            <span className="spacer" />
            <span className="mono dim small">
              {sourceLines.length} lines · {detail.sourceCode.length} chars
            </span>
          </div>
          <div className="codebox" style={{ border: 'none', borderRadius: 0 }}>
            <div className="gutter">
              {sourceLines.map((_, i) => (
                <span key={i}>
                  {i + 1}
                  <br />
                </span>
              ))}
            </div>
            <pre>{detail.sourceCode}</pre>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          <div className="card pad">
            <div className="cardhead" style={{ padding: 0, border: 'none', marginBottom: '10px' }}>
              <h2 className="cardtitle">Grade</h2>
              <span className="spacer" />
              <span className="mono dim small">automatic</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: '10px' }}>
              <span className="dispnum" style={{ fontSize: '34px' }}>
                <GradeCell grade={detail.grade} />
              </span>
              <span className="dim small">/ 10</span>
            </div>
            <div className="judgebar">
              <div
                className="fill"
                style={{
                  width: `${
                    detail.totalTests === 0
                      ? 0
                      : Math.round((detail.passedTests / detail.totalTests) * 100)
                  }%`,
                }}
              />
            </div>
            <p className="dim small" style={{ marginTop: '8px' }}>
              {detail.passedTests} of {detail.totalTests} tests passed
              {tests.length > (detail.sampleTestResults?.length ?? 0) ? ' · includes hidden tests' : ''}
            </p>
            <p className="dim small" style={{ marginTop: '6px' }}>
              Grading is automatic. Manual grade override is not wired to the API yet.
            </p>
          </div>


          <div className="card">
            <div className="cardhead">
              <h2 className="cardtitle">Test results</h2>
              <span className="spacer" />
              <span className="mono dim small">
                {tests.length - failed.length}/{tests.length}
              </span>
            </div>
            {tests.length === 0 ? (
              <Empty icon={<Lamp size={22} />}>
                {active ? 'Waiting for the judge to return results.' : 'No test results recorded yet.'}
              </Empty>
            ) : (
              <table className="tbl">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Test</th>
                    <th>Verdict</th>
                    <th className="num">Time</th>
                    <th className="num">Memory</th>
                  </tr>
                </thead>
                <tbody>
                  {tests.map((t) => (
                    <tr
                      key={t.testId}
                      className="clickable"
                      onClick={() =>
                        setExpanded((current) =>
                          current.includes(t.testId)
                            ? current.filter((id) => id !== t.testId)
                            : [...current, t.testId],
                        )
                      }
                    >
                      <td className="mono dim">{t.displayOrder}</td>
                      <td>
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                          <ChevronRight />
                          {t.testName || `Test ${t.displayOrder}`}
                        </span>
                      </td>
                      <td>
                        <TestPill status={t.status} />
                      </td>
                      <td className="num mono dim small">
                        {t.executionTimeMs === null || t.executionTimeMs === undefined
                          ? '—'
                          : `${t.executionTimeMs} ms`}
                      </td>
                      <td className="num mono dim small">
                        {t.memoryUsedKib === null || t.memoryUsedKib === undefined
                          ? '—'
                          : `${Math.round(t.memoryUsedKib / 1024)} MB`}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>

      {expanded.length > 0 && (
        <div className="card pad" style={{ marginTop: '18px' }}>
          <h2 className="cardtitle" style={{ marginBottom: '12px' }}>
            Test details
          </h2>
          {tests
            .filter((t) => expanded.includes(t.testId))
            .map((t) => (
              <div key={t.testId} style={{ marginBottom: '16px' }}>
                <div className="toolbar" style={{ marginBottom: '8px' }}>
                  <b>{t.testName || `Test ${t.displayOrder}`}</b>
                  <TestPill status={t.status} />
                </div>
                <div className="submitgrid">
                  <div>
                    <div className="iolabel">Input</div>
                    <div className="io">{t.input || '(no input)'}</div>
                  </div>
                  <div>
                    <div className="iolabel">Expected</div>
                    <div className="io">{t.expectedOutput}</div>
                  </div>
                  <div>
                    <div className="iolabel">Actual</div>
                    <div className="io">{t.actualOutput ?? '—'}</div>
                  </div>
                </div>
              </div>
            ))}
        </div>
      )}
    </section>
  );
}

