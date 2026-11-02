import { useCallback, useEffect, useState } from 'react';
import { data } from '../data';
import type { AssignmentSummary, Course, LeaderboardRow, SubmissionSummary } from '../types';

export interface OverviewState {
  loading: boolean;
  error: string;
  courses: Course[];
  assignments: AssignmentSummary[];
  submissionsByAssignment: Record<number, SubmissionSummary[]>;
}

const empty: OverviewState = {
  loading: true,
  error: '',
  courses: [],
  assignments: [],
  submissionsByAssignment: {},
};

/**
 * One loader shared by the dashboard, courses, course detail and history
 * screens. Everything comes from endpoints that already exist:
 *   GET /courses
 *   GET /courses/{id}/assignments
 *   GET /assignments/{id}/submissions
 */
export function useOverview() {
  const [state, setState] = useState<OverviewState>(empty);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let alive = true;
    (async () => {
      setState((s) => ({ ...s, loading: true, error: '' }));
      try {
        const courses = await data.courses();
        const perCourse = await Promise.all(
          courses.map((c) => data.assignments(c.id).catch(() => [] as AssignmentSummary[])),
        );
        const assignments = perCourse
          .flat()
          .map((a) => ({ ...a, courseCode: courses.find((c) => c.id === a.courseId)?.code ?? a.courseCode }));

        const pairs = await Promise.all(
          assignments.map(async (a) => {
            const subs = await data.submissions(a.id).catch(() => [] as SubmissionSummary[]);
            return [a.id, subs] as const;
          }),
        );
        if (!alive) return;
        setState({
          loading: false,
          error: '',
          courses,
          assignments,
          submissionsByAssignment: Object.fromEntries(pairs),
        });
      } catch (err: any) {
        if (!alive) return;
        setState({ ...empty, loading: false, error: err?.message || 'Could not load your courses.' });
      }
    })();
    return () => {
      alive = false;
    };
  }, [reloadKey]);

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);
  return { ...state, reload };
}

export function flattenSubmissions(state: OverviewState) {
  const rows: Array<SubmissionSummary & { assignment?: AssignmentSummary }> = [];
  for (const assignment of state.assignments) {
    for (const sub of state.submissionsByAssignment[assignment.id] ?? []) {
      rows.push({ ...sub, assignment });
    }
  }
  return rows;
}

/** Average of the best grade per assignment, scaled to the prototype's 0–100 score. */
export function ownLeaderboardRow(
  name: string,
  username: string,
  submissions: SubmissionSummary[],
): LeaderboardRow {
  const best = new Map<number, number>();
  for (const s of submissions) {
    if (typeof s.grade !== 'number') continue;
    const current = best.get(s.assignmentId);
    if (current === undefined || s.grade > current) best.set(s.assignmentId, s.grade);
  }
  const grades = [...best.values()];
  const average = grades.length ? grades.reduce((a, b) => a + b, 0) / grades.length : 0;
  return {
    rank: 1,
    username,
    name,
    initials: '',
    score: Math.round(average * 100) / 10,
    attempts: submissions.length,
  };
}
