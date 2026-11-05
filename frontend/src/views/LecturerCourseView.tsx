import { useEffect, useMemo, useState } from 'react';
import {
  AsgPill,
  Avatar,
  Countdown,
  Empty,
  Lamp,
  Search,
  Terminal,
  displayName,
  fmtShort,
} from '../components/SharedUI';
import { useSetCrumb } from '../state/crumb';
import { data } from '../data';
import { navigate } from '../router';
import type { AssignmentSummary, Course, CourseMember } from '../types';

export function LecturerCourseView({ courseId }: { courseId: number }) {
  const [course, setCourse] = useState<Course | null>(null);
  const [assignments, setAssignments] = useState<AssignmentSummary[]>([]);
  const [members, setMembers] = useState<CourseMember[]>([]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useSetCrumb(['Teaching', 'Courses'], course?.code ?? `Course #${courseId}`);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    (async () => {
      try {
        const [c, list, people] = await Promise.all([
          data.course(courseId),
          data.assignments(courseId),
          data.members(courseId).catch(() => []),
        ]);
        if (!alive) return;
        setCourse(c);
        setAssignments(list);
        setMembers(people);
        setError('');
      } catch (err: any) {
        if (alive) setError(err?.message || 'Could not load this course.');
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [courseId]);

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    return assignments
      .filter((a) => `${a.code} ${a.title} ${a.assignmentStatus}`.toLowerCase().includes(q))
      .sort((a, b) => new Date(b.openingTime ?? 0).getTime() - new Date(a.openingTime ?? 0).getTime());
  }, [assignments, query]);

  const students = members.filter((m) => m.role === 'STUDENT');
  const published = assignments.filter((a) => a.assignmentStatus === 'PUBLISHED');
  const testTotal = assignments.reduce((sum, a) => sum + (a.testCount ?? 0), 0);

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
          <button
            className="btn btn-sec btn-sm"
            style={{ marginTop: '12px' }}
            onClick={() => navigate('/teach/courses')}
          >
            Back to courses
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="screen on" id="scr-course">
      <span className="kicker">{course.code} · teaching</span>
      <h1 className="pagetitle" style={{ margin: '6px 0 2px' }}>
        {course.courseName}
      </h1>
      <p className="mut">{course.description || 'No description provided.'}</p>

      <div className="stats">
        <div className="stat">
          <div className="sv">{assignments.length}</div>
          <div className="sl">Assignments</div>
        </div>
        <div className="stat">
          <div className="sv">{published.length}</div>
          <div className="sl">Published</div>
        </div>
        <div className="stat">
          <div className="sv">{testTotal}</div>
          <div className="sl">Test cases</div>
        </div>
        <div className="stat">
          <div className="sv">{students.length}</div>
          <div className="sl">Students</div>
        </div>
      </div>

      <div className="toolbar">
        <div className="searchbox" style={{ width: '290px' }}>
          <Search />
          <input
            placeholder="Search code, title, status…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>
        <span className="spacer" />
        <button
          className="btn btn-primary"
          onClick={() => navigate(`/teach/courses/${course.id}/assignments/new`)}
        >
          New assignment
        </button>
      </div>

      <div className="card" style={{ marginBottom: '18px' }}>
        <div className="cardhead">
          <h2 className="cardtitle">Assignments</h2>
          <span className="spacer" />
          <span className="mono dim small">{rows.length}</span>
        </div>
        {rows.length === 0 ? (
          <Empty icon={<Lamp size={22} />}>
            Nothing here yet. Create the first assignment for {course.code}.
          </Empty>
        ) : (
          <table className="tbl hoverable">
            <thead>
              <tr>
                <th>Code</th>
                <th>Title</th>
                <th>Status</th>
                <th>Opens</th>
                <th>Closes</th>
                <th className="num">Tests</th>
                <th className="num">Subs</th>
                <th className="num">Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((a) => (
                <tr key={a.id}>
                  <td className="mono">{a.code}</td>
                  <td>
                    {a.title}
                    {a.assignmentStatus === 'PUBLISHED' &&
                      a.closingTime &&
                      new Date(a.closingTime).getTime() > Date.now() && (
                        <div className="dim small">
                          closes in <Countdown until={a.closingTime} />
                        </div>
                      )}
                  </td>
                  <td>
                    <AsgPill status={a.assignmentStatus} />
                  </td>
                  <td className="dim small">{fmtShort(a.openingTime)}</td>
                  <td className="dim small">{fmtShort(a.closingTime)}</td>
                  <td className="num mono">{a.testCount ?? 0}</td>
                  <td className="num mono">{a.submissionCount ?? 0}</td>
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
                      <button
                        className="btn btn-ghost btn-sm"
                        onClick={() => navigate(`/teach/assignments/${a.id}/submissions`)}
                      >
                        Submissions
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
          <h2 className="cardtitle">Students</h2>
          <span className="spacer" />
          <span className="mono dim small">{students.length}</span>
        </div>
        {students.length === 0 ? (
          <Empty icon={<Terminal size={22} />}>
            Nobody is enrolled yet. POST /api/courses/{course.id}/members to add a student.
          </Empty>
        ) : (
          <table className="tbl">
            <thead>
              <tr>
                <th>Student</th>
                <th>Username</th>
              </tr>
            </thead>
            <tbody>
              {students.map((m) => (
                <tr key={m.id}>
                  <td>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '9px' }}>
                      <Avatar name={displayName(m.username)} size={26} radius={7} />
                      {displayName(m.username)}
                    </span>
                  </td>
                  <td className="mono dim">@{m.username}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}

