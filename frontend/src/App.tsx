import { useEffect } from 'react';
import { Shell, type ShellSection } from './components/Shell';
import { ToastProvider } from './components/Toasts';
import { AuthProvider, useAuth } from './state/auth';
import { CrumbProvider } from './state/crumb';
import { ThemeProvider } from './state/theme';
import { isTeachRoute, useRoute, type Route } from './router';
import { LoginView } from './views/LoginView';
import { DashboardView } from './views/DashboardView';
import { CoursesView } from './views/CoursesView';
import { CourseDetailView } from './views/CourseDetailView';
import { WorkspaceView } from './views/WorkspaceView';
import { HistoryView } from './views/HistoryView';
import { LecturerDashboardView } from './views/LecturerDashboardView';
import { LecturerCourseView } from './views/LecturerCourseView';
import { AssignmentEditorView } from './views/AssignmentEditorView';
import { TestCasesView } from './views/TestCasesView';
import { ReviewQueueView } from './views/ReviewQueueView';
import { SubmissionReviewView } from './views/SubmissionReviewView';

/** Maps a route onto the sidebar highlight. */
function sectionFor(name: Route['name']): ShellSection {
  switch (name) {
    case 'course':
      return 'courses';
    case 'teach-courses':
    case 'teach-course':
    case 'teach-assignment':
    case 'teach-assignment-new':
      return 'teach-courses';
    default:
      return name as ShellSection;
  }
}

function Root() {
  const { session, logout } = useAuth();
  const route = useRoute();
  const signedIn = Boolean(session);
  const isLecturer = session?.roles?.includes('ROLE_LECTURER') ?? false;
  const home = isLecturer ? '#/teach' : '#/dashboard';

  useEffect(() => {
    if (!window.location.hash) window.location.hash = home;
  }, [home]);

  // Escape hatch: visiting #/login while signed in drops the session, so the login screen
  // is always reachable (e.g. to switch between lecturer and student accounts).
  useEffect(() => {
    if (signedIn && window.location.hash.startsWith('#/login')) {
      logout();
    }
  }, [signedIn, logout]);

  useEffect(() => {
    if (!signedIn) return;
    const hash = window.location.hash;
    if (!hash || hash === '#/' || hash === '#/login') {
      window.location.hash = home;
      return;
    }
    // Students never land on teaching screens; lecturers never land on student ones.
    if (isLecturer && !isTeachRoute(route)) {
      window.location.hash = '#/teach';
    } else if (!isLecturer && isTeachRoute(route)) {
      window.location.hash = '#/dashboard';
    }
  }, [signedIn, isLecturer, route, home]);

  if (!signedIn) return <LoginView />;

  return (
    <Shell active={sectionFor(route.name)}>
      {route.name === 'dashboard' && <DashboardView />}
      {route.name === 'courses' && <CoursesView />}
      {route.name === 'course' && <CourseDetailView courseId={route.courseId} />}
      {route.name === 'workspace' && <WorkspaceView assignmentId={route.assignmentId} />}
      {route.name === 'history' && <HistoryView />}

      {route.name === 'teach' && <LecturerDashboardView />}
      {route.name === 'teach-courses' && <CoursesView variant="lecturer" />}
      {route.name === 'teach-course' && <LecturerCourseView courseId={route.courseId} />}
      {route.name === 'teach-assignment-new' && (
        <AssignmentEditorView mode="create" courseId={route.courseId} />
      )}
      {route.name === 'teach-assignment' && (
        <AssignmentEditorView mode="edit" assignmentId={route.assignmentId} />
      )}
      {route.name === 'teach-tests' && <TestCasesView assignmentId={route.assignmentId} />}
      {route.name === 'teach-queue' && <ReviewQueueView assignmentId={route.assignmentId} />}
      {route.name === 'teach-review' && <SubmissionReviewView submissionId={route.submissionId} />}
    </Shell>
  );
}

export function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <ToastProvider>
          <CrumbProvider>
            <Root />
          </CrumbProvider>
        </ToastProvider>
      </AuthProvider>
    </ThemeProvider>
  );
}
