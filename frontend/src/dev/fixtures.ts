import type {
  AssignmentDetail,
  AssignmentSummary,
  Course,
  LeaderboardRow,
  NotificationItem,
  SampleTestCase,
  SubmissionDetail,
  SubmissionSummary,
  SubmissionTestResult,
} from '../types';

const MIN = 60_000;
const HOUR = 60 * MIN;
const DAY = 24 * HOUR;

const at = (offsetMs: number) => new Date(Date.now() + offsetMs).toISOString();

export const DEMO_USER = { id: 1, username: 'minh.tran', name: 'Minh Tran' };

export const demoCourses: Course[] = [
  {
    id: 201,
    code: 'CS201',
    courseName: 'Data Structures',
    description: 'Lists, trees, hashing, and the cost of each.',
    term: 'Fall 2026',
    students: 42,
    lecturer: 'Dr. Lan Pham',
  },
  {
    id: 210,
    code: 'CS210',
    courseName: 'Algorithms & Complexity',
    description: 'Graphs, dynamic programming, and what makes problems hard.',
    term: 'Fall 2026',
    students: 38,
    lecturer: 'Dr. Lan Pham',
  },
  {
    id: 101,
    code: 'CS101',
    courseName: 'Introduction to Programming',
    description: 'A first course in Java: variables, control flow, methods, arrays.',
    term: 'Fall 2026',
    students: 65,
    lecturer: '',
  },
];

export const TEMPLATE = `import java.io.*;

public class Main {
  static class Node { int val; Node next; Node(int v){ val = v; } }
  static Node head = null;
  static void append(int v) { /* TODO */ }
  static void prepend(int v) { /* TODO */ }
  static void delete(int v) { /* TODO */ }
  static void reverse()    { /* TODO */ }
  static void print()      { /* TODO */ }
  public static void main(String[] args) throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String line;
    while ((line = br.readLine()) != null) {
      String[] p = line.trim().split(" ");
      switch (p[0]) {
        case "APPEND":  append(Integer.parseInt(p[1]));  break;
        case "PREPEND": prepend(Integer.parseInt(p[1])); break;
        case "DELETE":  delete(Integer.parseInt(p[1]));  break;
        case "REVERSE": reverse(); break;
        case "PRINT":   print();   break;
      }
    }
  }
}
`;

const DESCRIPTION = `Implement a singly linked list of integers supporting five commands read from standard input, one per line, until EOF:

APPEND v — insert v at the tail
PREPEND v — insert v at the head
DELETE v — remove the first occurrence of v
REVERSE — reverse the list in place
PRINT — print all values space-separated, or EMPTY if the list has no nodes

Your solution must not use java.util.LinkedList. Aim for O(n) DELETE and O(1) PREPEND.`;

export const demoAssignments: AssignmentSummary[] = [
  {
    id: 11,
    courseId: 201,
    courseCode: 'CS201',
    code: 'LAB 1',
    title: 'Arrays and complexity',
    assignmentStatus: 'CLOSED',
    description: 'Big-O of array operations.',
    submissionLimit: 3,
    openingTime: at(-40 * DAY),
    closingTime: at(-39 * DAY),
    allowedLanguages: ['JAVA'],
  },
  {
    id: 12,
    courseId: 201,
    courseCode: 'CS201',
    code: 'LAB 2',
    title: 'Stack and queue',
    assignmentStatus: 'CLOSED',
    description: 'Implement a stack and a queue on top of an array.',
    submissionLimit: 3,
    openingTime: at(-22 * DAY),
    closingTime: at(-16 * DAY),
    allowedLanguages: ['JAVA'],
  },
  {
    id: 13,
    courseId: 201,
    courseCode: 'CS201',
    code: 'LAB 3',
    title: 'Linked list',
    assignmentStatus: 'PUBLISHED',
    description: DESCRIPTION,
    submissionLimit: 5,
    openingTime: at(-1 * DAY),
    closingTime: at(2 * DAY + 9 * HOUR + 41 * MIN),
    allowedLanguages: ['JAVA'],
    timeLimitMs: 2000,
    memoryLimitKib: 262144,
  },
  {
    id: 21,
    courseId: 210,
    courseCode: 'CS210',
    code: 'LAB 1',
    title: 'Graph traversal',
    assignmentStatus: 'PUBLISHED',
    description: 'Breadth-first and depth-first traversal over an adjacency list.',
    submissionLimit: 3,
    openingTime: at(-2 * DAY),
    closingTime: at(6 * DAY),
    allowedLanguages: ['JAVA'],
    timeLimitMs: 2000,
    memoryLimitKib: 262144,
  },
];

