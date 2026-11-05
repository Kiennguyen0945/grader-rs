import { useMemo, useState } from 'react';
import { Search, Terminal } from '../components/SharedUI';
import { useOverview } from '../hooks/useOverview';
import { useSetCrumb } from '../state/crumb';
import { data } from '../data';
import { navigate } from '../router';

export function CoursesView({ variant = 'student' }: { variant?: 'student' | 'lecturer' }) {
  const { loading, error, courses, assignments, reload } = useOverview();
  const [query, setQuery] = useState('');
  const isLecturer = variant === 'lecturer';
  useSetCrumb(['Teaching'], isLecturer ? 'Courses' : 'My courses');

  const rows = useMemo(() => {
    const now = Date.now();
    return courses
      .map((course) => {
        const own = assignments.filter((a) => a.courseId === course.id);
        const open = own.filter(
          (a) => a.assignmentStatus === 'PUBLISHED' && (!a.closingTime || new Date(a.closingTime).getTime() > now),
        );
        return { course, total: own.length, open };
      })
      .filter(({ course }) => {
        const haystack = `${course.code} ${course.courseName} ${course.description ?? ''}`.toLowerCase();
        return haystack.includes(query.trim().toLowerCase());
      });
  }, [courses, assignments, query]);

  const draftsFor = (courseId: number) =>
    assignments.filter((a) => a.courseId === courseId && a.assignmentStatus === 'DRAFT').length;

  if (loading) {
    return (
      <section className="screen on" id="scr-courses">
        <div style={{ padding: '60px', textAlign: 'center' }} className="mut">
          <span className="spin" /> Loading courses…
        </div>
      </section>
    );
  }

  return (
    <section className="screen on" id="scr-courses">
      <span className="kicker">
        {isLecturer ? `teaching · ${courses.length}` : `enrolled · ${courses.length}`}
      </span>
      <h1 className="pagetitle" style={{ margin: '6px 0 18px' }}>
        {isLecturer ? 'Courses I teach' : 'My courses'}
      </h1>

      <div className="toolbar">
        <div className="searchbox" style={{ width: '290px' }}>
          <Search />
          <input
            id="cq"
            placeholder="Search code, name, topic…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>
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

      <div className="coursegrid" id="coursegrid">
        {rows.length === 0 ? (
          <div className="card pad">
            <p className="dim">No courses match your search.</p>
          </div>
        ) : (
          rows.map(({ course, total, open }) => {
            const next = open[0];
            return (
              <div className="coursecard" key={course.id} data-id={course.code} onClick={() => navigate(isLecturer ? `/teach/courses/${course.id}` : `/courses/${course.id}`)}>
                <div className="cc-top">
                  <span className="chip">{course.code}</span>
                  {course.term && <span className="cc-term">{course.term}</span>}
                </div>
                <h3>{course.courseName}</h3>
                <p className="desc">{course.description || 'No description provided.'}</p>
                <div className="cc-meta">
                  <span>{total} assignments</span>
                  {typeof course.students === 'number' && <span>{course.students} students</span>}
                  {(data.demo || course.lecturer) && <span>{course.lecturer || '—'}</span>}
                </div>
                <div className="cc-foot">
                  {next ? (
                    <span className="duedot">
                      {open.length} due · {next.code.toLowerCase()}
                    </span>
                  ) : (
                    <span className="dim small">
                      {isLecturer ? `${draftsFor(course.id)} drafts` : 'no open assignments'}
                    </span>
                  )}
                  <span className="spacer" style={{ marginLeft: 'auto' }} />
                  <span className="btn btn-sec btn-sm">{isLecturer ? 'Manage' : 'Open course'}</span>
                </div>
              </div>
            );
          })
        )}
      </div>
    </section>
  );
}
