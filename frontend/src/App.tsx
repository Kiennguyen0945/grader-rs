import { useEffect } from 'react';
import { Shell } from './components/Shell';
import { ToastProvider } from './components/Toasts';
import { AuthProvider, useAuth } from './state/auth';
import { CrumbProvider } from './state/crumb';
import { ThemeProvider } from './state/theme';
import { useRoute } from './router';
import { LoginView } from './views/LoginView';
import { DashboardView } from './views/DashboardView';
import { CoursesView } from './views/CoursesView';
import { CourseDetailView } from './views/CourseDetailView';
import { WorkspaceView } from './views/WorkspaceView';
import { HistoryView } from './views/HistoryView';

function Root() {
  const { session } = useAuth();
  const route = useRoute();
  const signedIn = Boolean(session);

  useEffect(() => {
    if (!window.location.hash) window.location.hash = '#/dashboard';
  }, []);

  useEffect(() => {
    if (signedIn && (!window.location.hash || window.location.hash === '#/' || window.location.hash === '#/login')) {
      window.location.hash = '#/dashboard';
    }
  }, [signedIn]);

  if (!signedIn) return <LoginView />;

  const section = route.name === 'course' ? 'courses' : route.name;

  return (
    <Shell active={section}>
      {route.name === 'dashboard' && <DashboardView />}
      {route.name === 'courses' && <CoursesView />}
      {route.name === 'course' && <CourseDetailView courseId={route.courseId} />}
      {route.name === 'workspace' && <WorkspaceView assignmentId={route.assignmentId} />}
      {route.name === 'history' && <HistoryView />}
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