export const demoSampleTests: Record<number, SampleTestCase[]> = {
  13: [
    {
      id: 1,
      name: 'Sample: append three',
      input: 'APPEND 1\nAPPEND 2\nAPPEND 3\nPRINT',
      expectedOutput: '1 2 3',
      displayOrder: 1,
    },
    {
      id: 2,
      name: 'Sample: prepend and reverse',
      input: 'PREPEND 4\nAPPEND 1\nPRINT\nREVERSE\nPRINT',
      expectedOutput: '4 1\n1 4',
      displayOrder: 2,
    },
    {
      id: 3,
      name: 'Sample: delete by value',
      input: 'APPEND 1\nAPPEND 2\nAPPEND 3\nDELETE 2\nPRINT',
      expectedOutput: '1 3',
      displayOrder: 3,
    },
  ],
  21: [
    { id: 4, name: 'Sample: small graph', input: '4 3\n1 2\n2 3\n3 4\n1', expectedOutput: '1 2 3 4', displayOrder: 1 },
  ],
  11: [{ id: 5, name: 'Sample: sum', input: '3\n1 2 3', expectedOutput: '6', displayOrder: 1 }],
  12: [{ id: 6, name: 'Sample: stack ops', input: 'PUSH 1\nPOP', expectedOutput: '1', displayOrder: 1 }],
};

export const assignmentsById = (id: number) => demoAssignments.find((a) => a.id === id);

export function demoAssignmentDetail(id: number): AssignmentDetail {
  const assignment = assignmentsById(id) ?? demoAssignments[2];
  return { ...assignment, sampleTestCases: demoSampleTests[id] ?? [] };
}

export function assignmentsForCourse(courseId: number): AssignmentSummary[] {
  return demoAssignments.filter((a) => a.courseId === courseId);
}

/* ---------------------------------------------------------------------------
 * Submissions (prototype sample data, s94 → s102)
 * ------------------------------------------------------------------------- */

const testResults = (status: SubmissionSummary['status']): SubmissionTestResult[] => {
  const tests = demoSampleTests[13];
  const times: Array<[number, number]> = [
    [28, 24 * 1024],
    [162, 42 * 1024],
    [89, 29 * 1024],
  ];
  return tests.map((t, i) => ({
    testId: t.id,
    testName: t.name,
    displayOrder: t.displayOrder,
    status,
    input: t.input,
    expectedOutput: t.expectedOutput,
    actualOutput: status === 'ACCEPTED' ? t.expectedOutput : 'EMPTY',
    executionTimeMs: times[i][0],
    memoryUsedKib: times[i][1],
  }));
};

export let demoSubmissions: SubmissionSummary[] = [
  {
    submissionId: 102,
    assignmentId: 13,
    userId: 1,
    sourceCode: TEMPLATE,
    status: 'TIME_LIMIT_EXCEEDED',
    grade: 5.0,
    attemptNumber: 2,
    submittedAt: at(-5 * HOUR),
  },
  {
    submissionId: 101,
    assignmentId: 13,
    userId: 1,
    sourceCode: TEMPLATE,
    status: 'WRONG_ANSWER',
    grade: 3.5,
    attemptNumber: 1,
    submittedAt: at(-1 * DAY),
  },
  {
    submissionId: 100,
    assignmentId: 21,
    userId: 1,
    sourceCode: TEMPLATE,
    status: 'COMPILE_ERROR',
    grade: null,
    attemptNumber: 1,
    submittedAt: at(-3 * DAY),
  },
  {
    submissionId: 98,
    assignmentId: 12,
    userId: 1,
    sourceCode: TEMPLATE,
    status: 'ACCEPTED',
    grade: 9.5,
    attemptNumber: 2,
    submittedAt: at(-16 * DAY),
  },
  {
    submissionId: 97,
    assignmentId: 12,
    userId: 1,
    sourceCode: TEMPLATE,
    status: 'WRONG_ANSWER',
    grade: 4.0,
    attemptNumber: 1,
    submittedAt: at(-16 * DAY),
  },
  {
    submissionId: 94,
    assignmentId: 11,
    userId: 1,
    sourceCode: TEMPLATE,
    status: 'ACCEPTED',
    grade: 10.0,
    attemptNumber: 1,
    submittedAt: at(-30 * DAY),
  },
];

const demoDetails: Record<number, SubmissionDetail> = {
  102: {
    ...demoSubmissions[0],
    passedTests: 7,
    totalTests: 7,
    sampleTestResults: testResults('ACCEPTED'),
  },
  101: {
    ...demoSubmissions[1],
    passedTests: 2,
    totalTests: 7,
    sampleTestResults: testResults('WRONG_ANSWER'),
  },
  100: {
    ...demoSubmissions[2],
    passedTests: 0,
    totalTests: 3,
    sampleTestResults: [],
  },
  98: {
    ...demoSubmissions[3],
    passedTests: 3,
    totalTests: 3,
    sampleTestResults: testResults('ACCEPTED'),
  },
};

export function submissionsForAssignment(assignmentId: number): SubmissionSummary[] {
  return demoSubmissions
    .filter((s) => s.assignmentId === assignmentId)
    .sort((a, b) => a.attemptNumber - b.attemptNumber);
}

let nextDemoId = 600;
const created: Record<number, { submission: SubmissionSummary; startedAt: number }> = {};

