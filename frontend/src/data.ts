import { apiRequest } from './api';
import { isDemo } from './dev/demo';
import * as demo from './dev/fixtures';
import type {
  AssignmentDetail,
  AssignmentSummary,
  Course,
  CourseMember,
  LeaderboardRow,
  NotificationItem,
  SubmissionDetail,
  SubmissionSummary,
  TestCase,
} from './types';

/**
 * Single data gateway for every view.
 *
 * Default: real Spring Boot API (unchanged).
 * `?demo=1` in a dev build: prototype sample data, so screens can be compared
 * 1:1 with docs/assets/HTML-StudentWorkflow/*.html.
 */
export const data = {
  demo: isDemo(),

  async courses(): Promise<Course[]> {
    if (isDemo()) return demo.demoCourses;
    return apiRequest<Course[]>('/courses');
  },

  async course(courseId: number): Promise<Course> {
    if (isDemo()) {
      return demo.demoCourses.find((c) => c.id === courseId) ?? demo.demoCourses[0];
    }
    return apiRequest<Course>(`/courses/${courseId}`);
  },

  async assignments(courseId: number): Promise<AssignmentSummary[]> {
    if (isDemo()) return demo.assignmentsForCourse(courseId);
    return apiRequest<AssignmentSummary[]>(`/courses/${courseId}/assignments`);
  },

  async assignment(assignmentId: number): Promise<AssignmentDetail> {
    if (isDemo()) return demo.demoAssignmentDetail(assignmentId);
    return apiRequest<AssignmentDetail>(`/assignments/${assignmentId}`);
  },

  async submissions(assignmentId: number): Promise<SubmissionSummary[]> {
    if (isDemo()) return demo.submissionsForAssignment(assignmentId);
    return apiRequest<SubmissionSummary[]>(`/assignments/${assignmentId}/submissions`);
  },

  async submission(submissionId: number): Promise<SubmissionDetail> {
    if (isDemo()) return demo.demoSubmissionDetail(submissionId);
    return apiRequest<SubmissionDetail>(`/submissions/${submissionId}`);
  },

  async createSubmission(assignmentId: number, sourceCode: string): Promise<SubmissionSummary> {
    if (isDemo()) return demo.demoCreateSubmission(assignmentId, sourceCode);
    return apiRequest<SubmissionSummary>(`/assignments/${assignmentId}/submissions`, {
      method: 'POST',
      body: { sourceCode },
    });
  },

  /* ---------------------------------------------------------------------
   * Lecturer ("teaching") gateway.
   * `submissions()` above already returns every student's rows for a
   * lecturer because the backend decides scope from the JWT; in demo mode
   * the fixtures provide a multi-student queue instead.
   * ------------------------------------------------------------------- */

  async assignmentSubmissions(assignmentId: number): Promise<SubmissionSummary[]> {
    if (isDemo()) return demo.demoReviewQueueFor(assignmentId);
    return apiRequest<SubmissionSummary[]>(`/assignments/${assignmentId}/submissions`);
  },

  async members(courseId: number): Promise<CourseMember[]> {
    if (isDemo()) return demo.demoMembers;
    return apiRequest<CourseMember[]>(`/courses/${courseId}/members`);
  },

  async tests(assignmentId: number): Promise<TestCase[]> {
    if (isDemo()) return demo.demoTestsFor(assignmentId);
    return apiRequest<TestCase[]>(`/assignments/${assignmentId}/tests`);
  },

  async createTest(assignmentId: number, body: Partial<TestCase>): Promise<TestCase> {
    if (isDemo()) return demo.demoCreateTest(assignmentId, body);
    return apiRequest<TestCase>(`/assignments/${assignmentId}/tests`, { method: 'POST', body });
  },

  async updateTest(assignmentId: number, testId: number, body: Partial<TestCase>): Promise<TestCase> {
    if (isDemo()) return demo.demoUpdateTest(assignmentId, testId, body);
    return apiRequest<TestCase>(`/assignments/${assignmentId}/tests/${testId}`, { method: 'PATCH', body });
  },

  async deleteTest(assignmentId: number, testId: number): Promise<void> {
    if (isDemo()) return demo.demoDeleteTest(assignmentId, testId);
    await apiRequest(`/assignments/${assignmentId}/tests/${testId}`, { method: 'DELETE' });
  },

  async createAssignment(courseId: number, body: Record<string, unknown>): Promise<AssignmentSummary> {
    if (isDemo()) return demo.demoCreateAssignment(courseId, body);
    return apiRequest<AssignmentSummary>(`/courses/${courseId}/assignments`, { method: 'POST', body });
  },

  async updateAssignment(assignmentId: number, body: Record<string, unknown>): Promise<AssignmentSummary> {
    if (isDemo()) return demo.demoUpdateAssignment(assignmentId, body);
    return apiRequest<AssignmentSummary>(`/assignments/${assignmentId}`, { method: 'PATCH', body });
  },

  async deleteAssignment(assignmentId: number): Promise<void> {
    if (isDemo()) return demo.demoDeleteAssignment(assignmentId);
    await apiRequest(`/assignments/${assignmentId}`, { method: 'DELETE' });
  },

  async leaderboard(): Promise<LeaderboardRow[]> {
    // The API has no leaderboard endpoint: demo shows the prototype table,
    // live mode renders the signed-in student's own row only.
    return isDemo() ? demo.demoLeaderboard : [];
  },

  async notifications(): Promise<NotificationItem[]> {
    if (isDemo()) return demo.demoNotifications;

    try {
      const courses = await apiRequest<Course[]>('/courses');
      const lists = await Promise.all(
        courses.map((c) =>
          apiRequest<AssignmentSummary[]>(`/courses/${c.id}/assignments`).catch(() => []),
        ),
      );
      const open = lists.flat().filter((a) => a.assignmentStatus === 'PUBLISHED');
      return open.slice(0, 4).map((a) => {
        const closes = a.closingTime
          ? new Date(a.closingTime).toLocaleString(undefined, {
              weekday: 'short',
              day: 'numeric',
              month: 'short',
              hour: '2-digit',
              minute: '2-digit',
            })
          : 'no deadline';
        return {
          text: `${a.code}: ${a.title} closes ${closes}`,
          when: 'open now',
          tone: 'amber' as const,
        };
      });
    } catch {
      return [];
    }
  },
};
