import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  AsgPill,
  bannerFor,
  Check,
  ChevronDown,
  ChevronRight,
  Clock,
  Countdown,
  Empty,
  Eye,
  Focus,
  fmtAgo,
  fmtLong,
  isTerminal,
  Reset as ResetIcon,
  Send,
  SubPill,
  subStatusLabel,
  Terminal,
} from '../components/SharedUI';
import { useToast } from '../components/Toasts';
import { useSetCrumb } from '../state/crumb';
import { data } from '../data';
import type { AssignmentDetail, SampleTestCase, SubmissionDetail, SubmissionSummary } from '../types';

const DEFAULT_TEMPLATE = `import java.io.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String line;
    while ((line = br.readLine()) != null) {
      System.out.println(line);
    }
  }
}
`;

const draftKey = (assignmentId: number) => `lamplight-draft-${assignmentId}`;

export function WorkspaceView({ assignmentId }: { assignmentId: number }) {
  const { push } = useToast();
  const [assignment, setAssignment] = useState<AssignmentDetail | null>(null);
  const [courseCode, setCourseCode] = useState('');
  const [submissions, setSubmissions] = useState<SubmissionSummary[]>([]);
  const [detail, setDetail] = useState<SubmissionDetail | null>(null);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [code, setCode] = useState(DEFAULT_TEMPLATE);
  const [tab, setTab] = useState<'desc' | 'tests' | 'subs'>('desc');
  const [dockOpen, setDockOpen] = useState(false);
  const [dockTab, setDockTab] = useState<'result' | 'code'>('result');
  const [openTest, setOpenTest] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);
  const [focusMode, setFocusMode] = useState(false);
  const [leftWidth, setLeftWidth] = useState(46);
  const [wsView, setWsView] = useState<'problem' | 'editor' | 'result'>('editor');
  const [saveLabel, setSaveLabel] = useState('template loaded');

  const pollRef = useRef<number | null>(null);
  const aliveRef = useRef(true);
  const splitRef = useRef<HTMLDivElement | null>(null);
  const areaRef = useRef<HTMLTextAreaElement | null>(null);

  useSetCrumb([courseCode || 'Workspace'], assignment ? `${assignment.code} · ${assignment.title}` : 'Workspace');

  const stopPolling = () => {
    if (pollRef.current !== null) {
      window.clearInterval(pollRef.current);
      pollRef.current = null;
    }
  };

  const pollOnce = async (submissionId: number) => {
    try {
      const next = await data.submission(submissionId);
      if (!aliveRef.current) return;
      setDetail(next);
      setSubmissions((prev) =>
        prev.map((s) => (s.submissionId === submissionId ? { ...s, status: next.status, grade: next.grade } : s)),
      );
      if (isTerminal(next.status)) stopPolling();
    } catch {
      stopPolling();
    }
  };

  const startPolling = (submissionId: number) => {
    stopPolling();
    pollOnce(submissionId);
    pollRef.current = window.setInterval(() => pollOnce(submissionId), 1400);
  };

  useEffect(() => {
    aliveRef.current = true;
    setLoading(true);
    (async () => {
      try {
        const loaded = await data.assignment(assignmentId);
        const subs = await data.submissions(assignmentId);
        if (!aliveRef.current) return;
        setAssignment(loaded);
        setSubmissions(subs);
        data
          .course(loaded.courseId)
          .then((c) => aliveRef.current && setCourseCode(c.code))
          .catch(() => {});

        let draft: string | null = null;
        try {
          draft = localStorage.getItem(draftKey(assignmentId));
        } catch {}
        const latest = subs.length > 0 ? subs[subs.length - 1] : null;
        setCode(draft ?? latest?.sourceCode ?? DEFAULT_TEMPLATE);
        if (latest) {
          setActiveId(latest.submissionId);
          setDockOpen(true);
          setDockTab('result');
          startPolling(latest.submissionId);
        }
      } catch (err: any) {
        push(err?.message || 'Could not open this assignment.', 'err');
      } finally {
        if (aliveRef.current) setLoading(false);
      }
    })();
    return () => {
      aliveRef.current = false;
      stopPolling();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [assignmentId]);

  useEffect(() => {
    const id = window.setTimeout(() => {
      try {
        localStorage.setItem(draftKey(assignmentId), code);
        setSaveLabel('draft saved');
      } catch {}
    }, 700);
    return () => window.clearTimeout(id);
  }, [code, assignmentId]);

  const limit = assignment?.submissionLimit ?? 3;
  const attemptsLeft = Math.max(0, limit - submissions.length);
  const lang = (assignment?.allowedLanguages?.[0] ?? 'JAVA').toString();
  const lines = code.split('\n').length;
  const activeSummary = submissions.find((s) => s.submissionId === activeId) ?? null;
  const activeStatus = detail?.status ?? activeSummary?.status ?? null;
  const results = detail?.sampleTestResults ?? [];
  const sampleTests = useMemo<SampleTestCase[]>(() => assignment?.sampleTestCases ?? [], [assignment]);

  async function handleSubmit() {
    if (!assignment || submitting) return;
    setSubmitting(true);
    try {
      const created = await data.createSubmission(assignment.id, code);
      setSubmissions((prev) => [...prev, created]);
      setActiveId(created.submissionId);
      setDetail({ ...created, passedTests: 0, totalTests: 0, sampleTestResults: [] });
      setDockOpen(true);
      setDockTab('result');
      setWsView('result');
      startPolling(created.submissionId);
      push(`Attempt #${created.attemptNumber} submitted — judging started.`, 'ok');
    } catch (err: any) {
      push(err?.message || 'Submission failed.', 'err');
    } finally {
      setSubmitting(false);
    }
  }

  function handleReset() {
    setCode(DEFAULT_TEMPLATE);
    setSaveLabel('template loaded');
  }

  function handleCopy() {
    navigator.clipboard?.writeText(code).then(
      () => push('Code copied to clipboard.', 'ok'),
      () => push('Could not copy code.', 'err'),
    );
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      handleSubmit();
      return;
    }
    if (e.key === 'Tab') {
      e.preventDefault();
      const el = e.currentTarget;
      const start = el.selectionStart;
      const end = el.selectionEnd;
      setCode(`${code.slice(0, start)}  ${code.slice(end)}`);
      requestAnimationFrame(() => {
        el.selectionStart = el.selectionEnd = start + 2;
      });
    }
  }

  function startDrag(e: React.MouseEvent) {
    e.preventDefault();
    const container = splitRef.current;
    if (!container) return;
    const rect = container.getBoundingClientRect();
    const move = (ev: MouseEvent) => {
      const raw = ((ev.clientX - rect.left) / rect.width) * 100;
      setLeftWidth(Math.min(70, Math.max(28, raw)));
    };
    const up = () => {
      document.removeEventListener('mousemove', move);
      document.removeEventListener('mouseup', up);
    };
    document.addEventListener('mousemove', move);
    document.addEventListener('mouseup', up);
  }

  function openSubmission(s: SubmissionSummary) {
    setActiveId(s.submissionId);
    setDockOpen(true);
    setDockTab('result');
    setWsView('result');
    startPolling(s.submissionId);
  }

  if (loading) {
    return (
      <section className="screen on" id="scr-workspace">
        <div style={{ padding: '60px', textAlign: 'center' }} className="mut">
          <span className="spin" /> Opening workspace…
        </div>
      </section>
    );
  }

  if (!assignment) {
    return (
      <section className="screen on" id="scr-workspace">
        <div className="card pad">
          <p>Assignment not found, or it is not published yet.</p>
        </div>
      </section>
    );
  }

  const banner = activeStatus ? bannerFor(activeStatus, assignment) : null;

  return (
    <section className="screen on" id="scr-workspace" data-wsview={wsView}>
      <div className="ws-head">
        <span className="ws-title">
          {assignment.code} · {assignment.title}
        </span>
        <span className="ws-chips" id="ws-chips">
          <AsgPill status={assignment.assignmentStatus} />
          <span className="langchip">{lang.charAt(0) + lang.slice(1).toLowerCase()}</span>
          <span className="pill st-queued">
            <span className="dot" />
            {submissions.length}/{limit} attempts
          </span>
          <span className="pill st-err">
            <Countdown until={assignment.closingTime} />
          </span>
        </span>
        <span className="ws-actions">
          <div className="ws-seg">
            {(['problem', 'editor', 'result'] as const).map((v) => (
              <button key={v} className={wsView === v ? 'on' : undefined} onClick={() => setWsView(v)}>
                {v === 'problem' ? 'Problem' : v === 'editor' ? 'Editor' : 'Result'}
              </button>
            ))}
          </div>
          <button className="iconbtn" title="Focus mode (hide problem panel)" onClick={() => setFocusMode((v) => !v)}>
            <Focus />
          </button>
          <button className="btn btn-sec btn-sm" title="Reset editor to template" onClick={handleReset}>
            <ResetIcon /> Reset
          </button>
          <button
            className="btn btn-primary"
            id="ws-submit"
            title="Submit your solution (⌘/Ctrl+Enter)"
            onClick={handleSubmit}
            disabled={submitting || attemptsLeft === 0 || assignment.assignmentStatus !== 'PUBLISHED'}
          >
            {submitting ? <span className="spin" /> : <Send />}
            {submitting ? 'Submitting…' : 'Submit'}
            <span className="mono" style={{ opacity: 0.7 }}>
              ⌘↵
            </span>
          </button>
        </span>
      </div>

      <div className="ws-split" id="ws-split" ref={splitRef}>
        <div className="ws-left" id="ws-left" style={{ width: `${leftWidth}%`, display: focusMode ? 'none' : undefined }}>
          <div className="ws-lefttabs" id="ws-lefttabs">
            <button className={tab === 'desc' ? 'tab on' : 'tab'} onClick={() => setTab('desc')}>
              Description
            </button>
            <button className={tab === 'tests' ? 'tab on' : 'tab'} onClick={() => setTab('tests')}>
              Tests<span className="cnt">{sampleTests.length}</span>
            </button>
            <button className={tab === 'subs' ? 'tab on' : 'tab'} onClick={() => setTab('subs')}>
              Submissions<span className="cnt">{submissions.length}</span>
            </button>
          </div>

          <div className="ws-leftbody" id="ws-leftbody">
            {tab === 'desc' && (
              <>
                {(assignment.description || 'No description provided.')
                  .split(/\n{2,}/)
                  .filter(Boolean)
                  .map((para, i) => (
                    <p key={i} className="mut" style={{ marginBottom: '10px', whiteSpace: 'pre-line' }}>
                      {para}
                    </p>
                  ))}

                {sampleTests[0] && (
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', margin: '14px 0' }}>
                    <div>
                      <div className="iolabel">Sample input</div>
                      <div className="io">{sampleTests[0].input || '(no input)'}</div>
                    </div>
                    <div>
                      <div className="iolabel">Sample output</div>
                      <div className="io">{sampleTests[0].expectedOutput}</div>
                    </div>
                  </div>
                )}

                <div className="card pad" style={{ boxShadow: 'none' }}>
                  <div className="ws-ruled">
                    <span className="k">Opens</span>
                    <span className="v">{fmtLong(assignment.openingTime)}</span>
                  </div>
                  <div className="ws-ruled">
                    <span className="k">Closes</span>
                    <span className="v">{fmtLong(assignment.closingTime)}</span>
                  </div>
                  <div className="ws-ruled">
                    <span className="k">Attempt limit</span>
                    <span className="v">
                      {limit} · best counts ({attemptsLeft} left)
                    </span>
                  </div>
                  {assignment.timeLimitMs && (
                    <div className="ws-ruled">
                      <span className="k">Limits</span>
                      <span className="v">
                        {(assignment.timeLimitMs / 1000).toFixed(1)}s ·{' '}
                        {Math.round((assignment.memoryLimitKib ?? 0) / 1024)}MB / test
                      </span>
                    </div>
                  )}
                </div>

                <div className="notice violet" style={{ marginTop: '12px' }}>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z" />
                    <circle cx="12" cy="12" r="2.6" />
                  </svg>
                  <div>
                    <b>Visibility.</b> Public tests show exact input/output. Hidden tests are graded silently — name and
                    verdict only.
                  </div>
                </div>
              </>
            )}

            {tab === 'tests' && (
              <div>
                {sampleTests.length === 0 ? (
                  <Empty icon={<Terminal />} padding={26}>
                    Public tests are not available for this assignment.
                  </Empty>
                ) : (
                  sampleTests.map((t, i) => (
                    <div key={t.id} style={{ borderBottom: '1px solid var(--line-soft)' }}>
                      <button className="ws-testrow" onClick={() => setOpenTest(openTest === t.id ? null : t.id)}>
                        <ChevronRight className="expandchev" />
                        <span className="idx">{i + 1}</span>
                        <span className="nm">{t.name || `Sample test ${i + 1}`}</span>
                        <span className="tm">public</span>
                      </button>
                      {openTest === t.id && (
                        <div style={{ padding: '0 8px 14px 40px' }}>
                          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                            <div>
                              <div className="iolabel">Input</div>
                              <div className="io">{t.input || '(no input)'}</div>
                            </div>
                            <div>
                              <div className="iolabel">Expected</div>
                              <div className="io">{t.expectedOutput}</div>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  ))
                )}
              </div>
            )}

            {tab === 'subs' && (
              <div>
                {submissions.length === 0 ? (
                  <Empty icon={<Terminal />} padding={26}>
                    No attempts yet — submit to see verdicts here.
                  </Empty>
                ) : (
                  submissions.map((s) => (
                    <button
                      key={s.submissionId}
                      className="ws-testrow"
                      onClick={() => openSubmission(s)}
                      style={{ borderBottom: '1px solid var(--line-soft)' }}
                    >
                      <span className="idx">#{s.attemptNumber}</span>
                      <span className="nm">
                        <SubPill status={s.status} />
                      </span>
                      <span className="tm">{typeof s.grade === 'number' ? s.grade.toFixed(1) : subStatusLabel(s.status)}</span>
                    </button>
                  ))
                )}
              </div>
            )}

          </div>
        </div>

        {!focusMode && (
          <div
            className="ws-divider"
            id="ws-div"
            title="Drag to resize · double-click to reset"
            onMouseDown={startDrag}
            onDoubleClick={() => setLeftWidth(46)}
          />
        )}

        <div className="ws-right">
          <div className="ws-editor">
            <div className="codetools">
              <span className="fname">Main.{lang.toLowerCase() === 'python' ? 'py' : 'java'}</span>
              <span className="langchip">{lang.charAt(0) + lang.slice(1).toLowerCase()}</span>
              <span className="mono dim small" id="ws-save">
                {saveLabel}
              </span>
              <span className="spacer" />
              <span className="mono dim small" id="ws-count">
                {code.length} chars · {lines} lines
              </span>
              <button className="iconbtn" title="Copy code" onClick={handleCopy}>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                  <rect x="9" y="9" width="12" height="12" rx="2" />
                  <path d="M5 15V5a2 2 0 0 1 2-2h10" />
                </svg>
              </button>
            </div>
            <div className="codebox">
              <div className="gutter" id="ws-gut">
                {Array.from({ length: lines }, (_, i) => (
                  <React.Fragment key={i}>
                    {i + 1}
                    <br />
                  </React.Fragment>
                ))}
              </div>
              <textarea
                id="ws-area"
                ref={areaRef}
                spellCheck={false}
                value={code}
                onChange={(e) => setCode(e.target.value)}
                onKeyDown={handleKeyDown}
              />
            </div>
          </div>

          <div className={dockOpen ? 'ws-dock' : 'ws-dock collapsed'} id="ws-dock">
            <div className="ws-dockhead" onClick={() => setDockOpen((v) => !v)}>
              <span
                id="ws-dockchev"
                className="expandchev"
                style={{ transform: dockOpen ? 'rotate(90deg)' : undefined }}
              >
                <ChevronRight />
              </span>
              <span className="dt">Console</span>
              <span id="ws-docksum" className="dim small">
                {activeStatus ? (
                  <>
                    <SubPill status={activeStatus} />
                    {typeof (detail?.grade ?? activeSummary?.grade) === 'number' && (
                      <span className="mono" style={{ marginLeft: '6px' }}>
                        grade {(detail?.grade ?? activeSummary?.grade)?.toFixed(1)}
                      </span>
                    )}
                  </>
                ) : (
                  'no submission selected — submit to judge here'
                )}
              </span>
              <span className="spacer" />
              <span className="ws-docktabs" id="ws-docktabs" onClick={(e) => e.stopPropagation()}>
                <button className={dockTab === 'result' ? 'on' : undefined} onClick={() => setDockTab('result')}>
                  Result
                </button>
                <button className={dockTab === 'code' ? 'on' : undefined} onClick={() => setDockTab('code')}>
                  Code
                </button>
              </span>
              <button
                className="iconbtn"
                title="Collapse"
                onClick={(e) => {
                  e.stopPropagation();
                  setDockOpen(false);
                }}
              >
                <ChevronDown />
              </button>
            </div>

            <div className="ws-dockbody" id="ws-dockbody">
              {dockTab === 'code' ? (
                <pre className="mono" style={{ margin: 0, whiteSpace: 'pre-wrap', fontSize: '12.6px', lineHeight: 1.65 }}>
                  {detail?.sourceCode ?? activeSummary?.sourceCode ?? code}
                </pre>
              ) : !activeStatus || !banner ? (
                <Empty icon={<Terminal />} padding={26}>
                  Your judging console lives here. Submit and watch tests run live — no page jumps.
                </Empty>
              ) : (
                <>
                  <div className={`banner ${banner.cls}`}>
                    <div className="bic">
                      {banner.icon === 'check' ? <Check /> : banner.icon === 'eye' ? <Eye /> : <Clock />}
                    </div>
                    <div>
                      <h3>{banner.title}</h3>
                      <p>{banner.text}</p>
                    </div>
                    <span className="spacer" style={{ marginLeft: 'auto' }} />
                    <span className="mono small dim">
                      attempt #{activeSummary?.attemptNumber ?? '—'} · {fmtAgo(activeSummary?.submittedAt)}
                    </span>
                  </div>

                  <div className="timeline">
                    {[
                      { label: 'Queued', done: true },
                      { label: 'Running', done: activeStatus !== 'QUEUED' },
                      { label: 'Completed', done: isTerminal(activeStatus) },
                    ].map((step, i, all) => (
                      <React.Fragment key={step.label}>
                        <div className={step.done ? 'tstep done' : 'tstep'}>
                          <span className="tdot">{step.done ? <Check /> : null}</span>
                          <span className="tl">{step.label}</span>
                        </div>
                        {i < all.length - 1 && <div className={all[i + 1].done ? 'tline done' : 'tline'} />}
                      </React.Fragment>
                    ))}
                  </div>

                  <div className="small mut" style={{ margin: '8px 2px' }}>
                    Passed{' '}
                    <b style={{ color: 'var(--ink)' }}>
                      {detail ? `${detail.passedTests}/${detail.totalTests}` : '—'}
                    </b>{' '}
                    · grade{' '}
                    <span className="grademid mono">
                      {typeof (detail?.grade ?? activeSummary?.grade) === 'number'
                        ? (detail?.grade ?? activeSummary?.grade)?.toFixed(1)
                        : '—'}
                    </span>{' '}
                    · hidden tests counted silently
                  </div>

                  {results.length === 0 ? (
                    <p className="dim small" style={{ padding: '8px 2px' }}>
                      {isTerminal(activeStatus)
                        ? 'No public test detail was returned for this attempt.'
                        : 'Waiting for the runner to report test results…'}
                    </p>
                  ) : (
                    results.map((r) => (
                      <div key={`${r.testId}-${r.displayOrder}`} style={{ borderBottom: '1px solid var(--line-soft)' }}>
                        <button
                          className="ws-testrow"
                          onClick={() => setOpenTest(openTest === r.testId ? null : r.testId)}
                        >
                          <ChevronRight className="expandchev" />
                          <span className="idx">{r.displayOrder}</span>
                          <span className="nm">{r.testName || `Test ${r.displayOrder}`}</span>
                          <span className="tm">
                            {r.executionTimeMs ?? '—'} ms · {Math.round((r.memoryUsedKib ?? 0) / 1024)} MB
                          </span>
                          <SubPill status={r.status} />
                        </button>
                        {openTest === r.testId && (
                          <div className="rowdetail" style={{ padding: '0 8px 14px 40px' }}>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                              <div>
                                <div className="iolabel">Input</div>
                                <div className="io">{r.input || '(no input)'}</div>
                              </div>
                              <div>
                                <div className="iolabel">Expected</div>
                                <div className="io">{r.expectedOutput}</div>
                              </div>
                            </div>
                            <div style={{ marginTop: '10px' }}>
                              <div className="iolabel">Your output</div>
                              <div className="io">{r.actualOutput ?? '(none)'}</div>
                            </div>
                          </div>
                        )}
                      </div>
                    ))
                  )}
                </>
              )}

            </div>
          </div>
        </div>

      </div>

    </section>
  );

}

