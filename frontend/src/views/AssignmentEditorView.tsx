import { useEffect, useState } from 'react';
import { AsgPill, Lamp, Terminal } from '../components/SharedUI';
import { useAuth } from '../state/auth';
import { useSetCrumb } from '../state/crumb';
import { data } from '../data';
import { navigate } from '../router';
import type { AssignmentStatus, Course } from '../types';

type Props = { mode: 'create'; courseId: number } | { mode: 'edit'; assignmentId: number };

/** ISO → value for <input type="datetime-local"> */
function toLocalInput(iso?: string | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** <input type="datetime-local"> → LocalDateTime string the backend accepts */
function fromLocalInput(value: string): string | null {
  if (!value) return null;
  return value.length === 16 ? `${value}:00` : value;
}

export function AssignmentEditorView(props: Props) {
  const { session } = useAuth();
  const [courses, setCourses] = useState<Course[]>([]);
  const [courseId, setCourseId] = useState<number>(props.mode === 'create' ? props.courseId : 0);
  const [code, setCode] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [submissionLimit, setSubmissionLimit] = useState('3');
  const [opening, setOpening] = useState('');
  const [closing, setClosing] = useState('');
  const [testCount, setTestCount] = useState(0);
  const [status, setStatus] = useState<AssignmentStatus>('DRAFT');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [note, setNote] = useState('');

  const assignmentId = props.mode === 'edit' ? props.assignmentId : null;
  useSetCrumb(
    ['Teaching', 'Courses'],
    props.mode === 'create' ? 'New assignment' : `Edit ${code || 'assignment'}`,
  );

  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const list = await data.courses();
        if (!alive) return;
        setCourses(list);
        if (props.mode === 'edit') {
          const detail = await data.assignment(props.assignmentId);
          if (!alive) return;
          setCourseId(detail.courseId);
          setCode(detail.code);
          setTitle(detail.title);
          setDescription(detail.description ?? '');
          setSubmissionLimit(String(detail.submissionLimit ?? 3));
          setOpening(toLocalInput(detail.openingTime));
          setClosing(toLocalInput(detail.closingTime));
          setStatus(detail.assignmentStatus);
          const siblings = await data.assignments(detail.courseId).catch(() => []);
          if (alive) setTestCount(siblings.find((a) => a.id === detail.id)?.testCount ?? 0);
        } else if (list.length > 0) {
          setCourseId((current) => current || list[0].id);
        }
      } catch (err: any) {
        if (alive) setError(err?.message || 'Could not load the assignment.');
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [props]);

  async function save(nextStatus: AssignmentStatus) {
    setError('');
    setNote('');
    if (!code.trim() || !title.trim()) {
      setError('Code and title are required.');
      return;
    }
    if (opening && closing && new Date(closing) <= new Date(opening)) {
      setError('Closing time must be after opening time.');
      return;
    }
    setSaving(true);
    try {
      const body: Record<string, unknown> = {
        code: code.trim(),
        title: title.trim(),
        assignmentStatus: nextStatus,
        description: description.trim() || null,
        submissionLimit: Number(submissionLimit) || 3,
        openingTime: fromLocalInput(opening),
        closingTime: fromLocalInput(closing),
        allowedLanguages: ['JAVA'],
      };
      if (props.mode === 'create') {
        if (!session?.userId) throw new Error('Missing user id in the session token.');
        const created = await data.createAssignment(courseId, { ...body, userId: session.userId });
        navigate(`/teach/courses/${created.courseId}`);
        return;
      }
      const updated = await data.updateAssignment(props.assignmentId, body);
      setStatus(updated.assignmentStatus);
      setNote(nextStatus === 'PUBLISHED' ? 'Published. Students can see it now.' : 'Saved as draft.');
    } catch (err: any) {
      setError(err?.message || 'Could not save the assignment.');
    } finally {
      setSaving(false);
    }
  }

  async function remove() {
    if (props.mode !== 'edit') return;
    if (!window.confirm('Delete this assignment and its test cases?')) return;
    setSaving(true);
    try {
      await data.deleteAssignment(props.assignmentId);
      navigate(`/teach/courses/${courseId}`);
    } catch (err: any) {
      setError(err?.message || 'Could not delete the assignment.');
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <section className="screen on" id="scr-editor">
        <div style={{ padding: '60px', textAlign: 'center' }} className="mut">
          <span className="spin" /> Loading assignment…
        </div>
      </section>
    );
  }

  return (
    <section className="screen on" id="scr-editor">
      <span className="kicker">
        {props.mode === 'create' ? 'new assignment' : `editing · ${status.toLowerCase()}`}
      </span>
      <h1 className="pagetitle" style={{ margin: '6px 0 2px' }}>
        {props.mode === 'create' ? 'New assignment' : title || 'Untitled assignment'}
      </h1>
      <div className="toolbar" style={{ marginTop: '10px' }}>
        <button className="btn btn-ghost btn-sm" onClick={() => navigate(`/teach/courses/${courseId}`)}>
          ← Back to course
        </button>
        <span className="spacer" />
        {assignmentId !== null && (
          <>
            <button
              className="btn btn-sec btn-sm"
              onClick={() => navigate(`/teach/assignments/${assignmentId}/tests`)}
            >
              Manage test cases
            </button>
            <button
              className="btn btn-sec btn-sm"
              onClick={() => navigate(`/teach/assignments/${assignmentId}/submissions`)}
            >
              Submissions
            </button>
          </>
        )}
      </div>

      {error && (
        <div className="notice red" style={{ marginBottom: '16px' }}>
          <Terminal />
          <span>{error}</span>
        </div>
      )}
      {note && (
        <div className="notice amber" style={{ marginBottom: '16px' }}>
          <Lamp />
          <span>{note}</span>
        </div>
      )}

      <div className="grid2">
        <div className="card pad">
          <div className="grid3">
            <div className="field">
              <label htmlFor="ed-code">Code</label>
              <input
                id="ed-code"
                type="text"
                value={code}
                placeholder="LAB 5"
                onChange={(e) => setCode(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="ed-course">Course</label>
              <select
                id="ed-course"
                value={courseId}
                disabled={props.mode === 'edit'}
                onChange={(e) => setCourseId(Number(e.target.value))}
              >
                {courses.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.code} — {c.courseName}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="ed-lang">Language</label>
              <select id="ed-lang" value="JAVA" disabled>
                <option value="JAVA">Java</option>
              </select>
            </div>
          </div>

          <div className="field">
            <label htmlFor="ed-title">Title</label>
            <input
              id="ed-title"
              type="text"
              value={title}
              placeholder="Linked list"
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>

          <div className="grid3">
            <div className="field">
              <label htmlFor="ed-opens">Opens</label>
              <input
                id="ed-opens"
                type="datetime-local"
                value={opening}
                onChange={(e) => setOpening(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="ed-closes">Closes</label>
              <input
                id="ed-closes"
                type="datetime-local"
                value={closing}
                onChange={(e) => setClosing(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="ed-limit">Attempt limit</label>
              <input
                id="ed-limit"
                type="number"
                min={1}
                value={submissionLimit}
                onChange={(e) => setSubmissionLimit(e.target.value)}
              />
            </div>
          </div>

          <div className="field">
            <label htmlFor="ed-desc">Description</label>
            <textarea
              id="ed-desc"
              rows={10}
              value={description}
              placeholder="What students must implement, with any constraints."
              onChange={(e) => setDescription(e.target.value)}
            />
            <div className="dim small" style={{ marginTop: '6px' }}>
              Plain text. Students see this on the assignment page once it is published.
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          <div className="card pad">
            <div
              className="cardhead"
              style={{ padding: 0, border: 'none', marginBottom: '12px' }}
            >
              <h2 className="cardtitle">Status</h2>
              <span className="spacer" />
              <AsgPill status={status} />
            </div>
            {status === 'DRAFT' && (
              <p className="dim small" style={{ marginBottom: '12px' }}>
                Drafts are hidden from students. Publish when at least one test case exists.
              </p>
            )}
            <button
              className="btn btn-sec"
              style={{ width: '100%', marginBottom: '9px' }}
              disabled={saving}
              onClick={() => save('DRAFT')}
            >
              {saving ? <span className="spin" /> : null} Save draft
            </button>
            <button
              className="btn btn-primary"
              style={{ width: '100%' }}
              disabled={saving}
              onClick={() => save('PUBLISHED')}
            >
              Publish to students
            </button>
            {testCount === 0 && (
              <p className="dim small" style={{ marginTop: '10px' }}>
                This assignment has 0 test cases. Publishing without tests will be flagged.
              </p>
            )}
            {assignmentId !== null && (
              <button
                className="btn btn-ghost btn-sm"
                style={{ width: '100%', marginTop: '14px', color: 'var(--err)' }}
                disabled={saving}
                onClick={remove}
              >
                Delete assignment
              </button>
            )}
          </div>

          <div className="card pad">
            <h2 className="cardtitle" style={{ marginBottom: '10px' }}>
              Test cases
            </h2>
            <p className="dim small">
              {testCount} test case{testCount === 1 ? '' : 's'} attached to this assignment.
            </p>
            {assignmentId !== null ? (
              <button
                className="btn btn-sec btn-sm"
                style={{ width: '100%', marginTop: '10px' }}
                onClick={() => navigate(`/teach/assignments/${assignmentId}/tests`)}
              >
                Open test cases
              </button>
            ) : (
              <p className="dim small" style={{ marginTop: '10px' }}>
                Save the assignment first, then add test cases.
              </p>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}

