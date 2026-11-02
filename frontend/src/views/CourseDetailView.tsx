import { useEffect, useMemo, useState } from 'react';
import {
  ArrowRight,
  AsgPill,
  Avatar,
  Countdown,
  displayName,
  Empty,
  fmtShort,
  GradeCell,
  Lamp,
  Search,
  Trophy,
} from '../components/SharedUI';
import { useAuth } from '../state/auth';
import { useSetCrumb } from '../state/crumb';
import { data } from '../data';
import { navigate, useHashQuery } from '../router';
import type { AssignmentSummary, Course, LeaderboardRow, SubmissionSummary } from '../types';

type CourseTab = 'overview' | 'assignments' | 'leaderboard';

const TREND: Record<string, { cls: string; text: string }> = {
  up: { cls: 'up', text: '▲1' },
  down: { cls: 'down', text: '▼1' },
  same: { cls: 'same', text: '·' },
};

export function CourseDetailView({ courseId }: { courseId: number }) {
  const { session } = useAuth();
  const query = useHashQuery();
  const [course, setCourse] = useState<Course | null>(null);
  const [assignments, setAssignments] = useState<AssignmentSummary[]>([]);
  const [board, setBoard] = useState<LeaderboardRow[]>([]);
  const [submissions, setSubmissions] = useState<Record<number, SubmissionSummary[]>>({});
  const [tab, setTab] = useState<CourseTab>(() => {
    const t = query.get('tab');
    return t === 'assignments' || t === 'leaderboard' ? t : 'overview';
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [boardQuery, setBoardQuery] = useState('');

  useSetCrumb(['Courses'], course?.code ?? `Course #${courseId}`);

  const queryTab = query.get('tab');
  useEffect(() => {
    if (queryTab === 'overview' || queryTab === 'assignments' || queryTab === 'leaderboard') {
      setTab(queryTab);
    }
  }, [queryTab]);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    (async () => {
      try {
        const [c, list, lb] = await Promise.all([
          data.course(courseId),
          data.assignments(courseId),
          data.leaderboard(),
        ]);
        const pairs = await Promise.all(
          list.map(async (a) => [a.id, await data.submissions(a.id).catch(() => [])] as const),
        );
        if (!alive) return;
        setCourse(c);
        setAssignments(list);
        setBoard(lb);
        setSubmissions(Object.fromEntries(pairs));
        setError('');
      } catch (err: any) {
        if (!alive) return;
        setError(err?.message || 'Could not load this course.');
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [courseId]);

  const now = Date.now();
  const openAssignments = useMemo(
    () =>
      assignments
        .filter((a) => a.assignmentStatus === 'PUBLISHED' && (!a.closingTime || new Date(a.closingTime).getTime() > now))
        .sort((a, b) => new Date(a.closingTime ?? 0).getTime() - new Date(b.closingTime ?? 0).getTime()),
    [assignments, now],
  );
  const next = openAssignments[0];
  const name = displayName(session?.username ?? 'student');

  const boardRows = useMemo(() => {
    if (board.length > 0) return board;
    const attempts = Object.values(submissions).flat();
    const graded = attempts.filter((s) => typeof s.grade === 'number');
    if (graded.length === 0) return [];
    const avg = graded.reduce((sum, s) => sum + (s.grade as number), 0) / graded.length;
    return [
      {
        rank: 1,
        username: session?.username ?? 'student',
        name,
        initials: '',
        score: Math.round(avg * 100) / 10,
        attempts: attempts.length,
        you: true,
      } as LeaderboardRow,
    ];
  }, [board, submissions, session, name]);

  const filteredBoard = boardRows.filter((row) => {
    const q = boardQuery.trim().toLowerCase();
    if (!q) return true;
    return `${row.name} ${row.username}`.toLowerCase().includes(q);
  });

  if (loading) {
    return (
      <section className="screen on" id="scr-course">
        <div style={{ padding: '60px', textAlign: 'center' }} className="mut">
          <span className="spin" /> Opening course…
        </div>
      </section>
    );
  }

  if (error || !course) {
    return (
      <section className="screen on" id="scr-course">
        <div className="card pad">
          <p>{error || 'Course not found.'}</p>
          <button className="btn btn-sec btn-sm" style={{ marginTop: '12px' }} onClick={() => navigate('/courses')}>
            Back to my courses
          </button>
        </div>
      </section>
    );
  }

  const tabButton = (key: CourseTab, label: string, count?: number) => (
    <button
      className={tab === key ? 'tab on' : 'tab'}
      onClick={() => {
        setTab(key);
        navigate(`/courses/${courseId}?tab=${key}`);
      }}
    >
      {label}
      {count !== undefined && <span className="cnt">{count}</span>}
    </button>
  );

  return (
    <section className="screen on" id="scr-course">
      <span className="kicker">
        {course.code}
        {course.term ? ` · ${course.term}` : ''}
      </span>
      <h1 className="pagetitle" style={{ margin: '6px 0 4px' }}>
        {course.courseName}
      </h1>
      <p className="mut" style={{ marginBottom: '14px' }}>
        {course.description || 'No course description yet.'}
        {course.lecturer ? (
          <>
            {' · Taught by '}
            <b style={{ color: 'var(--ink)' }}>{course.lecturer}</b>
          </>
        ) : null}
      </p>

      <div className="tabs">
        {tabButton('overview', 'Overview')}
        {tabButton('assignments', 'Assignments', assignments.length)}
        {tabButton('leaderboard', 'Leaderboard', boardRows.length)}
      </div>

      {tab === 'overview' && (
        <div className="grid2">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {next && (
              <div className="notice amber">
                <Lamp />
                <div>
                  <b>
                    {next.code}: {next.title}
                  </b>{' '}
                  closes <Countdown until={next.closingTime} />.{' '}
                  {next.submissionLimit ? `${next.submissionLimit} attempts are allowed; the best graded attempt counts.` : ''}
                </div>
              </div>
            )}

            <div className="card pad">
              <h2 className="cardtitle" style={{ marginBottom: '8px' }}>
                About this course
              </h2>
              <p className="mut">
                {course.description || 'No description provided for this course.'} Weekly labs are auto-graded against
                public and hidden test cases. Grades release after the closing date; the course leaderboard updates
                live.
              </p>
              <div style={{ display: 'flex', gap: '20px', marginTop: '14px' }} className="mono small dim">
                <span>{assignments.length} ASSIGNMENTS</span>
                {typeof course.students === 'number' && <span>{course.students} STUDENTS</span>}
                <span>{(assignments[0]?.allowedLanguages?.[0] ?? 'JAVA').toUpperCase()}</span>
              </div>
            </div>

            <div className="card">
              <div className="cardhead">
                <h2 className="cardtitle">Assignments at a glance</h2>
              </div>
              <table className="tbl">
                <tbody>
                  {assignments.length === 0 ? (
                    <tr>
                      <td className="dim" style={{ padding: '18px' }}>
                        No assignments published yet.
                      </td>
                    </tr>
                  ) : (
                    assignments.map((a) => {
                      const isOpen = a.assignmentStatus === 'PUBLISHED';
                      return (
                        <tr
                          key={a.id}
                          className={isOpen ? 'clickable' : undefined}
                          onClick={isOpen ? () => navigate(`/assignments/${a.id}`) : undefined}
                        >
                          <td className="mono dim">{a.code}</td>
                          <td>{a.title}</td>
                          <td>
                            <AsgPill status={a.assignmentStatus} />
                          </td>
                          <td className="num mut small">{fmtShort(a.closingTime)}</td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div className="card pad">
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' }}>
              <span className="livedot" />
              <h2 className="cardtitle">Leaderboard</h2>
            </div>
            {boardRows.slice(0, 5).map((row) => (
              <div key={`${row.username}-${row.rank}`} className={row.you ? 'lb-row you' : 'lb-row'}>
                <span className="lb-rank">{row.rank}</span>
                <Avatar name={row.name} size={26} radius={7} />
                <span style={{ fontSize: '13.5px' }}>
                  {row.name} {row.you && <span className="dim small">(you)</span>}
                </span>
                <span className="lb-score">{row.score.toFixed(1)}</span>
              </div>
            ))}
            {boardRows.length === 0 && <p className="dim small">Rankings appear once grading runs complete.</p>}
            <button
              className="btn btn-sec btn-sm"
              style={{ width: '100%', marginTop: '12px' }}
              onClick={() => navigate(`/courses/${courseId}?tab=leaderboard`)}
            >
              <Trophy /> View full leaderboard
            </button>
          </div>
        </div>
      )}

      {tab === 'assignments' && (
        <div className="card">
          <div className="cardhead">
            <h2 className="cardtitle">Assignments</h2>
            <span className="spacer" />
            <span className="dim small">{assignments.length} total</span>
          </div>
          <table className="tbl hoverable">
            <thead>
              <tr>
                <th>Assignment</th>
                <th>Status</th>
                <th className="num">Att.</th>
                <th className="num">Grade</th>
                <th>Closes</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {assignments.length === 0 ? (
                <tr>
                  <td colSpan={6} className="dim" style={{ textAlign: 'center', padding: '24px' }}>
                    No assignments published yet.
                  </td>
                </tr>
              ) : (
                assignments.map((a) => {
                  const attempts = submissions[a.id] ?? [];
                  const best = attempts.reduce<number | null>(
                    (acc, s) => (typeof s.grade === 'number' && (acc === null || s.grade > acc) ? s.grade : acc),
                    null,
                  );
                  const isOpen = a.assignmentStatus === 'PUBLISHED';
                  return (
                    <tr
                      key={a.id}
                      className={isOpen ? 'clickable' : undefined}
                      onClick={isOpen ? () => navigate(`/assignments/${a.id}`) : undefined}
                    >
                      <td>
                        <b>
                          {a.code}: {a.title}
                        </b>
                      </td>
                      <td>
                        <AsgPill status={a.assignmentStatus} />
                      </td>
                      <td className="num mono dim">
                        {attempts.length}/{a.submissionLimit ?? 3}
                      </td>
                      <td className="num">
                        <GradeCell grade={best} />
                      </td>
                      <td className="mut small">
                        {isOpen ? <Countdown until={a.closingTime} /> : fmtShort(a.closingTime)}
                      </td>
                      <td className="num">
                        {isOpen ? (
                          <button
                            className="btn btn-primary btn-sm"
                            onClick={(e) => {
                              e.stopPropagation();
                              navigate(`/assignments/${a.id}`);
                            }}
                          >
                            Open
                          </button>
                        ) : null}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'leaderboard' && (
        <>
          <div className="toolbar">
            <div style={{ display: 'flex', alignItems: 'center', gap: '9px' }}>
              <span className="livedot" />
              <b style={{ fontFamily: 'var(--disp)', fontSize: '15px' }}>Live leaderboard</b>
              <span className="dim small">updates as submissions are judged</span>
            </div>
            <span className="spacer" />
            <div className="searchbox" style={{ width: '230px' }}>
              <Search />
              <input
                id="lb-q"
                placeholder="Search student…"
                value={boardQuery}
                onChange={(e) => setBoardQuery(e.target.value)}
              />
            </div>
          </div>

          {boardRows.length >= 3 && (
            <div className="podiumrow">
              {[boardRows[1], boardRows[0], boardRows[2]].map((row) => (
                <div key={row.username} className={`podium p${row.rank} ${row.you ? 'you' : ''}`}>
                  <div className="prank">{row.rank}</div>
                  <Avatar name={row.name} size={52} radius={14} />
                  <div className="pname">{row.name}</div>
                  <div className="puser">{row.username}</div>
                  <div className="pscore">{row.score.toFixed(1)}</div>
                  <div className="pmeta">
                    <span>
                      <span className={`trend ${TREND[row.trend ?? 'same'].cls}`}>{TREND[row.trend ?? 'same'].text}</span>
                    </span>
                    <span>
                      {row.testsPassed ?? '—'}/{row.testsTotal ?? '—'} tests
                    </span>
                    <span>{row.lastActive ?? '—'}</span>
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="card">
            <table className="tbl hoverable">
              <thead>
                <tr>
                  <th style={{ width: '52px' }}>Rank</th>
                  <th style={{ width: '52px' }}>Δ</th>
                  <th>Student</th>
                  <th className="num">Tests passed</th>
                  <th className="num">Attempts</th>
                  <th>Last active</th>
                  <th className="num">Best score</th>
                </tr>
              </thead>
              <tbody id="lb-rows">
                {filteredBoard.length === 0 ? (
                  <tr>
                    <td colSpan={7}>
                      <Empty icon={<Search size={15} />}>No students match your search.</Empty>
                    </td>
                  </tr>
                ) : (
                  filteredBoard.map((row) => (
                    <tr key={`${row.rank}-${row.username}`} className={row.you ? 'lb-you lb-top1' : undefined}>
                      <td>
                        <span
                          className="dispnum"
                          style={{
                            fontSize: '16px',
                            color: row.you ? 'var(--accent-text)' : row.rank <= 3 ? 'var(--ink)' : 'var(--dim)',
                          }}
                        >
                          {row.rank}
                        </span>
                      </td>
                      <td>
                        <span className={`trend ${TREND[row.trend ?? 'same'].cls}`}>
                          {TREND[row.trend ?? 'same'].text}
                        </span>
                      </td>
                      <td>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          <Avatar name={row.name} size={28} radius={8} />
                          <span>
                            <b>{row.name}</b>
                            {row.you && <span className="dim small"> (you)</span>}
                            <div className="mono dim small">{row.username}</div>
                          </span>
                        </span>
                      </td>
                      <td className="num mono">
                        {row.testsPassed ?? '—'}/{row.testsTotal ?? '—'}
                      </td>
                      <td className="num mono dim">{row.attempts ?? '—'}</td>
                      <td className="mut small">{row.lastActive ?? '—'}</td>
                      <td className="num">
                        <span className="lb-scorecell">{row.score.toFixed(1)}</span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          <p className="dim small" style={{ marginTop: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <ArrowRight size={15} /> Best score per student, across closed and published assignments. Ties break by
            earlier achievement.
            {!data.demo && ' Cohort-wide rankings are not exposed by the current API, so only your own row is shown.'}
          </p>
        </>
      )}



    </section>
  );
}
