import { useEffect, useMemo, useState } from 'react';
import {
  AsgPill,
  Avatar,
  Book,
  ChevronRight,
  Clock,
  Countdown,
  Empty,
  GradeCell,
  Lamp,
  SubPill,
  Terminal,
  displayName,
  fmtAgo,
  fmtShort,
} from '../components/SharedUI';
import { useOverview } from '../hooks/useOverview';
import { useAuth } from '../state/auth';
import { useSetCrumb } from '../state/crumb';
import { data } from '../data';
import { navigate } from '../router';
import type { SubmissionSummary } from '../types';

function greeting(now = new Date()) {
  const h = now.getHours();
  if (h < 12) return 'Good morning';
  if (h < 18) return 'Good afternoon';
  return 'Good evening';
}

export function LecturerDashboardView() {
  const { session } = useAuth();
  const { loading, error, courses, assignments, submissionsByAssignment, reload } = useOverview();
  const [studentsByCourse, setStudentsByCourse] = useState<Record<number, number>>({});
  useSetCrumb(['Teaching'], 'Dashboard');

  useEffect(() => {
    if (courses.length === 0) return;
    let alive = true;
    (async () => {
      const pairs = await Promise.all(
        courses.map(async (course) => {
          const members = await data.members(course.id).catch(() => []);
          return [course.id, members.filter((m) => m.role === 'STUDENT').length] as const;
        }),
      );
      if (alive) setStudentsByCourse(Object.fromEntries(pairs));
    })();
    return () => {
      alive = false;
    };
  }, [courses]);

  const name = displayName(session?.username ?? 'lecturer');
  const firstName = name.split(' ')[0] || 'lecturer';
  const now = Date.now();

  const drafts = useMemo(
    () => assignments.filter((a) => a.assignmentStatus === 'DRAFT'),
    [assignments],
  );
  const openNow = useMemo(
    () =>
      assignments
        .filter(
          (a) =>
            a.assignmentStatus === 'PUBLISHED' &&
            (!a.closingTime || new Date(a.closingTime).getTime() > now),
        )
        .sort((a, b) => new Date(a.closingTime ?? 0).getTime() - new Date(b.closingTime ?? 0).getTime()),
    [assignments, now],
  );

  const allSubmissions = useMemo(() => {
    const rows: SubmissionSummary[] = [];
    for (const assignment of assignments) {
      rows.push(...(submissionsByAssignment[assignment.id] ?? []));
    }
    return rows;
  }, [assignments, submissionsByAssignment]);

  const pending = allSubmissions.filter((s) => s.status === 'QUEUED' || s.status === 'RUNNING');
  const flagged = allSubmissions
    .filter((s) => s.status !== 'ACCEPTED' && s.status !== 'QUEUED' && s.status !== 'RUNNING')
    .sort((a, b) => new Date(b.submittedAt ?? 0).getTime() - new Date(a.submittedAt ?? 0).getTime());
  const toReview = [...pending, ...flagged];
  const next = openNow[0];
  const nextSubmissions = next ? (submissionsByAssignment[next.id]?.length ?? 0) : 0;
  const totalStudents = Object.values(studentsByCourse).reduce((sum, n) => sum + n, 0);

  const courseCode = (courseId: number) => courses.find((c) => c.id === courseId)?.code ?? `#${courseId}`;

  if (loading) {
    return (
      <section className="screen on" id="scr-dashboard">
        <div style={{ padding: '60px', textAlign: 'center' }} className="mut">
          <span className="spin" /> Loading your courses…
        </div>
      </section>
    );
  }

  return (
    <section className="screen on" id="scr-dashboard">
      <span className="kicker">teaching · {new Date().getFullYear()}</span>
      <h1 className="pagetitle" style={{ margin: '6px 0 2px' }}>
        {greeting()}, {firstName}.
      </h1>
      <p className="mut" style={{ marginBottom: '4px' }}>
        {next ? (
          <>
            {courseCode(next.courseId)} · {next.code} has <b>{nextSubmissions}</b> submission
            {nextSubmissions === 1 ? '' : 's'} and closes in{' '}
            <Countdown until={next.closingTime} fallback="no deadline" />.
          </>
        ) : drafts.length > 0 ? (
          <>
            You have {drafts.length} draft assignment{drafts.length === 1 ? '' : 's'} waiting to be published.
          </>
        ) : (
          <>Nothing is open right now — create an assignment to get started.</>
        )}
      </p>

      {error && (
        <div className="banner b-err" style={{ margin: '16px 0' }}>
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
            <Book size={16} />
          </span>
          <div className="sv">{courses.length}</div>
          <div className="sl">Courses taught</div>
        </div>
        <div className="stat">
          <span className="si">
            <Lamp size={16} />
          </span>
          <div className="sv">{drafts.length}</div>
          <div className="sl">Draft assignments</div>
        </div>
        <div className="stat">
          <span className="si">
            <Clock size={16} />
          </span>
          <div className="sv">{openNow.length}</div>
          <div className="sl">Published &amp; open</div>
        </div>
        <div className={toReview.length > 0 ? 'stat hot' : 'stat'}>
          <span className="si">
            <Terminal size={16} />
          </span>
          <div className="sv">{toReview.length}</div>
          <div className="sl">Submissions to review</div>
        </div>
      </div>

      <div className="grid2">
        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          <div className="card">
            <div className="cardhead">
              <h2 className="cardtitle">Draft assignments</h2>
              <span className="spacer" />
              <span className="mono dim small">{drafts.length}</span>
            </div>
            {drafts.length === 0 ? (
              <Empty icon={<Lamp size={22} />}>
                No drafts. Open a course and create an assignment to see it here.
              </Empty>
            ) : (
              <table className="tbl hoverable">
                <thead>
                  <tr>
                    <th>Course</th>
                    <th>Title</th>
                    <th>Status</th>
                    <th>Opens</th>
                    <th className="num">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {drafts.slice(0, 5).map((a) => (
                    <tr key={a.id}>
                      <td className="mono dim">{courseCode(a.courseId)}</td>
                      <td>
                        {a.code}: {a.title}
                      </td>
                      <td>
                        <AsgPill status={a.assignmentStatus} />
                      </td>
                      <td className="dim small">{fmtShort(a.openingTime)}</td>
                      <td className="num">
                        <span className="rowactions">
                          <button
                            className="btn btn-ghost btn-sm"
                            onClick={() => navigate(`/teach/assignments/${a.id}/edit`)}
                          >
                            Edit
                          </button>
                          <button
                            className="btn btn-ghost btn-sm"
                            onClick={() => navigate(`/teach/assignments/${a.id}/tests`)}
                          >
                            Tests
                          </button>
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          <div className="card">
            <div className="cardhead">
              <h2 className="cardtitle">Submissions to review</h2>
              <span className="spacer" />
              <span className="mono dim small">{toReview.length}</span>
            </div>
            {toReview.length === 0 ? (
              <Empty icon={<Terminal size={22} />}>
                Nothing waiting. Submissions appear here as soon as students send code.
              </Empty>
            ) : (
              <table className="tbl hoverable">
                <thead>
                  <tr>
                    <th>Student</th>
                    <th className="num">Att.</th>
                    <th>Status</th>
                    <th className="num">Grade</th>
                    <th>When</th>
                    <th className="num" />
                  </tr>
                </thead>
                <tbody>
                  {toReview.slice(0, 6).map((s) => (
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
        </div>



        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          <div className="card pad">
            <h2 className="cardtitle" style={{ marginBottom: '12px' }}>
              Quick actions
            </h2>
            <div className="jumplist">
              {next && (
                <button className="jumpitem" onClick={() => navigate(`/teach/assignments/${next.id}/tests`)}>
                  <span className="jn">
                    <Lamp size={15} />
                  </span>
                  <span className="jt">
                    Manage {next.code} test cases
                    <span className="dim small" style={{ display: 'block' }}>
                      {next.testCount ?? 0} test{(next.testCount ?? 0) === 1 ? '' : 's'}
                    </span>
                  </span>
                  <span className="jr dim">
                    <ChevronRight />
                  </span>
                </button>
              )}
              {next && (
                <button className="jumpitem" onClick={() => navigate(`/teach/assignments/${next.id}/submissions`)}>
                  <span className="jn">
                    <Terminal size={15} />
                  </span>
                  <span className="jt">
                    {next.code} submissions
                    <span className="dim small" style={{ display: 'block' }}>
                      {nextSubmissions} received
                    </span>
                  </span>
                  <span className="jr dim">
                    <ChevronRight />
                  </span>
                </button>
              )}
              {courses[0] && (
                <button className="jumpitem" onClick={() => navigate(`/teach/courses/${courses[0].id}`)}>
                  <span className="jn">
                    <Book size={15} />
                  </span>
                  <span className="jt">
                    Open {courses[0].code} course page
                    <span className="dim small" style={{ display: 'block' }}>
                      {studentsByCourse[courses[0].id] ?? 0} students
                    </span>
                  </span>
                  <span className="jr dim">
                    <ChevronRight />
                  </span>
                </button>
              )}
            </div>
          </div>

          <div className="card pad">
            <h2 className="cardtitle" style={{ marginBottom: '12px' }}>
              Courses taught
            </h2>
            {courses.length === 0 ? (
              <p className="dim small">
                No courses yet. Ask an admin to attach you to a course, or create one through the API.
              </p>
            ) : (
              <div className="jumplist">
                {courses.map((c) => (
                  <button key={c.id} className="jumpitem" onClick={() => navigate(`/teach/courses/${c.id}`)}>
                    <span className="jn" style={{ borderColor: 'var(--line)', color: 'var(--mut)' }}>
                      <Book size={15} />
                    </span>
                    <span className="jt">
                      {c.code} · {c.courseName}
                      <span className="dim small" style={{ display: 'block' }}>
                        {studentsByCourse[c.id] ?? 0} students
                      </span>
                    </span>
                    <span className="jr dim">
                      <ChevronRight />
                    </span>
                  </button>
                ))}
              </div>
            )}
            <p className="dim small" style={{ marginTop: '10px' }}>
              {totalStudents} student{totalStudents === 1 ? '' : 's'} across your courses.
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
