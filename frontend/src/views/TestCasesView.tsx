import { useEffect, useMemo, useState } from 'react';
import { AsgPill, Empty, Lamp, Terminal, fmtShort } from '../components/SharedUI';
import { useSetCrumb } from '../state/crumb';
import { data } from '../data';
import { navigate } from '../router';
import type { AssignmentDetail, TestCase } from '../types';

interface FormState {
  name: string;
  input: string;
  expectedOutput: string;
  isPublic: boolean;
  timeLimitMs: string;
  memoryLimitKib: string;
}

const EMPTY_FORM: FormState = {
  name: '',
  input: '',
  expectedOutput: '',
  isPublic: false,
  timeLimitMs: '2000',
  memoryLimitKib: '262144',
};

type Filter = 'all' | 'public' | 'private';

export function TestCasesView({ assignmentId }: { assignmentId: number }) {
  const [assignment, setAssignment] = useState<AssignmentDetail | null>(null);
  const [submissionCount, setSubmissionCount] = useState(0);
  const [tests, setTests] = useState<TestCase[]>([]);
  const [filter, setFilter] = useState<Filter>('all');
  const [editing, setEditing] = useState<TestCase | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [note, setNote] = useState('');

  const locked = submissionCount > 0;
  useSetCrumb(['Teaching'], assignment ? `${assignment.code} · Test cases` : 'Test cases');

  async function load() {
    setLoading(true);
    try {
      const detail = await data.assignment(assignmentId);
      const [siblings, list] = await Promise.all([
        data.assignments(detail.courseId).catch(() => []),
        data.tests(assignmentId),
      ]);
      setAssignment(detail);
      setSubmissionCount(siblings.find((a) => a.id === assignmentId)?.submissionCount ?? 0);
      setTests(list);
      setError('');
    } catch (err: any) {
      setError(err?.message || 'Could not load the test cases.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [assignmentId]);

  const publicCount = tests.filter((t) => t.isPublic).length;
  const privateCount = tests.length - publicCount;

  const rows = useMemo(() => {
    const list = tests.filter((t) =>
      filter === 'all' ? true : filter === 'public' ? t.isPublic : !t.isPublic,
    );
    return [...list].sort((a, b) => a.displayOrder - b.displayOrder);
  }, [tests, filter]);

  function startCreate() {
    setEditing(null);
    setForm(EMPTY_FORM);
    setFormOpen(true);
    setNote('');
  }

  function startEdit(test: TestCase) {
    setEditing(test);
    setForm({
      name: test.name ?? '',
      input: test.input ?? '',
      expectedOutput: test.expectedOutput,
      isPublic: test.isPublic,
      timeLimitMs: String(test.timeLimitMs ?? 2000),
      memoryLimitKib: String(test.memoryLimitKib ?? 262144),
    });
    setFormOpen(true);
    setNote('');
  }

  function closeForm() {
    setFormOpen(false);
    setEditing(null);
    setForm(EMPTY_FORM);
  }

  async function submitForm() {
    setError('');
    setNote('');
    if (!form.expectedOutput.trim()) {
      setError('Expected output is required.');
      return;
    }
    setBusy(true);
    try {
      const body = {
        name: form.name.trim() || null,
        input: form.input,
        expectedOutput: form.expectedOutput,
        isPublic: form.isPublic,
        timeLimitMs: Number(form.timeLimitMs) || 2000,
        memoryLimitKib: Number(form.memoryLimitKib) || 262144,
      };
      if (editing) {
        await data.updateTest(assignmentId, editing.testId, body);
        setNote('Test case updated.');
      } else {
        await data.createTest(assignmentId, body);
        setNote('Test case added.');
      }
      closeForm();
      await load();
    } catch (err: any) {
      setError(err?.message || 'Could not save the test case.');
    } finally {
      setBusy(false);
    }
  }

  async function remove(test: TestCase) {
    if (!window.confirm(`Delete “${test.name ?? `Test ${test.displayOrder}`}”?`)) return;
    setBusy(true);
    setError('');
    try {
      await data.deleteTest(assignmentId, test.testId);
      setNote('Test case deleted.');
      await load();
    } catch (err: any) {
      setError(err?.message || 'Could not delete the test case.');
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <section className="screen on" id="scr-tests">
        <div style={{ padding: '60px', textAlign: 'center' }} className="mut">
          <span className="spin" /> Loading test cases…
        </div>
      </section>
    );
  }

  if (!assignment) {
    return (
      <section className="screen on" id="scr-tests">
        <div className="card pad">
          <p>{error || 'Assignment not found.'}</p>
          <button
            className="btn btn-sec btn-sm"
            style={{ marginTop: '12px' }}
            onClick={() => navigate('/teach')}
          >
            Back to dashboard
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="screen on" id="scr-tests">
      <span className="kicker">{assignment.code} · test cases</span>
      <h1 className="pagetitle" style={{ margin: '6px 0 2px' }}>
        {assignment.title}
      </h1>
      <div className="toolbar" style={{ marginTop: '10px' }}>
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => navigate(`/teach/courses/${assignment.courseId}`)}
        >
          ← Course
        </button>
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => navigate(`/teach/assignments/${assignment.id}/edit`)}
        >
          Assignment settings
        </button>
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => navigate(`/teach/assignments/${assignment.id}/submissions`)}
        >
          Submissions
        </button>
        <span className="spacer" />
        <AsgPill status={assignment.assignmentStatus} />
        <button className="btn btn-primary btn-sm" onClick={startCreate} disabled={locked || busy}>
          Add test case
        </button>
      </div>

      <div className="card pad" style={{ marginBottom: '16px' }}>
        <div style={{ display: 'flex', gap: '18px', flexWrap: 'wrap', alignItems: 'center' }}>
          <span className="chip">{publicCount} public</span>
          <span className="dim small">visible to students</span>
          <span className="chip" style={{ color: 'var(--violet-hi)', borderColor: 'color-mix(in srgb, var(--violet) 45%, transparent)' }}>
            {privateCount} private
          </span>
          <span className="dim small">hidden, used for full grading</span>
          <span className="spacer" style={{ marginLeft: 'auto' }} />
          <span className="mono dim small">{submissionCount} submissions</span>
        </div>
      </div>

      {locked && (
        <div className="notice amber" style={{ marginBottom: '16px' }}>
          <Lamp />
          <span>
            This assignment already has submission results, so its test cases are locked. Delete the
            submissions first if you really need to change them.
          </span>
        </div>
      )}
      {error && (
        <div className="notice red" style={{ marginBottom: '16px' }}>
          <Terminal />
          <span>{error}</span>
        </div>
      )}
      {note && (
        <div className="notice violet" style={{ marginBottom: '16px' }}>
          <Lamp />
          <span>{note}</span>
        </div>
      )}

      {formOpen && (
        <div className="card pad" style={{ marginBottom: '18px' }}>
          <h2 className="cardtitle" style={{ marginBottom: '12px' }}>
            {editing ? `Edit test #${editing.displayOrder}` : 'New test case'}
          </h2>
          <div className="grid3">
            <div className="field">
              <label htmlFor="tc-name">Name</label>
              <input
                id="tc-name"
                type="text"
                placeholder="Sample: append three"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="tc-time">Time limit (ms)</label>
              <input
                id="tc-time"
                type="number"
                min={100}
                value={form.timeLimitMs}
                onChange={(e) => setForm({ ...form, timeLimitMs: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="tc-mem">Memory limit (KiB)</label>
              <input
                id="tc-mem"
                type="number"
                min={1024}
                value={form.memoryLimitKib}
                onChange={(e) => setForm({ ...form, memoryLimitKib: e.target.value })}
              />
            </div>
          </div>
          <div className="grid2">
            <div className="field">
              <label htmlFor="tc-in">Input</label>
              <textarea
                id="tc-in"
                rows={7}
                style={{ fontFamily: 'var(--mono)', fontSize: '12.5px' }}
                value={form.input}
                onChange={(e) => setForm({ ...form, input: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="tc-out">Expected output</label>
              <textarea
                id="tc-out"
                rows={7}
                style={{ fontFamily: 'var(--mono)', fontSize: '12.5px' }}
                value={form.expectedOutput}
                onChange={(e) => setForm({ ...form, expectedOutput: e.target.value })}
              />
            </div>
          </div>
          <div className="toolbar" style={{ marginBottom: 0 }}>
            <label className="small" style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
              <input
                type="checkbox"
                checked={form.isPublic}
                onChange={(e) => setForm({ ...form, isPublic: e.target.checked })}
              />
              Public sample (students can see it)
            </label>
            <span className="spacer" />
            <button className="btn btn-ghost btn-sm" onClick={closeForm} disabled={busy}>
              Cancel
            </button>
            <button className="btn btn-primary btn-sm" onClick={submitForm} disabled={busy}>
              {busy ? <span className="spin" /> : null} {editing ? 'Save changes' : 'Add test case'}
            </button>
          </div>
        </div>
      )}

      <div className="card">
        <div className="cardhead">
          <h2 className="cardtitle">All test cases</h2>
          <span className="spacer" />
          <div className="tabs" style={{ border: 'none', margin: 0 }}>
            <button className={filter === 'all' ? 'tab on' : 'tab'} onClick={() => setFilter('all')}>
              All <span className="cnt">{tests.length}</span>
            </button>
            <button className={filter === 'public' ? 'tab on' : 'tab'} onClick={() => setFilter('public')}>
              Public <span className="cnt">{publicCount}</span>
            </button>
            <button className={filter === 'private' ? 'tab on' : 'tab'} onClick={() => setFilter('private')}>
              Private <span className="cnt">{privateCount}</span>
            </button>
          </div>
        </div>

        {rows.length === 0 ? (
          <Empty icon={<Lamp size={22} />}>
            No test cases yet. Public tests are visible to students; private tests are hidden and used
            for grading.
          </Empty>
        ) : (
          <table className="tbl hoverable">
            <thead>
              <tr>
                <th>#</th>
                <th>Name</th>
                <th>Visibility</th>
                <th>Input</th>
                <th>Expected output</th>
                <th className="num">Limits</th>
                <th className="num">Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((t) => (
                <tr key={t.testId}>
                  <td className="mono dim">{t.displayOrder}</td>
                  <td>{t.name || <span className="dim">untitled</span>}</td>
                  <td>
                    <span className={t.isPublic ? 'pill vis-pub' : 'pill vis-priv'}>
                      <span className="dot" />
                      {t.isPublic ? 'public' : 'private'}
                    </span>
                  </td>
                  <td>
                    <pre
                      className="mono dim small"
                      style={{ whiteSpace: 'pre-wrap', maxWidth: '240px', margin: 0 }}
                    >
                      {t.input ? t.input.slice(0, 120) : '(no input)'}
                    </pre>
                  </td>
                  <td>
                    <pre
                      className="mono dim small"
                      style={{ whiteSpace: 'pre-wrap', maxWidth: '220px', margin: 0 }}
                    >
                      {t.expectedOutput.slice(0, 120)}
                    </pre>
                  </td>
                  <td className="num mono dim small">
                    {((t.timeLimitMs ?? 0) / 1000).toFixed(1)}s ·{' '}
                    {Math.round((t.memoryLimitKib ?? 0) / 1024)}MB
                  </td>
                  <td className="num">
                    <span className="rowactions">
                      <button
                        className="btn btn-ghost btn-sm"
                        disabled={locked || busy}
                        onClick={() => startEdit(t)}
                      >
                        Edit
                      </button>
                      <button
                        className="btn btn-ghost btn-sm"
                        disabled={locked || busy}
                        onClick={() => remove(t)}
                      >
                        Delete
                      </button>
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <p className="dim small" style={{ marginTop: '12px' }}>
        Assignment created {fmtShort(assignment.openingTime)} · attempt limit{' '}
        {assignment.submissionLimit ?? 3}
      </p>
    </section>
  );
}

