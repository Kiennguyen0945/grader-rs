import { useEffect, useState } from 'react';
import {
  ArrowRight,
  AsgPill,
  Avatar,
  Book,
  Clock,
  Countdown,
  displayName,
  fmtAgo,
  fmtShort,
  GradeCell,
  Lamp,
  Send,
  SubPill,
  Terminal,
  Trophy,
} from '../components/SharedUI';
import { useAuth } from '../state/auth';
import { useSetCrumb } from '../state/crumb';
import { flattenSubmissions, ownLeaderboardRow, useOverview } from '../hooks/useOverview';
import { data } from '../data';
import { navigate } from '../router';
import type { LeaderboardRow } from '../types';

function greeting(now = new Date()) {
  const h = now.getHours();
  if (h < 12) return 'Good morning';
  if (h < 18) return 'Good afternoon';
  return 'Good evening';
}

export function DashboardView() {
  const { session } = useAuth();
  const { loading, error, courses, assignments, submissionsByAssignment, reload } = useOverview();
  const [leaderboard, setLeaderboard] = useState<LeaderboardRow[]>([]);
  useSetCrumb(['Home'], 'Dashboard');

  useEffect(() => {
    data.leaderboard().then(setLeaderboard).catch(() => {});
  }, []);

  const username = session?.username ?? 'student';
  const name = displayName(username);
  const firstName = name.split(' ')[0] || 'student';

  const submissions = flattenSubmissions({ loading, error, courses, assignments, submissionsByAssignment });
  const now = Date.now();
  const published = assignments.filter((a) => a.assignmentStatus === 'PUBLISHED');
  const openNow = published
    .filter((a) => !a.closingTime || new Date(a.closingTime).getTime() > now)
    .sort((a, b) => new Date(a.closingTime ?? 0).getTime() - new Date(b.closingTime ?? 0).getTime());
  const closed = assignments
    .filter((a) => a.assignmentStatus === 'CLOSED')
    .sort((a, b) => new Date(b.closingTime ?? 0).getTime() - new Date(a.closingTime ?? 0).getTime());

  const next = openNow[0];
  const nextAttemptsUsed = next ? (submissionsByAssignment[next.id]?.length ?? 0) : 0;
  const nextAttemptsLeft = next ? Math.max(0, (next.submissionLimit ?? 3) - nextAttemptsUsed) : 0;

  const graded = submissions.filter((s) => typeof s.grade === 'number');
  const average = graded.length
    ? graded.reduce((sum, s) => sum + (s.grade as number), 0) / graded.length
    : null;

  const recent = [...submissions].sort((a, b) => {
    const at = a.submittedAt ? new Date(a.submittedAt).getTime() : 0;
    const bt = b.submittedAt ? new Date(b.submittedAt).getTime() : 0;
    if (at !== bt) return bt - at;
    return b.submissionId - a.submissionId;
  });

  const dueRows = [...openNow, ...closed].slice(0, 4);
  const board = data.demo
    ? leaderboard.slice(0, 5)
    : submissions.length > 0
      ? [ownLeaderboardRow(name, username, submissions)]
      : [];
  const boardCourse = courses[0]?.code ?? '';

  const dateLabel = new Date().toLocaleDateString('en-GB', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  });

  if (loading) {
    return (
      <section className="screen on" id="scr-dashboard">
        <div style={{ padding: '60px', textAlign: 'center' }} className="mut">
          <span className="spin" /> Loading dashboard…
        </div>
      </section>
    );
  }

  return (
    <section className="screen on" id="scr-dashboard">
      <span className="kicker">{dateLabel} · student view</span>
      <h1 className="pagetitle" style={{ margin: '6px 0 2px' }}>
        {greeting()}, {firstName}.
      </h1>
      <p className="mut">
        {next ? (
          <>
            {next.code} closes in <Countdown until={next.closingTime} />
            {nextAttemptsLeft > 0 ? ` — you have ${nextAttemptsLeft} attempts left.` : ' — no attempts left.'}
          </>
        ) : (
          'No open assignments right now — enjoy the quiet.'
        )}
      </p>

      {error && (
        <div className="banner b-err" style={{ marginTop: '14px' }}>
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

      <div className="stats">
        <div className="stat">
          <span className="si">
            <Book />
          </span>
          <div className="sv">{courses.length}</div>
          <div className="sl">Enrolled courses</div>
        </div>
        <div className="stat hot">
          <span className="si">
            <Clock />
          </span>
          <div className="sv">{openNow.length}</div>
          <div className="sl">Due soon</div>
        </div>
        <div className="stat">
          <span className="si">
            <Send />
          </span>
          <div className="sv">{submissions.length}</div>
          <div className="sl">Submissions this term</div>
        </div>
        <div className="stat">
          <span className="si">
            <Lamp />
          </span>
          <div className="sv">{average === null ? '—' : average.toFixed(1)}</div>
          <div className="sl">Average grade</div>
        </div>
      </div>

      <div className="grid2">
        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          <div className="card">
            <div className="cardhead">
              <h2 className="cardtitle">Due soon</h2>
              <span className="spacer" />
              <button className="btn btn-ghost btn-sm" onClick={() => navigate('/courses')}>
                All courses <ArrowRight />
              </button>
            </div>
            <table className="tbl hoverable">
              <thead>
                <tr>
                  <th>Assignment</th>
                  <th>Course</th>
                  <th>Closes</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {dueRows.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="dim" style={{ textAlign: 'center', padding: '22px' }}>
                      No assignments yet.
                    </td>
                  </tr>
                ) : (
                  dueRows.map((a, i) => {
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
                        <td className="mono dim">{a.courseCode}</td>
                        <td>
                          {isOpen ? (
                            <Countdown until={a.closingTime} />
                          ) : (
                            <span className="mut small">{fmtShort(a.closingTime)}</span>
                          )}
                        </td>
                        <td>
                          <AsgPill status={a.assignmentStatus} />
                        </td>
                        <td className="num">
                          {isOpen ? (
                            <button
                              className={i === 0 ? 'btn btn-primary btn-sm' : 'btn btn-sec btn-sm'}
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

          <div className="card">
            <div className="cardhead">
              <h2 className="cardtitle">Recent submissions</h2>
              <span className="spacer" />
              <button className="btn btn-ghost btn-sm" onClick={() => navigate('/history')}>
                History <ArrowRight />
              </button>
            </div>
            <table className="tbl hoverable">
              <thead>
                <tr>
                  <th>Assignment</th>
                  <th className="num">Att.</th>
                  <th>Status</th>
                  <th className="num">Grade</th>
                  <th>When</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {recent.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="dim" style={{ textAlign: 'center', padding: '22px' }}>
                      You have not submitted anything yet.
                    </td>
                  </tr>
                ) : (
                  recent.slice(0, 5).map((s) => (
                    <tr
                      key={s.submissionId}
                      className="clickable"
                      onClick={() => navigate(`/assignments/${s.assignmentId}`)}
                    >
                      <td>
                        {s.assignment ? `${s.assignment.code}: ${s.assignment.title}` : `Assignment #${s.assignmentId}`}
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
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/assignments/${s.assignmentId}`);
                          }}
                        >
                          View
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          <div className="card pad">
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' }}>
              <span className="livedot" />
              <h2 className="cardtitle">Leaderboard</h2>
              <span className="spacer" />
              <span className="mono dim small">{boardCourse}</span>
            </div>
            {board.length === 0 ? (
              <p className="dim small">Rankings appear once grading runs complete.</p>
            ) : (
              board.map((row) => (
                <div key={row.username} className={row.you ? 'lb-row you' : 'lb-row'}>
                  <span className="lb-rank">{row.rank}</span>
                  <Avatar name={row.name} size={26} radius={7} />
                  <span style={{ fontSize: '13.5px' }}>
                    {row.name} {row.you && <span className="dim small">(you)</span>}
                  </span>
                  <span className="lb-score">{row.score.toFixed(1)}</span>
                </div>
              ))
            )}
            <button
              className="btn btn-sec btn-sm"
              style={{ width: '100%', marginTop: '12px' }}
              onClick={() => navigate(courses[0] ? `/courses/${courses[0].id}?tab=leaderboard` : '/courses')}
            >
              <Trophy /> View full leaderboard
            </button>
          </div>

          <div className="card pad">
            <h2 className="cardtitle" style={{ marginBottom: '12px' }}>
              My courses
            </h2>
            {courses.length === 0 ? (
              <p className="dim small">You are not enrolled in any course yet.</p>
            ) : (
              courses.map((c) => (
                <button key={c.id} className="jumpitem" onClick={() => navigate(`/courses/${c.id}`)}>
                  <span className="jn" style={{ borderColor: 'var(--line)', color: 'var(--mut)' }}>
                    <Book />
                  </span>
                  <span className="jt">
                    {c.code} · {c.courseName}
                  </span>
                  <span className="jr dim">
                    <ArrowRight />
                  </span>
                </button>
              ))
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
