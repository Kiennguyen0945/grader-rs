/**
 * Shapes returned by the existing Spring Boot API.
 * Field names match the backend DTOs exactly (no backend changes involved).
 */

export type AssignmentStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED';

export type SubmissionStatus =
  | 'QUEUED'
  | 'RUNNING'
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'COMPILE_ERROR'
  | 'TIME_LIMIT_EXCEEDED'
  | 'MEMORY_LIMIT_EXCEEDED'
  | 'RUNTIME_ERROR'
  | 'SYSTEM_ERROR'
  | 'NOT_RUN'
  | 'OUTPUT_LIMIT_EXCEEDED';

export type ProgrammingLanguage = 'JAVA' | 'PYTHON' | string;

/** `GET /api/courses` (Course entity). Only the flat fields are relied upon. */
export interface Course {
  id: number;
  code: string;
  courseName: string;
  description?: string | null;
  /** demo-mode only metadata (not exposed by the backend) */
  term?: string;
  students?: number;
  lecturer?: string;
}

/** `GET /api/courses/{courseId}/assignments` — AssignmentResponseDto */
export interface AssignmentSummary {
  id: number;
  courseId: number;
  userId?: number;
  code: string;
  title: string;
  assignmentStatus: AssignmentStatus;
  description?: string | null;
  submissionLimit?: number | null;
  openingTime?: string | null;
  closingTime?: string | null;
  allowedLanguages?: ProgrammingLanguage[];
  /** joined client-side from the course list */
  courseCode?: string;
  /** demo-mode only */
  timeLimitMs?: number;
  memoryLimitKib?: number;
}

export interface SampleTestCase {
  id: number;
  name?: string | null;
  input?: string | null;
  expectedOutput: string;
  displayOrder: number;
}

/** `GET /api/assignments/{assignmentId}` — AssignmentDetailResponseDto */
export interface AssignmentDetail extends AssignmentSummary {
  sampleTestCases: SampleTestCase[];
}

/** `GET /api/assignments/{assignmentId}/submissions` — SubmissionResponseDto */
export interface SubmissionSummary {
  submissionId: number;
  assignmentId: number;
  userId: number;
  sourceCode: string;
  status: SubmissionStatus;
  grade?: number | null;
  attemptNumber: number;
  /** demo-mode only (the Submission entity has no timestamp) */
  submittedAt?: string;
}

export interface SubmissionTestResult {
  testId: number;
  testName?: string | null;
  displayOrder: number;
  status: SubmissionStatus;
  input?: string | null;
  expectedOutput: string;
  actualOutput?: string | null;
  executionTimeMs?: number | null;
  memoryUsedKib?: number | null;
}

/** `GET /api/submissions/{submissionId}` — SubmissionDetailResponseDto */
export interface SubmissionDetail extends SubmissionSummary {
  passedTests: number;
  totalTests: number;
  sampleTestResults: SubmissionTestResult[];
}

/** `POST /api/auth/login` */
export interface LoginResponse {
  accessToken: string;
}

/** `GET /api/users` (only used for display labels in demo mode) */
export interface User {
  id: number;
  username: string;
  role: 'ADMIN' | 'LECTURER' | 'STUDENT';
  status?: string;
}

export interface LeaderboardRow {
  rank: number;
  username: string;
  name: string;
  initials: string;
  score: number;
  trend?: 'up' | 'down' | 'same';
  testsPassed?: number;
  testsTotal?: number;
  attempts?: number;
  lastActive?: string;
  you?: boolean;
}

export interface NotificationItem {
  text: string;
  when: string;
  tone: 'amber' | 'ok' | 'violet';
}