export function demoCreateSubmission(assignmentId: number, sourceCode: string): SubmissionSummary {
  const attempts = submissionsForAssignment(assignmentId);
  const submission: SubmissionSummary = {
    submissionId: nextDemoId++,
    assignmentId,
    userId: 1,
    sourceCode,
    status: 'QUEUED',
    grade: null,
    attemptNumber: attempts.length + 1,
    submittedAt: new Date().toISOString(),
  };
  demoSubmissions = [...demoSubmissions, submission];
  created[submission.submissionId] = { submission, startedAt: Date.now() };
  return submission;
}

/** Simulates the RabbitMQ/Kubernetes pipeline so the polling UX is visible. */
export function demoSubmissionDetail(submissionId: number): SubmissionDetail {
  const pending = created[submissionId];
  if (!pending) {
    const known = demoDetails[submissionId];
    if (known) return known;
    const fallback = demoSubmissions.find((s) => s.submissionId === submissionId);
    return {
      ...(fallback ?? demoSubmissions[0]),
      passedTests: 0,
      totalTests: 0,
      sampleTestResults: [],
    };
  }

  const elapsed = Date.now() - pending.startedAt;
  const status: SubmissionSummary['status'] =
    elapsed < 2200 ? 'QUEUED' : elapsed < 4600 ? 'RUNNING' : 'ACCEPTED';
  const detail: SubmissionDetail = {
    ...pending.submission,
    status,
    grade: status === 'ACCEPTED' ? 10 : null,
    passedTests: status === 'ACCEPTED' ? 3 : 0,
    totalTests: 3,
    sampleTestResults: status === 'ACCEPTED' ? testResults('ACCEPTED') : [],
  };
  pending.submission = { ...pending.submission, status, grade: detail.grade };
  return detail;
}

/* ---------------------------------------------------------------------------
 * Leaderboard / notifications (design-only data)
 * ------------------------------------------------------------------------- */

export const demoLeaderboard: LeaderboardRow[] = [
  { rank: 1, username: 'minh.tran', name: 'Minh Tran', initials: 'MT', score: 96.0, trend: 'same', testsPassed: 21, testsTotal: 23, attempts: 8, lastActive: '5h ago', you: true },
  { rank: 2, username: 'an.nguyen', name: 'An Nguyen', initials: 'AN', score: 93.5, trend: 'up', testsPassed: 20, testsTotal: 23, attempts: 7, lastActive: '9h ago' },
  { rank: 3, username: 'huy.le', name: 'Huy Le', initials: 'HL', score: 91.0, trend: 'down', testsPassed: 19, testsTotal: 23, attempts: 6, lastActive: '22h ago' },
  { rank: 4, username: 'thu.vo', name: 'Thu Vo', initials: 'TV', score: 88.5, trend: 'up', testsPassed: 18, testsTotal: 23, attempts: 9, lastActive: '2d ago' },
  { rank: 5, username: 'bao.dang', name: 'Bao Dang', initials: 'BD', score: 86.0, trend: 'same', testsPassed: 17, testsTotal: 23, attempts: 5, lastActive: '2d ago' },
  { rank: 6, username: 'linh.do', name: 'Linh Do', initials: 'LD', score: 84.5, trend: 'up', testsPassed: 17, testsTotal: 23, attempts: 11, lastActive: '3d ago' },
  { rank: 7, username: 'khoa.banh', name: 'Khoa Banh', initials: 'KB', score: 82.0, trend: 'down', testsPassed: 16, testsTotal: 23, attempts: 6, lastActive: '4d ago' },
  { rank: 8, username: 'mai.nguyen', name: 'Mai Nguyen', initials: 'MN', score: 80.5, trend: 'up', testsPassed: 16, testsTotal: 23, attempts: 8, lastActive: '6d ago' },
  { rank: 9, username: 'duc.hoang', name: 'Duc Hoang', initials: 'DH', score: 78.5, trend: 'same', testsPassed: 15, testsTotal: 23, attempts: 6, lastActive: '1d ago' },
  { rank: 10, username: 'thao.truong', name: 'Thao Truong', initials: 'TT', score: 76.0, trend: 'same', testsPassed: 15, testsTotal: 23, attempts: 7, lastActive: '22m ago' },
  { rank: 11, username: 'nam.pham', name: 'Nam Pham', initials: 'NP', score: 72.5, trend: 'down', testsPassed: 13, testsTotal: 23, attempts: 9, lastActive: '3h ago' },
  { rank: 12, username: 'hien.bui', name: 'Hien Bui', initials: 'HB', score: 69.0, trend: 'up', testsPassed: 12, testsTotal: 23, attempts: 8, lastActive: '11h ago' },
];

export const demoNotifications: NotificationItem[] = [
  { text: 'Lab 3: Linked list closes Friday 23:59', when: '12m ago', tone: 'amber' },
  { text: 'Your Lab 2 final grade was released: 9.5', when: '2d ago', tone: 'ok' },
  { text: 'Announcement · CS201 office hours moved to Thu 14:00', when: '1d ago', tone: 'violet' },
];

export const demoLoginMeta = [
  { strong: 'CS201', text: 'Data Structures' },
  { strong: '42', text: 'students' },
  { strong: '31/42', text: 'submitted · Lab 3' },
];


